package com.tdc.subvectio

import java.time.LocalDateTime

class Delivery {
    var merchantName: String = ""
    var merchantAddress: String = ""
    var customerAddress: String = ""

    var deliveryDistance: Double = 0.0
    var deliveryTime: Double = 0.0
    var offerAmount: Double = 0.0

    var tip: Double = 0.0
    var basePay: Double = 0.0
    var peakPay: Double = 0.0

    var acceptedAt: LocalDateTime = LocalDateTime.MIN
    var arrivedMerchantAt: LocalDateTime = LocalDateTime.MIN
    var departedMerchantAt: LocalDateTime = LocalDateTime.MIN
    var arrivedCustAt: LocalDateTime = LocalDateTime.MIN
    var completedAt: LocalDateTime = LocalDateTime.MIN

    var isActive: Boolean = false
    var isDeclined: Boolean = false
}