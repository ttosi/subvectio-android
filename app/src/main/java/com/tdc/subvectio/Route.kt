package com.tdc.subvectio

import kotlinx.serialization.Serializable

@Serializable
data class Route (
    val duration: Double,
    val distance: Double,
    val tags: List<Tag>
) : java.io.Serializable

@Serializable
data class Tag (
    val name: String,
    val textColor: String,
    val backgroundColor: String,
    val type: String
) : java.io.Serializable
