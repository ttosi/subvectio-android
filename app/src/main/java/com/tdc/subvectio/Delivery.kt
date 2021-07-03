package com.tdc.subvectio

import java.time.LocalDateTime

class Shift {
    var startAt: LocalDateTime = LocalDateTime.MIN
    var endAt: LocalDateTime = LocalDateTime.MIN

    var numOffers: Int = 0
    var numAccepted: Int = 0
    var numDeclined: Int = 0
    var numUnassigned: Int = 0

    var routes: MutableList<Route> = mutableListOf()
}

class Route {
    var deliveryCount: Int = 0
    var deliveries: MutableList<Delivery> = mutableListOf()

    var acceptedAt: LocalDateTime = LocalDateTime.MIN
    var completedAt: LocalDateTime = LocalDateTime.MIN
}

class Delivery {
    var storeName: String? = ""
    var storeAddress: String? = ""
    var customerAddress: String? = ""

    var deliveryDistance: Double = 0.0
    var distance: Double? = 0.0
    var deliveryTime: Double = 0.0
    var deliveryIndex: Double = 0.0

    var offer: Double? = 0.0
    var tip: Double = 0.0
    var basePay: Double = 0.0
    var peakPay: Double = 0.0
    var actual: Double = 0.0

    var acceptedAt: LocalDateTime = LocalDateTime.MIN
    var arrivedStoreAt: LocalDateTime = LocalDateTime.MIN
    var departedStoreAt: LocalDateTime = LocalDateTime.MIN
    var completedAt: LocalDateTime = LocalDateTime.MIN

    var isActive: Boolean = false
    var wasDeclined: Boolean = false
}