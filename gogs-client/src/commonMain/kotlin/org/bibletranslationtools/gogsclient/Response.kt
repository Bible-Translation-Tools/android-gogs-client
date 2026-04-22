package org.bibletranslationtools.gogsclient

data class Response(
    val success: Boolean,
    val code: Int,
    val message: String? = null
)
