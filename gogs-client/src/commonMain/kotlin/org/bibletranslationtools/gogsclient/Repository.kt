package org.bibletranslationtools.gogsclient

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Repository(
    val name: String = "",
    val description: String = "",
    @SerialName("full_name")
    val fullName: String = "",
    @SerialName("private")
    val isPrivate: Boolean = false,
    val fork: Boolean = false,
    @SerialName("html_url")
    val htmlUrl: String = "",
    @SerialName("clone_url")
    val cloneUrl: String = "",
    @SerialName("ssh_url")
    val sshUrl: String = "",
    val owner: User? = null,
    val id: Int = 0
)
