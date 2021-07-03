package com.tdc.subvectio

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.TextView

class ScreenService : AccessibilityService()  {
    var overlayDelivery: View? = null
    var tvStoreName: TextView? = null
    var tvOfferAmount: TextView? = null
    var tvDollarsPerMile: TextView? = null
    var tvTotalMiles: TextView? = null
    var tvTotalTime: TextView? = null

    override fun onServiceConnected() {
        println("screen service init..;;;..")

    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    fun initOverlay() {
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

        overlayDelivery?.visibility = View.GONE

        windowManager.addView(frame, params)
    }
//    private var tvOfferAmount: TextView? = null
//    private var tvDollarsPerMile: TextView? = null
//    private var tvTotalMiles: TextView? = null
//    private var tvTotalTime: TextView? = null
//
////    var currentScreenText: MutableList<String> = mutableListOf()
//    var overlayDelivery: View? = null
//
////    var deliveryDistance: Double by Delegates.observable(0.0) { _, _, newVal ->
//////            ss.tvTotalMiles?.text = "%.1f mi".format(newVal)
////        tvTotalMiles?.text = "yikes"
////        println("whelp.a. -->>> $newVal")
////    }
//
//    companion object {
//        @SuppressLint("StaticFieldLeak")
//        val ss = ScreenService()
//        var merchantName: String = ""
//
//        //        var deliveryDistance: Double = 0.0
//        var deliveryTime: Double = 0.0
//        var offerAmount: Double = 0.0
//
////        var deliveryDistance: Double by Delegates.observable(0.0) { _, _, newVal ->
//////            ss.tvTotalMiles?.text = "%.1f mi".format(newVal)
////            ss.tvTotalMiles?.text = "huh?"
////            println("whelp.... -->>> $newVal")
////
////        }
//
////        fun updDist(ss: ScreenService) {
////            var deliveryDistance: Double by Delegates.observable(0.0) { _, _, newVal ->
////                println("whelp -->>> $newVal")
////                ss.tvTotalMiles?.text = "%.1f mi".format(newVal)
////            }
////        }
//    }
//
////    fun myass(valu: String) {
////        println("1111111122234 $valu")
////        tvTotalMiles?.text = valu
////    }
//
//    override fun onServiceConnected() {
//        initOverlay()
//    }
//
//    override fun onInterrupt() {}
//
//    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        when (event?.eventType) {
//            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
//                println("ScreenService::onAccessibilityEvent.TYPE_WINDOW_STATE_CHANGED")
//                handleWindowStateChange(event)
//            }
//            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
//                println("ScreenService::onAccessibilityEvent.TYPE_WINDOWS_CHANGED")
//            }
//            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
//                println("ScreenService::onAccessibilityEvent.TYPE_VIEW_CLICKED")
//            }
//            else -> {
//            }
//        }
//    }
//
//    @SuppressLint("SetTextI18n")
//    private fun handleWindowStateChange(event: AccessibilityEvent) {
////        if (event.source == null) return
////
////        currentScreenText.clear()
////        getAllNodeText(event.source, currentScreenText)
////
////        when (Screen.type(currentScreenText)) {
////            ScreenType.OFFER_SCREEN -> {
////                println("OFFER SCREEN!!!")
////                offerAmount = currentScreenText[3].replace("$", "").toDouble()
////
////                tvOfferAmount?.text = "$%.2f".format(offerAmount)
//////                tvTotalMiles?.text = "whitfuc"
////
//////                var ma = MainActivity()
//////                ma.runOnUiThread(Runnable {
//////                    val ss = ScreenService()
//////                    ss.myass("plz plz plz plz345")
//////                })
////
////                val client = OkHttpClient()
////
////                val req = Request.Builder()
////                    .url("https://jsonplaceholder.typicode.com/todos/1")
////                    .build()
////
////
////
////                client.newCall(req).enqueue(object : Callback {
////                    override fun onFailure(call: Call, e: IOException) {}
////                    override fun onResponse(call: Call, response: Response) {
////
//////                        println("--->>> ${response.body!!.string()}")
////                        println("--->>> DUUUdUUUUUDE?}")
//////                        var ma = MainActivity()
//////                        ma.runOnUiThread(Runnable {
//////                            tvTotalMiles?.text = "freaking out...x...x"
//////                        })
////
////                        ContextCompat.getMainExecutor(baseContext).execute(Runnable {
////                            tvTotalMiles?.text = "is dthis working??"
////                        })
////
////                        response.close()
////                    }
////                })
////
////
//////                 myass("====================")
//////                tvTotalMiles?.text = "%.1f mi".format(deliveryDistance)
////                tvTotalTime?.text = "%.0f mins".format(deliveryTime)
//////                tvDollarsPerMile?.text = "$%.2f/mi".format(offerAmount / deliveryDistance)
////                overlayDelivery?.visibility = View.VISIBLE
////
////                println("")
//////                println(".deliveryDistance >>>>>>>> $deliveryDistance")
////                println(".deliveryTime     >>>>>>>> $deliveryTime")
////                println("offerAmount       >>>>>>>> $offerAmount")
//////                println("dollarsPerMile    >>>>>>>> ${offerAmount / deliveryDistance}")
////            }
////            else -> {
////                overlayDelivery?.visibility = View.GONE
////            }
////        }
//    }
//
////    private fun getAllNodeText(node: AccessibilityNodeInfo, nodeText: MutableList<String>) {
////        if (node.childCount > 0) {
////            for (idx in 0 until node.childCount) {
////                if (!node.getChild(idx).text.isNullOrEmpty()) {
////                    nodeText.add(node.getChild(idx).text.toString())
////                }
////                getAllNodeText(node.getChild(idx), nodeText)
////            }
////        }
////    }
//
//    private fun initOverlay() {
//        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
//        val frame = FrameLayout(this)
//        val inflater = LayoutInflater.from(this)
//        val params = WindowManager.LayoutParams()
//
//        params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
//        params.format = PixelFormat.TRANSLUCENT
//        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//        params.width = WindowManager.LayoutParams.WRAP_CONTENT
//        params.height = WindowManager.LayoutParams.WRAP_CONTENT
//        params.gravity = Gravity.CENTER
//
//        overlayDelivery = inflater.inflate(R.layout.overlay_delivery, frame)
//
//        tvOfferAmount = overlayDelivery?.findViewById(R.id.offerAmount)
//        tvDollarsPerMile = overlayDelivery?.findViewById(R.id.dollarPerMile)
//        tvTotalMiles = overlayDelivery?.findViewById(R.id.totalMiles)
//        tvTotalTime = overlayDelivery?.findViewById(R.id.totalTime)
//
//        overlayDelivery?.visibility = View.GONE
//
//        windowManager.addView(frame, params)
//    }
//}
//
//enum class ScreenType {
//    OFFER_SCREEN,
//    STORE_SCREEN,
//    CUSTOMER_SCREEN,
//    NONE
//}
//
//object Screen {
//    fun type(nodeTextList: MutableList<String>): ScreenType {
//        when {
//            // is offer layout
//            nodeTextList.contains("Accept") && nodeTextList.contains("Decline")
//            -> return ScreenType.OFFER_SCREEN
//        }
//        return ScreenType.NONE
//    }
}