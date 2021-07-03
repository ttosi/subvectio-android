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

        try {
            if (notificationHash != lastNotificationHash) {
                lastNotificationHash = notificationHash

                when (notificationExtras?.getString("android.title")) {
                    "New Delivery!" -> "new"
                    else -> return
                }

                Logger.log("*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*")
                Logger.log("---TITLE----- ${notificationExtras.getString("android.title")}")
                Logger.log("---TEXT------ ${notificationExtras.getString("android.text")}")
                Logger.log("---BEGIN BIGTEXT---\n${notificationExtras.getString("android.bigText")}")
                Logger.log("---END BIGTEXT---")


                val bigtext = notificationExtras.getString("android.bigText")
                val storeName = notificationExtras.getString("android.text")?.substring(17)
                val addressData = bigtext?.split("\n")?.toTypedArray()
                // todo: need to do better checking before parsing - dude
                val distances = Regex("approx (.*?) mi").findAll(bigtext!!).map { it.groupValues[1].toDouble() }.toList()

                if (addressData!!.count() > 5) {
                    val deliveryIntent = Intent()
                    deliveryIntent.putExtra("sourcePackageName", sbn.packageName)
                    deliveryIntent.putExtra("storeName", storeName)
                    deliveryIntent.putExtra("storeAddress", addressData[3])
                    deliveryIntent.putExtra("customerAddress", addressData[5])
                    deliveryIntent.putExtra("distance", distances[0] + distances[1])
                    deliveryIntent.action = "com.tdc.subvectio.NEW_DELIVERY"
                    sendBroadcast(deliveryIntent)
                }
            }
        } catch (ex: Exception) {
            Logger.log(ex.stackTraceToString(), true)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }
}

