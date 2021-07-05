package com.tdc.subvectio

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.tdc.subvectio.data.DeliveryDao
import com.tdc.subvectio.data.SubvectioDatabase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.*
import okio.IOException
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class DeliveryService : AccessibilityService() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val client = OkHttpClient()

    private var offerScreenText: MutableList<String> = mutableListOf()

    private var overlayDelivery: View? = null
    private var tvStoreName: TextView? = null
    private var tvOfferAmount: TextView? = null
    private var tvDollarsPerMile: TextView? = null
    private var tvTotalMiles: TextView? = null
    private var tvTotalTime: TextView? = null
    private var tvDeliveryIndex: TextView? = null

    private val baseUrl = "https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial"
    private val optionsUrl = "&key=AIzaSyDGy4Y9IHHyVf3ABO2KnpLsDgeRm_uDRfc&traffic_model=best_guess&departure_time=now"

    private lateinit var db: SubvectioDatabase
    private lateinit var deliveryDao: DeliveryDao
    private lateinit var delivery: com.tdc.subvectio.entities.Delivery

    private var isDebug: Boolean = false

    var s: Shift = Shift()

    @DelicateCoroutinesApi
    override fun onServiceConnected() {
        db = SubvectioDatabase.getDatabase(this)
        deliveryDao = db.deliveryDao()

        initOverlay()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        registerReceiver(
            deliveryNotification,
            IntentFilter("com.tdc.subvectio.NEW_DELIVERY")
        )
    }

    override fun onInterrupt() {}

    @DelicateCoroutinesApi
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.source == null) return

        try {
            when(event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    if (Screen.type(getScreenText(event.source)) != ScreenType.OFFER_SCREEN) {
                        overlayDelivery?.visibility = View.GONE
                    }
                }
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    when(Screen.type(getScreenText(event.source))) {
                        ScreenType.OFFER_SCREEN -> {
                            offerScreenText = getScreenText(event.source)
                        }
                        ScreenType.ARE_YOU_SURE_DECLINE_SCREEN -> overlayDelivery?.visibility = View.GONE
                        ScreenType.DECLINED_SCREEN -> {
                            delcineDelivery()
                        }
                        ScreenType.DELIVERY_COMPLETE -> {
                            completeDelivery(getScreenText(event.source))
                        }
                        else -> return
                    }
                }
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    val buttonText = when (event.source.childCount) {
                        0 -> event.source.text
                        1 -> event.source.getChild(0).text
                        else -> return
                    } ?: return

