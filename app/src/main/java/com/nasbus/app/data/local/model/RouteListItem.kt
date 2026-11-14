package com.nasbus.app.data.local.model

data class RouteListItem(
    val routeId: Int,
    val routeName: String,
    val companyName: String,
    val originName: String,
    val destinationName: String
) {
    val displayText: String
        get() = "$routeId - $routeName ($companyName): $originName to $destinationName"

    override fun toString(): String = displayText
}
