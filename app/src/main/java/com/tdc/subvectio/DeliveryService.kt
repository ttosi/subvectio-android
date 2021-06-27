package com.tdc.subvectio

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
import java.util.*

class DeliveryService : AccessibilityService() {
    lateinit var fusedLocationClient: FusedLocationProviderClient
    private val client = OkHttpClient()

    var currentScreenText: MutableList<String> = mutableListOf()

    private var overlayDelivery: View? = null
    private var tvMerchantName: TextView? = null
    private var tvOfferAmount: TextView? = null
    private var tvDollarsPerMile: TextView? = null
    private var tvTotalMiles: TextView? = null
    private var tvTotalTime: TextView? = null

    private val baseUrl = "https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial"
    private val optionsUrl = "&key=AIzaSyDGy4Y9IHHyVf3ABO2KnpLsDgeRm_uDRfc&traffic_model=best_guess&departure_time=now"

    companion object {
        var delivery = Delivery()
    }

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

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            currentScreenText.clear()
            getAllNodeText(event.source, currentScreenText)

            if(Screen.type(currentScreenText) != ScreenType.OFFER_SCREEN) {
                overlayDelivery?.visibility = View.GONE
            }
        }

        if(event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            if(event.source.text == null) return
            when(event.source.text.toString().toLowerCase()) {
                "accept" -> {
                    delivery.acceptedAt = LocalDateTime.now()
                    delivery.isActive = true
                }
                "arrived at store" -> delivery.arrivedMerchantAt = LocalDateTime.now()
                "confirmed" -> delivery.departedMerchantAt = LocalDateTime.now() // x2?
                "completed" -> delivery.completedAt = LocalDateTime.now()
            }
//            if(event.source.text.toString().toLowerCase() == "accept") {
//                delivery.isActive = true
//                delivery.acceptedAt = LocalDateTime.now()
//            }
        }
    }

    private val deliveryNotification = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            if(intent == null) return
            if(intent.extras == null) return

            // bring dash app to front
            val packageManager = applicationContext.packageManager
            val launchIntent = packageManager.getLaunchIntentForPackage(intent.extras?.getString("sourcePackageName")!!)
            launchIntent?.addCategory(Intent.CATEGORY_LAUNCHER)
            applicationContext.startActivity(launchIntent)

            Thread.sleep(500)

            currentScreenText.clear()
            getAllNodeText(rootInActiveWindow, currentScreenText)

            delivery.offerAmount = currentScreenText.firstOrNull { it.startsWith("$") }!!.replace("$", "").toDouble()
            delivery.merchantName = intent.extras?.getString("merchantName")!!
            delivery.merchantAddress = intent.extras?.getString("merchantAddress")!!
            delivery.customerAddress = intent.extras?.getString("customerAddress")!!

            showDeliveryOverlay(
                URLEncoder.encode(delivery.merchantAddress, "utf-8"),
                URLEncoder.encode(delivery.customerAddress, "utf-8")
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun showDeliveryOverlay(merchAddr: String?, custAddr: String?) {
        fusedLocationClient.lastLocation // fixme - adds new listener each call, i think
            .addOnSuccessListener { location ->
                println(">> location event <<")
                if (location != null) {
                    val url = baseUrl +
                        "&origins=${location.latitude},${location.longitude}|${merchAddr}" +
                        "&destinations=${merchAddr}|${custAddr}${optionsUrl}"

                    println(">> $url")

                    val req = Request.Builder()
                        .url(url)
                        .build()

                    client.newCall(req).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {}

                        @SuppressLint("SetTextI18n")
                        override fun onResponse(call: Call, response: Response) {

                            val distTime = Json.decodeFromString<DistanceTime>(response.body!!.string())
                            delivery.deliveryTime = (distTime.rows[0].elements[0].durationInTraffic.value + distTime.rows[1].elements[1].durationInTraffic.value) / 60.0
                            delivery.deliveryDistance = (distTime.rows[0].elements[0].distance.value + distTime.rows[1].elements[1].distance.value) / 1609.0

                            ContextCompat.getMainExecutor(baseContext).execute(Runnable {
                                tvMerchantName?.text = delivery.merchantName.toUpperCase(Locale.ROOT)
                                tvOfferAmount?.text = "$%.2f".format(delivery.offerAmount)
                                tvTotalMiles?.text = "%.1f mi".format(delivery.deliveryDistance)
                                tvTotalTime?.text = "%.0f mins".format(delivery.deliveryTime)
                                tvDollarsPerMile?.text = "$%.2f/mi".format(delivery.offerAmount / delivery.deliveryDistance)
                                overlayDelivery?.visibility = View.VISIBLE
                            })

                            response.close()
                        }
                    })
                }
            }
    }

    private fun getAllNodeText(node: AccessibilityNodeInfo, nodeText: MutableList<String>) {
        if (node.childCount > 0) {
            for (idx in 0 until node.childCount) {
                if (!node.getChild(idx).text.isNullOrEmpty()) {
                    nodeText.add(node.getChild(idx).text.toString())
                }
                getAllNodeText(node.getChild(idx), nodeText)
            }
        }
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

        tvMerchantName = overlayDelivery?.findViewById(R.id.merchantName)
        tvOfferAmount = overlayDelivery?.findViewById(R.id.offerAmount)
        tvDollarsPerMile = overlayDelivery?.findViewById(R.id.dollarPerMile)
        tvTotalMiles = overlayDelivery?.findViewById(R.id.totalMiles)
        tvTotalTime = overlayDelivery?.findViewById(R.id.totalTime)

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
    STORE_SCREEN,
    CUSTOMER_SCREEN,
    NONE
}

object Screen {
    fun type(nodeTextList: MutableList<String>): ScreenType {
        when {
            // is offer layout
            nodeTextList.contains("Accept") && nodeTextList.contains("Decline")
            -> return ScreenType.OFFER_SCREEN
        }
        return ScreenType.NONE
    }
}