package org.bibletranslationtools.gogsclient

import kotlinx.serialization.Serializable

@Serializable
data class Token(
    val name: String,
    val sha1: String? = null,
    val scopes: List<String> = listOf("all"),
    val id: Int = 0,
)
