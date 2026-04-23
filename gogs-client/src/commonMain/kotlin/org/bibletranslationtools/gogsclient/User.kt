package org.bibletranslationtools.gogsclient

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int = 0,
    val username: String = "",
    val password: String? = null,
    val email: String? = null,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("login_name")
    val loginName: String? = null,
    val website: String? = null,
    val location: String? = null,
    val active: Boolean = true,
    val admin: Boolean = false,
    @SerialName("allow_git_hook")
    val allowGitHook: Boolean = false,
    @SerialName("allow_import_local")
    val allowImportLocal: Boolean = false,
    val token: Token? = null
)
