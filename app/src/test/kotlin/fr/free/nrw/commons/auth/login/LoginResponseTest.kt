package fr.free.nrw.commons.auth.login

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginResponseTest {
    private val gson = Gson()

    private fun toLoginResult(clientLoginJson: String): LoginResult? =
        gson
            .fromJson("""{"clientlogin": $clientLoginJson}""", LoginResponse::class.java)
            .toLoginResult("pw")

    @Test
    fun unknownStatusWithoutMessageReturnsNullMessage() {
        val result = toLoginResult("""{"status": "RESTART"}""")

        assertEquals("RESTART", result?.status)
        assertNull(result?.message)
    }

    @Test
    fun unknownStatusKeepsServerMessage() {
        val result = toLoginResult("""{"status": "RESTART", "message": "Server message"}""")

        assertEquals("Server message", result?.message)
    }

    @Test
    fun failStatusKeepsServerMessage() {
        val result =
            toLoginResult(
                """{"status": "FAIL", "message": "Incorrect username or password entered."}""",
            )

        assertEquals("Incorrect username or password entered.", result?.message)
    }

    @Test
    fun passStatusIsPass() {
        val result = toLoginResult("""{"status": "PASS", "username": "TestUser"}""")

        assertTrue(result?.pass == true)
        assertEquals("TestUser", result?.userName)
    }

    @Test
    fun apiErrorsArrayGivesErrorMessage() {
        val response =
            gson.fromJson(
                """{"errors":[{"code":"badtoken","text":"Invalid CSRF token."}]}""",
                LoginResponse::class.java,
            )

        assertNull(response.toLoginResult("pw"))
        assertEquals("Invalid CSRF token.", response.errorMessage())
    }

    @Test
    fun apiErrorWithoutTextFallsBackToCode() {
        val response =
            gson.fromJson("""{"errors":[{"code":"badtoken"}]}""", LoginResponse::class.java)

        assertEquals("badtoken", response.errorMessage())
    }

    @Test
    fun responseWithoutApiErrorHasNoErrorMessage() {
        val response =
            gson.fromJson("""{"clientlogin":{"status":"PASS"}}""", LoginResponse::class.java)

        assertNull(response.errorMessage())
    }
}
