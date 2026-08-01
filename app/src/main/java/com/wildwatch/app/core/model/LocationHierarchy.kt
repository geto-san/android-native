package com.wildwatch.app.core.model

data class District(
    val id: String = "",
    val label: String = "",
    val sub_counties: List<SubCounty> = emptyList()
)

data class SubCounty(
    val id: String = "",
    val label: String = "",
    val parishes: List<String> = emptyList()
)
