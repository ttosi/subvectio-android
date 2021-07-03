// adb shell tail -f /storage/self/primary/Android/data/com.tdc.subvectio/files/log.txt
// adb shell rm -f /storage/self/primary/Android/data/com.tdc.subvectio/files/log.txt

package com.tdc.subvectio

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val dir: File = File("/storage/self/primary/Android/data/com.tdc.subvectio/files/")
val log: File = File(dir, "activity.log")
val dataFile: File = File(dir, "dash.csv")
val exLog: File = File(dir, "exception.log")

class Logger {
    companion object {
        fun log(message: String?, isEx: Boolean = false) {
            if (message == null) return

            val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val formattedDate = LocalDateTime.now().format(dateFormat)

            if(!log.exists()) log.createNewFile()
            if(!exLog.exists()) exLog.createNewFile()

            val lines = message.split("\n").filter { it.isNotEmpty() }.toTypedArray()
            when(lines.count()) {
                1 -> {
                    when(isEx) {
                        true -> exLog.appendText("$message\n")
                        else -> log.appendText("[${formattedDate}] $message\n")
                    }
                }
                else -> {
                    for(line in lines) {
                        when(isEx) {
                            true -> exLog.appendText("$line\n")
                            else -> log.appendText("[${formattedDate}] $line\n")
                        }
                    }
                }
            }

            if(isEx) exLog.appendText("*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*--*-*-*-*-*-*\n")
        }

        fun data(d: Delivery) {
            if(!dataFile.exists()) {
                dataFile.createNewFile()
                dataFile.appendText("storeName,storeAddress,customerAddress,deliveryDistance,distance,deliveryTime,deliveryIndex,offer,tip,basePay,peakPay,actual,acceptedAt,arrivedAtStore,departedStoreAt,completedAt,wasDeclined\n")
            }

            dataFile.appendText("\"${d.storeName}\",\"${d.storeAddress}\",\"${d.customerAddress}\",${d.deliveryDistance},${d.distance},${d.deliveryTime},${d.deliveryIndex},${d.offer},${d.tip},${d.basePay},${d.peakPay},${d.actual},${d.acceptedAt},${d.arrivedStoreAt},${d.departedStoreAt},${d.completedAt},${d.wasDeclined}\n")
        }

        fun close() {
            if(log.exists()) {
                val dateFormat = DateTimeFormatter.ofPattern("ddHHmm")
                val backup = File(dir, "activity_${LocalDateTime.now().format(dateFormat)}.log")
                log.renameTo(backup)
            }
        }
    }
}