//                    Logger.log(">>> TYPE_VIEW_CLICKED (buttonText) >>> $buttonText <<<")

                    when (buttonText.toString().toLowerCase(Locale.ROOT)) {
                        "dash now" -> {
                            Logger.log("---------------------- SHIFT START ----------------------")
                            s = Shift()
                            s.startAt = LocalDateTime.now()
                        }
                        "accept" -> {
                            delivery.acceptedAt = LocalDateTime.now()
                            s.numAccepted++
                            GlobalScope.launch { deliveryDao.update(delivery) }

                            Logger.log(">>> ACCEPTED")
                        }
                        "arrived at store" -> {
                            delivery.arrivedStoreAt = LocalDateTime.now()
                            delivery.toStoreMins = getMinutes(delivery.acceptedAt, delivery.arrivedStoreAt)
                            GlobalScope.launch { deliveryDao.update(delivery) }

                            Logger.log("DRIVE TO STORE    >>> %.1f mins".format(delivery.toStoreMins))
                        }
                        "confirm pickup" -> {
                            delivery.departedStoreAt = LocalDateTime.now()
                            delivery.atStoreMins = getMinutes(delivery.arrivedStoreAt, delivery.departedStoreAt)
                            GlobalScope.launch { deliveryDao.update(delivery) }

                            Logger.log("WAIT AT STORE     >>> %.1f mins".format(delivery.atStoreMins))
                        }
                        // todo: FOR DEBUG ONLY
                        "complete delivery" -> {
                            if(isDebug) {
                                val payouts = mutableListOf("Delivery Complete!", "Pay", "Base pay", "$9.50", "Tip", "Customer tip", "$5.00", "Total", "$14.50", "Got it")
                                completeDelivery(payouts)
                            }
                        }
                        "pause dash", "pause orders" -> Logger.log(">>> PAUSED")
                        "resume dash" -> Logger.log(">>> RESUMED")
                        "end dash" -> {
                            s.endAt = LocalDateTime.now()

                            // todo: total earned, total tips, total base, total peak
                            // todo: time dashing, time active, percent active, percent declined
                            // todo: avg delivery index accepted, average delivery index declined
                            // todo: miles driven <- maybe
                            Logger.log("--------------------- SHIFT SUMMARY ---------------------")
                            Logger.log("SHIFT LENGTH      >>> %.1f hours".format(getMinutes(s.startAt, s.endAt) / 60))
                            Logger.log("OFFERS            >>> ${s.numOffers}")
                            Logger.log("ACCEPTED          >>> ${s.numAccepted}")
                            Logger.log("DECLINED          >>> ${s.numDeclined}")
                            Logger.log("UNASSIGNED        >>> ${s.numUnassigned}")
                            Logger.log("----------------------- SHIFT END -----------------------")
                        }
                        "unassign this delivery" -> {
                            s.numUnassigned++
                            delivery.isUnassigned = true
                            delivery.completedAt = LocalDateTime.now()

                            GlobalScope.launch { deliveryDao.update(delivery) }

                            Logger.log(">>> UNASSIGNED")
                        }
                        "decline" -> {
                            if(isDebug) {
                                delcineDelivery()
                            }
                        }
                    }
                }
                else -> return
            }
        } catch (ex: Exception) {
            Logger.log(ex.stackTraceToString(), true)
        }
    }

    @DelicateCoroutinesApi
    private fun completeDelivery(payouts: MutableList<String>) {
        delivery.completedAt = LocalDateTime.now()

        when(payouts.contains("Peak pay")) {
            false -> {
                delivery.basePay = payouts[3].replace("$", "").toDouble()
                delivery.tip = payouts[6].replace("$", "").toDouble()
                delivery.actualPay = payouts[8].replace("$", "").toDouble()
            }
            true -> {
                delivery.basePay = payouts[3].replace("$", "").toDouble()
                delivery.peakPay = payouts[5].replace("$", "").toDouble()
                delivery.tip = payouts[8].replace("$", "").toDouble()
                delivery.actualPay = payouts[10].replace("$", "").toDouble()
            }
        }

        delivery.toDeliverMins = getMinutes(delivery.departedStoreAt, delivery.completedAt)
        delivery.actualDuration = getMinutes(delivery.acceptedAt, delivery.completedAt)
        delivery.actualDeliveryIndex = 60.0 / delivery.actualDuration* delivery.actualPay

        GlobalScope.launch { deliveryDao.update(delivery) }

        Logger.log("DRIVE TO CUSTOMER >>> %.1f mins".format(delivery.toDeliverMins))
        Logger.log("ACTUAL DURATION   >>> %.1f mins".format(delivery.actualDuration))
        Logger.log("BASE PAY          >>> \$%.2f".format(delivery.basePay))
        Logger.log("PEAK PAY          >>> \$%.2f".format(delivery.peakPay))
        Logger.log("TIP               >>> \$%.2f".format(delivery.tip))
        Logger.log("ACTUAL PAY        >>> \$%.2f".format(delivery.actualPay))
        Logger.log("ACTUAL INDEX      >>> %.1f".format(delivery.actualDeliveryIndex))

        Logger.log(">>> COMPLETE  ")
    }

    @DelicateCoroutinesApi
    private fun delcineDelivery() {
        delivery.isDeclined = true
        delivery.completedAt = LocalDateTime.now()
        s.numDeclined++

        GlobalScope.launch { deliveryDao.update(delivery) }

        Logger.log(">>> DECLINED  ")
    }

    @DelicateCoroutinesApi
    private val deliveryNotification = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            if (intent == null) return
            if (intent.extras == null) return

            try {
                val sourcePackageName = intent.extras?.getString("sourcePackageName")

                isDebug = when(sourcePackageName) {
                    "com.tdc.subvectio_dummy" -> true
                    else -> false
                }

                // bring dash app to front
                // todo: check to see if app is already focused
                val packageManager = applicationContext.packageManager
                val launchIntent = packageManager.getLaunchIntentForPackage(sourcePackageName!!)
                launchIntent?.addCategory(Intent.CATEGORY_LAUNCHER)
                applicationContext.startActivity(launchIntent)

                Thread.sleep(1000)

                delivery = com.tdc.subvectio.entities.Delivery(
                    storeName = intent.extras?.getString("storeName")!!,
                    storeAddress = intent.extras?.getString("storeAddress")!!,
                    customerAddress = intent.extras?.getString("customerAddress")!!,
                    offer = offerScreenText.firstOrNull { it.startsWith("$") }?.replace("$", "")?.toDouble()!!,
                    createdAt = LocalDateTime.now(),
                    isDebug = isDebug
                )

                GlobalScope.launch {
                    var id = deliveryDao.insert(delivery)
                    delivery = deliveryDao.get(id)
                }

                showDeliveryOverlay(
                    URLEncoder.encode(delivery.storeAddress, "utf-8"),
                    URLEncoder.encode(delivery.customerAddress, "utf-8")
                )
            } catch (ex: Exception) {
                Logger.log(ex.stackTraceToString(), true)
            }
        }
    }

    @DelicateCoroutinesApi
    @SuppressLint("MissingPermission")
    private fun showDeliveryOverlay(merchAddr: String?, custAddr: String?) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val url = baseUrl +
                        "&origins=${location.latitude},${location.longitude}|${merchAddr}" +
                        "&destinations=${merchAddr}|${custAddr}${optionsUrl}"

                    val req = Request.Builder()
                        .url(url)
                        .addHeader("token", "04e5e24a540544d2b5158de16cc0c22a")
                        .build()

                    client.newCall(req).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {}

                        @SuppressLint("SetTextI18n")
                        override fun onResponse(call: Call, response: Response) {
                            val distTime = Json.decodeFromString<DistanceTime>(response.body!!.string())

                            delivery.estDuration = (distTime.rows[0].elements[0].durationInTraffic.value + distTime.rows[1].elements[1].durationInTraffic.value) / 60.0
                            delivery.distance = (distTime.rows[0].elements[0].distance.value + distTime.rows[1].elements[1].distance.value) / 1609.0
                            delivery.estDeliveryIndex = 60.0 / (delivery.estDuration + 3) * delivery.offer // add 3 minutes for avg store wait (2 min) & dropoff (1 min)

                            ContextCompat.getMainExecutor(baseContext).execute(Runnable {
                                val color = when {
                                    delivery.estDeliveryIndex <= 20 -> Color.parseColor("#FF0000")
                                    delivery.estDeliveryIndex < 30 && delivery.estDeliveryIndex > 20 -> Color.parseColor("#FFFF00")
                                    delivery.estDeliveryIndex >= 30 -> Color.parseColor("#00FF00")
                                    else -> Color.parseColor("#FFFFFF")
                                }

                                tvStoreName?.text = delivery.storeName.toUpperCase(Locale.ROOT)
                                tvOfferAmount?.text = "$%.2f".format(delivery.offer)
                                tvTotalMiles?.text = "%.1f miles".format(delivery.distance)
                                tvTotalTime?.text = "%.0f mins".format(delivery.estDuration)
                                tvDollarsPerMile?.text = "$%.2f/mi".format(delivery.offer.div(delivery.distance))
                                tvDeliveryIndex?.text = "Delivery Index: %.1f".format(delivery.estDeliveryIndex)
                                tvDeliveryIndex?.setTextColor(color)
                                overlayDelivery?.visibility = View.VISIBLE
                            })

                            GlobalScope.launch { deliveryDao.update(delivery) }

                            Logger.log("OFFER             >>> \$%.2f".format(delivery.offer))
                            Logger.log("DURATION          >>> %.1f mins".format(delivery.estDuration))
                            Logger.log("DISTANCE          >>> %.1f miles".format(delivery.distance))
                            Logger.log("ESTIMATED INDEX   >>> %.1f".format(delivery.estDeliveryIndex))

                            s.numOffers++
                            response.close()
                        }
                    })
                }
            }
    }

    private fun getScreenText(node: AccessibilityNodeInfo): MutableList<String> {
        val screenText: MutableList<String> = mutableListOf()
        readNodeText(node, screenText)
        return screenText
    }

    private fun readNodeText(node: AccessibilityNodeInfo, nodeText: MutableList<String>) {
        if (node.childCount > 0) {
            for (index in 0 until node.childCount) {
                if (!node.getChild(index).text.isNullOrEmpty()) {
                    nodeText.add(node.getChild(index).text.toString())
                }
                readNodeText(node.getChild(index), nodeText)
            }
        }
    }

    private fun getMinutes(start: LocalDateTime?, end: LocalDateTime?): Double {
        val seconds = ChronoUnit.SECONDS.between(start, end)
        return if(seconds < 60) 1.0 else seconds / 60.0
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initOverlay() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val frame = FrameLayout(this)
        val inflater = LayoutInflater.from(this)
        val params = WindowManager.LayoutParams()

        params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        params.format = PixelFormat.TRANSLUCENT
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.gravity = Gravity.CENTER

        overlayDelivery = inflater.inflate(R.layout.overlay_delivery, frame)

        tvStoreName = overlayDelivery?.findViewById(R.id.storeName)
        tvOfferAmount = overlayDelivery?.findViewById(R.id.offerAmount)
        tvDollarsPerMile = overlayDelivery?.findViewById(R.id.dollarPerMile)
        tvTotalMiles = overlayDelivery?.findViewById(R.id.totalMiles)
        tvTotalTime = overlayDelivery?.findViewById(R.id.totalTime)
        tvDeliveryIndex = overlayDelivery?.findViewById(R.id.deliveryIndex)

        val toggle: ImageView = overlayDelivery!!.findViewById(R.id.overlayTruck)
        toggle.setOnClickListener {
            val dataView: LinearLayout = overlayDelivery!!.findViewById(R.id.deliveryData)
            if (dataView.visibility == View.VISIBLE) {
                dataView.visibility = View.GONE
                toggle.background = getDrawable(R.drawable.border2)
            } else {
                dataView.visibility = View.VISIBLE
                toggle.background = getDrawable(R.drawable.border)
            }
        }

        overlayDelivery?.visibility = View.GONE
        windowManager.addView(frame, params)
    }
}

enum class ScreenType {
    OFFER_SCREEN,
    DECLINED_SCREEN,
    ARE_YOU_SURE_DECLINE_SCREEN,
    DELIVERY_COMPLETE,
    UNKNOWN
}

object Screen {
    fun type(text: MutableList<String>): ScreenType {
        return when {
            text.contains("Accept") && text.contains("Decline") -> ScreenType.OFFER_SCREEN
            text.contains("Are you sure you want to decline this order?") -> ScreenType.ARE_YOU_SURE_DECLINE_SCREEN
            text.count() == 2 && text.contains("Please select a reason") && text.contains("Go back") -> ScreenType.DECLINED_SCREEN
            text.contains("Delivery Complete!") -> ScreenType.DELIVERY_COMPLETE
            else -> return ScreenType.UNKNOWN
        }
    }
}