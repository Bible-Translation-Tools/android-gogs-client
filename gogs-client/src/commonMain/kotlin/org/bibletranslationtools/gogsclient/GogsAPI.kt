package org.bibletranslationtools.gogsclient

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class GogsAPI(
    apiUrl: String,
    readTimeoutMs: Long = 30_000,
    connectTimeoutMs: Long = 30_000,
    private val userAgent: String? = null,
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = readTimeoutMs
            connectTimeoutMillis = connectTimeoutMs
        }
    }
) {
    private val baseUrl = apiUrl.trimEnd('/') + "/"
    private var _lastResponse: Response? = null

    @OptIn(ExperimentalEncodingApi::class)
    private fun authHeader(user: User?): String? {
        val sha1 = user?.token?.sha1
        if (sha1 != null) return "token $sha1"
        val u = user?.username
        val p = user?.password
        if (!u.isNullOrEmpty() && !p.isNullOrEmpty()) {
            return "Basic ${Base64.encode("$u:$p".encodeToByteArray())}"
        }
        return null
    }

    private fun HttpRequestBuilder.setupRequest(user: User?) {
        authHeader(user)?.let { header(HttpHeaders.Authorization, it) }
        userAgent?.let { header(HttpHeaders.UserAgent, it) }
        contentType(ContentType.Application.Json)
    }

    private suspend inline fun <reified T> getResource(path: String, user: User?): T? {
        return try {
            val response = client.get(baseUrl + path.trimStart('/')) {
                setupRequest(user)
            }
            _lastResponse = Response(
                response.status.isSuccess(),
                response.status.value,
                response.status.description
            )
            if (response.status.isSuccess()) response.body<T>() else null
        } catch (e: Exception) {
            _lastResponse = Response(false, -1, e.message)
            null
        }
    }

    private suspend inline fun <reified T> postResource(
        path: String,
        user: User?,
        body: JsonObject
    ): T? {
        return try {
            val response = client.post(baseUrl + path.trimStart('/')) {
                setupRequest(user)
                setBody(body)
            }
            _lastResponse = Response(
                response.status.isSuccess(),
                response.status.value,
                response.status.description
            )
            if (response.status.isSuccess()) response.body<T>() else null
        } catch (e: Exception) {
            _lastResponse = Response(false, -1, e.message)
            null
        }
    }

    private suspend inline fun <reified T> patchResource(
        path: String,
        user: User?,
        body: JsonObject
    ): T? {
        return try {
            val response = client.patch(baseUrl + path.trimStart('/')) {
                setupRequest(user)
                setBody(body)
            }
            _lastResponse = Response(
                response.status.isSuccess(),
                response.status.value,
                response.status.description
            )
            if (response.status.isSuccess()) response.body<T>() else null
        } catch (e: Exception) {
            _lastResponse = Response(false, -1, e.message)
            null
        }
    }

    private suspend fun deleteResource(path: String, user: User?): Boolean {
        return try {
            val response = client.delete(baseUrl + path.trimStart('/')) {
                setupRequest(user)
            }
            val success = response.status == HttpStatusCode.NoContent
            _lastResponse = Response(
                success,
                response.status.value,
                response.status.description
            )
            success
        } catch (e: Exception) {
            _lastResponse = Response(false, -1, e.message)
            false
        }
    }

    fun getLastResponse(): Response? = _lastResponse

    // Users

    suspend fun createUser(user: User, authUser: User, notify: Boolean): User? =
        postResource("/admin/users", authUser, buildJsonObject {
            put("username", user.username)
            user.email?.let { put("email", it) }
            user.password?.let { put("password", it) }
            user.fullName?.let { put("full_name", it) }
            put("send_notify", notify)
        })

    suspend fun editUser(user: User, authUser: User): User? =
        patchResource("/admin/users/${user.username}", authUser, buildJsonObject {
            put("login_name", user.loginName ?: user.username)
            put("source_id", 0)
            user.fullName?.let { put("full_name", it) }
            user.email?.let { put("email", it) }
            user.password?.let { put("password", it) }
            user.website?.let { put("website", it) }
            user.location?.let { put("location", it) }
            put("active", user.active)
            put("admin", user.admin)
            put("allow_git_hook", user.allowGitHook)
            put("allow_import_local", user.allowImportLocal)
        })

    suspend fun deleteUser(user: User, authUser: User): Boolean {
        if (user.username == authUser.username) return false
        return deleteResource("/admin/users/${user.username}", authUser)
    }

    suspend fun searchUsers(query: String, limit: Int, authUser: User?): List<User> {
        if (query.isBlank()) return emptyList()
        val result = getResource<SearchUsersResult>(
            "/users/search?q=$query&limit=$limit",
            authUser
        )
        return if (result?.ok == true) result.data else emptyList()
    }

    suspend fun getUser(user: User, authUser: User?): User? =
        getResource("/users/${user.username}", authUser)

    // Repositories

    suspend fun searchRepos(query: String, uid: Int, limit: Int): List<Repository> {
        if (query.isBlank()) return emptyList()
        val result = getResource<SearchReposResult>(
            "/repos/search?q=${query.trim()}&uid=$uid&limit=$limit",
            null
        )
        return if (result?.ok == true) result.data else emptyList()
    }

    suspend fun createRepo(repo: Repository, user: User): Repository? =
        postResource("/user/repos", user, buildJsonObject {
            put("name", repo.name)
            repo.description?.let { put("description", it) }
            put("private", repo.isPrivate)
        })

    suspend fun getRepo(repo: Repository, authUser: User?): Repository? =
        getResource("/repos/${repo.fullName}", authUser)

    suspend fun listRepos(user: User): List<Repository> =
        getResource("/user/repos", user) ?: emptyList()

    suspend fun deleteRepo(repo: Repository, user: User): Boolean =
        deleteResource("/repos/${user.username}/${repo.name}", user)

    // Tokens

    suspend fun createToken(token: Token, user: User): Token? =
        postResource("/users/${user.username}/tokens", user, buildJsonObject {
            put("name", token.name)
            put("scopes", buildJsonArray {
                token.scopes.forEach { add(JsonPrimitive(it)) }
            })
        })

    suspend fun deleteToken(id: Int, user: User): Boolean =
        deleteResource("/users/${user.username}/tokens/$id", user)

    suspend fun listTokens(user: User): List<Token> =
        getResource("/users/${user.username}/tokens", user) ?: emptyList()

    // Public Keys

    suspend fun createPublicKey(key: PublicKey, user: User): PublicKey? =
        postResource("/user/keys", user, buildJsonObject {
            put("title", key.title)
            put("key", key.key)
        })

    suspend fun listPublicKeys(user: User): List<PublicKey> =
        getResource("/users/${user.username}/keys", user) ?: emptyList()

    suspend fun getPublicKey(key: PublicKey, user: User): PublicKey? =
        getResource("/user/keys/${key.id}", user)

    suspend fun deletePublicKey(key: PublicKey, user: User): Boolean =
        deleteResource("/user/keys/${key.id}", user)
}

@Serializable
private data class SearchUsersResult(val ok: Boolean, val data: List<User>)

@Serializable
private data class SearchReposResult(val ok: Boolean, val data: List<Repository>)
