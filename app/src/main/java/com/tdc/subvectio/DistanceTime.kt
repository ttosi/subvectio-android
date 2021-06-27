package com.tdc.subvectio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DistanceTime(
    @SerialName("destination_addresses")
    val destinationAddresses: List<String>,

    @SerialName("origin_addresses")
    val originAddresses: List<String>,
    val rows: List<Row>,
    val status: String
)  : java.io.Serializable

@Serializable
data class Row(
    val elements: List<Element>
) : java.io.Serializable

@Serializable
data class Element(
    val distance: Distance,
    val duration: Distance,

    @SerialName("duration_in_traffic")
    val durationInTraffic: Distance,
    val status: String
) : java.io.Serializable

@Serializable
data class Distance(
    val text: String,
    val value: Long
) : java.io.Serializable
