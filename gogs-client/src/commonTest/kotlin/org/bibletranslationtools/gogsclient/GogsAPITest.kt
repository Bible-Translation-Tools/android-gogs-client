package org.bibletranslationtools.gogsclient

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.*

class GogsAPITest {

    private fun makeClient(handler: MockRequestHandler): HttpClient =
        HttpClient(MockEngine(handler)) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

    private fun api(handler: MockRequestHandler) =
        GogsAPI("https://test.gogs.io/api/v1", client = makeClient(handler))

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun testGetUserSuccess() = runTest {
        val a = api {
            respond("""{"id":1,"username":"alice","email":"alice@test.com"}""", HttpStatusCode.OK, jsonHeaders())
        }
        val user = a.getUser(User(username = "alice"), null)
        assertNotNull(user)
        assertEquals("alice", user.username)
        assertEquals("alice@test.com", user.email)
    }

    @Test
    fun testGetUserNotFound() = runTest {
        val a = api { respond("{}", HttpStatusCode.NotFound, jsonHeaders()) }
        assertNull(a.getUser(User(username = "ghost"), null))
    }

    @Test
    fun testCreateUserSuccess() = runTest {
        val a = api { respond("""{"id":2,"username":"bob","email":"bob@test.com"}""", HttpStatusCode.Created, jsonHeaders()) }
        val result = a.createUser(
            User(username = "bob", email = "bob@test.com", password = "pass"),
            User(username = "admin", token = Token(id = 0, name = "t", sha1 = "tok123")),
            notify = false
        )
        assertNotNull(result)
        assertEquals("bob", result.username)
    }

    @Test
    fun testCreateUserUnauthorized() = runTest {
        val a = api { respond("{}", HttpStatusCode.Unauthorized, jsonHeaders()) }
        assertNull(a.createUser(
            User(username = "bob", email = "bob@test.com", password = "pass"),
            User(username = "notadmin", password = "wrong"),
            notify = false
        ))
    }

    @Test
    fun testDeleteUserSuccess() = runTest {
        val a = api { respond("", HttpStatusCode.NoContent, headersOf()) }
        assertTrue(a.deleteUser(
            User(username = "alice"),
            User(username = "admin", token = Token(id = 0, name = "t", sha1 = "tok"))
        ))
    }

    @Test
    fun testDeleteUserSelfPrevented() = runTest {
        var requestMade = false
        val a = api { requestMade = true; respond("", HttpStatusCode.NoContent, headersOf()) }
        val user = User(username = "alice", password = "pass")
        assertFalse(a.deleteUser(user, user))
        assertFalse(requestMade)
    }

    @Test
    fun testSearchUsersSuccess() = runTest {
        val a = api { respond("""{"ok":true,"data":[{"username":"alice"},{"username":"alex"}]}""", HttpStatusCode.OK, jsonHeaders()) }
        val users = a.searchUsers("al", 5, null)
        assertEquals(2, users.size)
        assertEquals("alice", users[0].username)
    }

    @Test
    fun testSearchUsersBlankQuery() = runTest {
        var requestMade = false
        val a = api { requestMade = true; respond("{}", HttpStatusCode.OK, jsonHeaders()) }
        assertTrue(a.searchUsers("   ", 5, null).isEmpty())
        assertFalse(requestMade)
    }

    @Test
    fun testSearchReposSuccess() = runTest {
        val a = api { respond("""{"ok":true,"data":[{"id":1,"name":"demo-repo"}]}""", HttpStatusCode.OK, jsonHeaders()) }
        val repos = a.searchRepos("demo", 0, 5)
        assertEquals(1, repos.size)
        assertEquals("demo-repo", repos[0].name)
    }

    @Test
    fun testListReposSuccess() = runTest {
        val a = api { respond("""[{"id":1,"name":"my-repo","full_name":"alice/my-repo"}]""", HttpStatusCode.OK, jsonHeaders()) }
        val repos = a.listRepos(User(username = "alice", password = "pass"))
        assertEquals(1, repos.size)
        assertEquals("my-repo", repos[0].name)
    }

    @Test
    fun testCreateRepoSuccess() = runTest {
        val a = api { respond("""{"id":1,"name":"new-repo","full_name":"alice/new-repo"}""", HttpStatusCode.Created, jsonHeaders()) }
        val result = a.createRepo(
            Repository(name = "new-repo", description = "test", isPrivate = false),
            User(username = "alice", password = "pass")
        )
        assertNotNull(result)
        assertEquals("new-repo", result.name)
    }

    @Test
    fun testDeleteRepoSuccess() = runTest {
        val a = api { respond("", HttpStatusCode.NoContent, headersOf()) }
        assertTrue(a.deleteRepo(
            Repository(name = "my-repo", fullName = "alice/my-repo"),
            User(username = "alice", password = "pass")
        ))
    }

    @Test
    fun testCreateTokenSuccess() = runTest {
        val a = api { respond("""{"id":1,"name":"my-token","sha1":"abc123def456"}""", HttpStatusCode.Created, jsonHeaders()) }
        val result = a.createToken(Token(id = 0, name = "my-token"), User(username = "alice", password = "pass"))
        assertNotNull(result)
        assertEquals("my-token", result.name)
        assertEquals("abc123def456", result.sha1)
    }

    @Test
    fun testListTokensSuccess() = runTest {
        val a = api { respond("""[{"id":1,"name":"tok1","sha1":"aaa"},{"id":2,"name":"tok2","sha1":"bbb"}]""", HttpStatusCode.OK, jsonHeaders()) }
        val tokens = a.listTokens(User(username = "alice", password = "pass"))
        assertEquals(2, tokens.size)
        assertEquals("tok1", tokens[0].name)
    }

    @Test
    fun testCreatePublicKeySuccess() = runTest {
        val a = api { respond("""{"id":1,"title":"my-key","key":"ssh-rsa AAAA..."}""", HttpStatusCode.Created, jsonHeaders()) }
        val result = a.createPublicKey(
            PublicKey(title = "my-key", key = "ssh-rsa AAAA..."),
            User(username = "alice", password = "pass")
        )
        assertNotNull(result)
        assertEquals("my-key", result.title)
    }

    @Test
    fun testDeletePublicKeySuccess() = runTest {
        val a = api { respond("", HttpStatusCode.NoContent, headersOf()) }
        assertTrue(a.deletePublicKey(PublicKey(id = 1), User(username = "alice", password = "pass")))
    }

    @Test
    fun testAuthUsesTokenHeader() = runTest {
        var capturedAuth: String? = null
        val a = api { request ->
            capturedAuth = request.headers[HttpHeaders.Authorization]
            respond("""{"id":1,"username":"alice"}""", HttpStatusCode.OK, jsonHeaders())
        }
        val authUser = User(username = "alice", token = Token(id = 0, name = "t", sha1 = "mytoken123"))
        a.getUser(User(username = "alice"), authUser)
        assertEquals("token mytoken123", capturedAuth)
    }

    @Test
    fun testAuthUsesBasicHeader() = runTest {
        var capturedAuth: String? = null
        val a = api { request ->
            capturedAuth = request.headers[HttpHeaders.Authorization]
            respond("""{"id":1,"username":"alice"}""", HttpStatusCode.OK, jsonHeaders())
        }
        val user = User(username = "alice", password = "secret")
        a.getUser(user, user)
        assertTrue(capturedAuth?.startsWith("Basic ") == true)
    }
}
