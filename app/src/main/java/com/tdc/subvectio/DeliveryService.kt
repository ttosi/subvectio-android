package com.tdc.subvectio

//import kotlin.reflect.full.memberProperties
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

//    companion object {
//        var d = Delivery()
//        var s = Shift()
//    }

    lateinit var d: Delivery
    var s: Shift = Shift()

    override fun onServiceConnected() {
        initOverlay()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        registerReceiver(
            deliveryNotification,
            IntentFilter("com.tdc.subvectio.NEW_DELIVERY")
        )
    }

    override fun onInterrupt() {}

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
//
//                    Logger.log(">>> TYPE_WINDOW_CONTENT_CHANGED >>> * <<<")
//                    Logger.log(">>> ${getScreenText(event.source)}")

//                    Logger.log(">>> TYPE_WINDOW_CONTENT_CHANGED (ScreenType) >>> ${Screen.type(getScreenText(event.source)).name} <<<")

                    when(Screen.type(getScreenText(event.source))) {
                        ScreenType.OFFER_SCREEN -> {
                            offerScreenText = getScreenText(event.source)
                        }
                        ScreenType.ARE_YOU_SURE_DECLINE_SCREEN -> overlayDelivery?.visibility = View.GONE
                        ScreenType.DECLINED_SCREEN -> {
                            Logger.log(">>> DECLINED")
                            s.numDeclined++
                            d.completedAt = LocalDateTime.now()
                            d.wasDeclined = true
                            Logger.data(d)
                        }
                        ScreenType.DELIVERY_COMPLETE -> {
                            // todo: need to handle when peak pay is in effect

                            val payouts = getScreenText(event.source)
                            d.basePay = payouts[3].replace("$", "").toDouble()
                            d.tip = payouts[6].replace("$", "").toDouble()
                            d.actual = payouts[8].replace("$", "").toDouble()

                            Logger.log("BASE PAY          >>> $${d.basePay}")
                            Logger.log("TIP               >>> $${d.tip}")
                            Logger.log("ACTUAL PAY        >>> $${d.actual}")
                            Logger.log(">>> COMPLETE  ")

                            Logger.data(d)
                        }
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
                            d.acceptedAt = LocalDateTime.now()
                            d.isActive = true
                            s.numAccepted++
                            Logger.log(">>> ACCEPTED")
                        }
                        "arrived at store" -> {
                            d.arrivedStoreAt = LocalDateTime.now()
                            Logger.log("DRIVE TO STORE    >>> ${getMinutes(d.acceptedAt, d.arrivedStoreAt)} mins")
                        }
                        "confirm pickup" -> {
                            d.departedStoreAt = LocalDateTime.now()
                            Logger.log("WAIT AT STORE     >>> ${ getMinutes(d.arrivedStoreAt, d.departedStoreAt)} mins")
                        }
                        "complete delivery", "confirm to complete delivery" -> {
                            d.completedAt = LocalDateTime.now()
                            d.isActive = false
                            Logger.log("DRIVE TO CUSTOMER >>> ${getMinutes(d.departedStoreAt, d.completedAt)} mins")
                            Logger.log(
                                "DELIVERY TIME     >>> ${getMinutes(d.acceptedAt, d.completedAt)} mins")

                            Logger.data(d)
                        }
                        "pause dash", "pause orders" -> Logger.log(">>> PAUSED")
                        "resume dash" -> Logger.log(">>> RESUMED")
                        "end dash" -> {
                            s.endAt = LocalDateTime.now()

                            Logger.log("--------------------- SHIFT SUMMARY ---------------------")
                            Logger.log(
                                "SHIFT LENGTH      >>> ${
                                    getMinutes(
                                        s.startAt,
                                        s.endAt
                                    ) / 60
                                } hours"
                            )
                            Logger.log("OFFERS            >>> ${s.numOffers}")
                            Logger.log("ACCEPTED          >>> ${s.numAccepted}")
                            Logger.log("DECLINED          >>> ${s.numDeclined}")
                            Logger.log("UNASSIGNED        >>> ${s.numUnassigned}")
                            Logger.log("----------------------- SHIFT END -----------------------")
                        }
                        "unassign this delivery" -> {
                            s.numUnassigned++
                            Logger.log(">>> UNASSIGNED")
                        }
                        // todo: FOR DEBUG ONLY
                        "decline" -> {
                            Logger.log(">>> DECLINED..")
                            s.numDeclined++
                            d.completedAt = LocalDateTime.now()
                            d.wasDeclined = true
                            Logger.data(d)
                        }
                    }
                }
                else -> return
            }
        } catch (ex: Exception) {
            Logger.log(ex.stackTraceToString(), true)
        }
    }

    private val deliveryNotification = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            if (intent == null) return
            if (intent.extras == null) return

            try {
                // bring dash app to front
                val packageManager = applicationContext.packageManager
                val launchIntent = packageManager.getLaunchIntentForPackage(intent.extras?.getString("sourcePackageName")!!)
                launchIntent?.addCategory(Intent.CATEGORY_LAUNCHER)
                applicationContext.startActivity(launchIntent)

                Thread.sleep(1000)

                d = Delivery()

                d.offer = offerScreenText.firstOrNull { it.startsWith("$") }?.replace("$", "")?.toDouble()
                d.storeName = intent.extras?.getString("storeName")
                d.storeAddress = intent.extras?.getString("storeAddress")
                d.customerAddress = intent.extras?.getString("customerAddress")
                d.distance = intent.extras?.getDouble("distance")

                showDeliveryOverlay(
                    URLEncoder.encode(d.storeAddress, "utf-8"),
                    URLEncoder.encode(d.customerAddress, "utf-8")
                )
            } catch (ex: Exception) {
                Logger.log(ex.stackTraceToString(), true)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showDeliveryOverlay(merchAddr: String?, custAddr: String?) {
        fusedLocationClient.lastLocation // fixme - adds new listener each call, i think
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
                            d.deliveryTime = (distTime.rows[0].elements[0].durationInTraffic.value + distTime.rows[1].elements[1].durationInTraffic.value) / 60.0
                            d.deliveryDistance = (distTime.rows[0].elements[0].distance.value + distTime.rows[1].elements[1].distance.value) / 1609.0

                            ContextCompat.getMainExecutor(baseContext).execute(Runnable {
                                tvStoreName?.text = d.storeName?.toUpperCase(Locale.ROOT)
                                tvOfferAmount?.text = "$%.2f".format(d.offer)
                                tvTotalMiles?.text = "%.1f mi".format(d.deliveryDistance)
                                tvTotalTime?.text = "%.0f mins".format(d.deliveryTime)
                                tvDollarsPerMile?.text = "$%.2f/mi".format(d.offer?.div(d.deliveryDistance))
                                tvDeliveryIndex?.text = "Index: %.1f".format(60 / (d.deliveryTime + 3) * d.offer!!)
                                overlayDelivery?.visibility = View.VISIBLE
                            })

                            d.deliveryIndex = 60 / (d.deliveryTime + 3) * d.offer!!

                            Logger.log("OFFER             >>> $${d.offer}")
                            Logger.log("TIME              >>> ${d.deliveryTime} mins")
                            Logger.log("DELIVERY INDEX    >>> ${d.deliveryIndex}")
                            Logger.log("DISTANCE (maps)   >>> ${d.deliveryDistance}")
                            Logger.log("DISTANCE (app)    >>> ${d.distance}")
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
        if (node != null && node.childCount > 0) {
            for (index in 0 until node.childCount) {
                if (!node.getChild(index).text.isNullOrEmpty()) {
                    nodeText.add(node.getChild(index).text.toString())
                }
                readNodeText(node.getChild(index), nodeText)
            }
        }
    }

    private fun getMinutes(start: LocalDateTime, end: LocalDateTime): Long {
        val seconds = ChronoUnit.SECONDS.between(start, end)
        return if(seconds < 60) 1 else seconds / 60
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