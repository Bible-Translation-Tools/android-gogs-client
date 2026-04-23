package org.bibletranslationtools.gogsclient

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicKey(
    val id: Int = 0,
    val title: String = "",
    val key: String = "",
    val url: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
