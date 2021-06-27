package com.tdc.subvectio

import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.math.BigInteger
import java.security.MessageDigest

enum class DeliveryType {
    NEW_DELIVERY,
    NEW_STACKED_DELIVERY,
    UPDATED_DELIVERY,
    MISSED_DELIVERY
}

class NotificationService : NotificationListenerService() {
    var lastNotificationHash: String = ""

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        if (!sbn.packageName.equals("com.tdc.subvectio_dummy") &&
            !sbn.packageName.equals("com.doordash.driverapp")
        ) return

        val notificationExtras = sbn.notification?.extras
        val notificationHash = md5("${notificationExtras?.getString("android.text")}${notificationExtras?.getString("android.bigText")}")

        if (notificationHash != lastNotificationHash) {
            lastNotificationHash = notificationHash

            when (notificationExtras?.getString("android.title")) {
                "New Delivery!" -> "new"
                "Missed Delivery" -> "miss"
                "Delivery Update" -> "update"
                else -> return
            }

            println("****************************************")
            println("---TITLE-------\r\n ${notificationExtras.getString("android.title")}")
            println("---TEXT--------\r\n ${notificationExtras.getString("android.text")}")
            println("--BIGTEXT-----\r\n ${notificationExtras.getString("android.bigText")}")
            println("****************************************")

            val merchantName = notificationExtras.getString("android.text")?.substring(17)
            val addressData = notificationExtras.getString("android.bigText")?.split("\n")?.toTypedArray()

            if (addressData!!.count() > 5) {
                val deliveryIntent = Intent()
                deliveryIntent.putExtra("sourcePackageName", sbn.packageName)
                deliveryIntent.putExtra("merchantName", merchantName)
                deliveryIntent.putExtra("merchantAddress", addressData[3])
                deliveryIntent.putExtra("customerAddress", addressData[5])
                deliveryIntent.action = "com.tdc.subvectio.NEW_DELIVERY"
                sendBroadcast(deliveryIntent)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }
}

