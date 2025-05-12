package me.chacham.cmessage.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import kotlinx.coroutines.test.runTest
import me.chacham.cmessage.auth.config.OAuthProperties
import me.chacham.cmessage.auth.domain.OAuthOpenIdTokenDto
import me.chacham.cmessage.auth.infra.IdTokenClaims
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.net.URLEncoder
import java.util.*

class AuthServiceTest {

    private lateinit var authService: AuthService
    private lateinit var mockOAuthProperties: OAuthProperties
    private lateinit var webClientBuilder: WebClient.Builder
    private lateinit var mockWebServer: MockWebServer
    private val objectMapper = ObjectMapper().registerModule(kotlinModule())

    @BeforeEach
    fun setUp() {
        mockOAuthProperties = mock()
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val baseUrl = mockWebServer.url("/").toString()
        webClientBuilder = WebClient.builder().baseUrl(baseUrl)
        authService = AuthService(mockOAuthProperties, webClientBuilder)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getLineLoginUri should return correct URI`() {
        // Given
        val clientId = "test-client-id"
        val clientSecret = "test-client-secret"
        val redirectUri = "http://localhost/callback"
        val authorizationGrantType = "authorization_code"
        val scope = listOf("profile", "openid", "email")
        val encodedRedirectUri = URLEncoder.encode(redirectUri, Charsets.UTF_8)
        val encodedScope = scope.joinToString("%20")
        val expectedAuthUri = "https://custom.authorization.server/auth"

        val lineRegistration = OAuthProperties.Registration(
            clientId = clientId,
            clientSecret = clientSecret,
            redirectUri = redirectUri,
            authorizationGrantType = authorizationGrantType,
            scope = scope
        )
        val lineProvider = OAuthProperties.Provider(
            authorizationUri = expectedAuthUri,
            tokenUri = "https://dummy.token.uri/token"
        )
        whenever(mockOAuthProperties.registration).thenReturn(mapOf("line" to lineRegistration))
        whenever(mockOAuthProperties.provider).thenReturn(mapOf("line" to lineProvider))

        // When
        val loginUri = authService.getLineLoginUri()

        // Then
        assertTrue(loginUri.startsWith(expectedAuthUri))
        assertTrue(loginUri.contains("response_type=code"))
        assertTrue(loginUri.contains("client_id=$clientId"))
        assertTrue(loginUri.contains("redirect_uri=$encodedRedirectUri"))
        assertTrue(loginUri.contains("scope=$encodedScope"))
        assertTrue(loginUri.contains("state=12345"))
    }

    @Test
    fun `getLineLoginUri should throw exception when LINE registration not found`() {
        // Given
        val lineProvider = OAuthProperties.Provider(
            authorizationUri = "https://auth.uri",
            tokenUri = "https://token.uri"
        )
        whenever(mockOAuthProperties.provider).thenReturn(mapOf("line" to lineProvider))
        whenever(mockOAuthProperties.registration).thenReturn(emptyMap())

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.getLineLoginUri()
        }
        assertEquals("LINE registration not found", exception.message)
    }

    @Test
    fun `getLineLoginUri should throw exception when LINE provider configuration not found`() {
        // Given
        val lineRegistration = OAuthProperties.Registration(
            clientId = "id", clientSecret = "secret", redirectUri = "uri",
            authorizationGrantType = "type", scope = listOf("s")
        )
        whenever(mockOAuthProperties.registration).thenReturn(mapOf("line" to lineRegistration))
        whenever(mockOAuthProperties.provider).thenReturn(emptyMap())

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.getLineLoginUri()
        }
        assertEquals("LINE provider not found", exception.message)
    }


    @Test
    fun `exchangeAccessToken should return OAuthOpenIdTokenDto on success`() = runTest {
        // Given
        val provider = "line"
        val code = "test-code"
        val clientId = "test-client-id"
        val clientSecret = "test-client-secret"
        val redirectUri = "http://localhost/callback"
        val tokenUri = mockWebServer.url("/token").toString()
        val scope = listOf("profile", "openid")

        val registration = OAuthProperties.Registration(
            clientId = clientId,
            clientSecret = clientSecret,
            redirectUri = redirectUri,
            authorizationGrantType = "authorization_code",
            scope = scope
        )
        val providerInfo = OAuthProperties.Provider(
            authorizationUri = "https://dummy.auth.uri",
            tokenUri = tokenUri
        )

        whenever(mockOAuthProperties.registration).thenReturn(mapOf(provider to registration))
        whenever(mockOAuthProperties.provider).thenReturn(mapOf(provider to providerInfo))

        val expectedTokenResponse = OAuthOpenIdTokenDto(
            accessToken = "mockAccessToken",
            idToken = "mockIdToken",
            expiresIn = 3600,
            refreshToken = "mockRefreshToken",
            scope = "profile openid",
            tokenType = "Bearer",
            refreshTokenExpiresIn = 2592000
        )
        mockWebServer.enqueue(
            MockResponse()
                .setBody(objectMapper.writeValueAsString(expectedTokenResponse))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        )

        // When
        val tokenDto = authService.exchangeAccessToken(provider, code)

        // Then
        assertEquals(expectedTokenResponse, tokenDto)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/token", recordedRequest.path)
        assertEquals("POST", recordedRequest.method)
        val requestBody = recordedRequest.body.readUtf8()
        assertTrue(requestBody.contains("grant_type=authorization_code"))
        assertTrue(requestBody.contains("client_id=$clientId"))
        assertTrue(requestBody.contains("client_secret=$clientSecret"))
        assertTrue(requestBody.contains("redirect_uri=${URLEncoder.encode(redirectUri, Charsets.UTF_8)}"))
        assertTrue(requestBody.contains("code=$code"))
    }

    @Test
    fun `parseIdTokenClaims should return IdTokenClaims for valid token`() = runTest {
        // Given
        val claims = IdTokenClaims(
            issuer = "https://access.line.me",
            subject = "U1234567890abcdef1234567890abcdef",
            audience = listOf("1234567890"),
            expiresAt = System.currentTimeMillis() / 1000 + 3600,
            issuedAt = System.currentTimeMillis() / 1000,
            name = "Test User"
        )
        val payloadJson = objectMapper.writeValueAsString(claims)
        val encodedPayload =
            Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.toByteArray(Charsets.UTF_8))
        val idToken = "header.$encodedPayload.signature"

        // When
        val parsedClaims = authService.parseIdTokenClaims(idToken)

        // Then
        assertEquals(claims.subject, parsedClaims.subject)
        assertEquals(claims.name, parsedClaims.name)
        assertEquals(claims.audience, parsedClaims.audience)
    }

    @Test
    fun `parseIdTokenClaims should throw exception for invalid JWT format`() = runTest {
        // Given
        val idToken = "invalid.token"

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.parseIdTokenClaims(idToken)
        }
        assertEquals("INVALID_ID_TOKEN_FORMAT", exception.message)
    }

    @Test
    fun `parseIdTokenClaims should throw exception for malformed payload`() = runTest {
        // Given
        val malformedPayload =
            Base64.getUrlEncoder().withoutPadding().encodeToString("{not_a_json".toByteArray(Charsets.UTF_8))
        val idToken = "header.$malformedPayload.signature"

        // When & Then
        assertThrows<Exception> {
            authService.parseIdTokenClaims(idToken)
        }
    }


    @Test
    fun `exchangeUserInfo should return UserInfo`() = runTest {
        // Given
        val provider = "line"
        val code = "test-code"
        val mockAccessToken = "mockAccessToken"

        val mockClaims = IdTokenClaims(
            issuer = "iss",
            subject = "user_subject_id",
            audience = listOf("aud"),
            expiresAt = 1L,
            issuedAt = 0L,
            name = "Test User Name"
        )
        val payloadJson = objectMapper.writeValueAsString(mockClaims)
        val encodedPayload =
            Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.toByteArray(Charsets.UTF_8))
        val correctlyFormattedMockIdTokenForParsing = "header.$encodedPayload.signature"

        val mockTokenDto = OAuthOpenIdTokenDto(
            accessToken = mockAccessToken,
            idToken = correctlyFormattedMockIdTokenForParsing,
            expiresIn = 3600,
            refreshToken = "mockRefreshToken",
            scope = "profile openid",
            tokenType = "Bearer",
            refreshTokenExpiresIn = 2592000
        )

        val registration = OAuthProperties.Registration(
            clientId = "id",
            clientSecret = "secret",
            redirectUri = "uri",
            authorizationGrantType = "type",
            scope = listOf("profile", "openid")
        )
        whenever(mockOAuthProperties.registration).thenReturn(mapOf(provider to registration))

        val providerInfo = OAuthProperties.Provider(
            authorizationUri = "https://dummy.auth.uri",
            tokenUri = mockWebServer.url("/token").toString()
        )
        whenever(mockOAuthProperties.provider).thenReturn(mapOf(provider to providerInfo))

        mockWebServer.enqueue(
            MockResponse()
                .setBody(objectMapper.writeValueAsString(mockTokenDto))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        )

        // When
        val userInfo = authService.exchangeUserInfo(provider, code)

        // Then
        assertEquals(provider, userInfo.provider)
        assertEquals(mockClaims.subject, userInfo.id.id)
        assertEquals(mockClaims.name, userInfo.name)
    }

    @Test
    fun `exchangeUserInfo should use 'Unknown' for name if IdTokenClaims name is null`() = runTest {
        // Given
        val provider = "line"
        val code = "test-code"

        val mockClaimsNoName = IdTokenClaims(
            issuer = "iss", subject = "user_subject_id_no_name", audience = listOf("aud"),
            expiresAt = 1L, issuedAt = 0L, name = null
        )
        val payloadJsonNoName = objectMapper.writeValueAsString(mockClaimsNoName)
        val encodedPayloadNoName =
            Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJsonNoName.toByteArray(Charsets.UTF_8))
        val idTokenNoNameForParsing = "header.$encodedPayloadNoName.signature"

        val mockTokenDto = OAuthOpenIdTokenDto(
            accessToken = "accessToken",
            idToken = idTokenNoNameForParsing,
            expiresIn = 3600,
            refreshToken = "refreshToken",
            scope = "scope",
            tokenType = "Bearer",
            refreshTokenExpiresIn = 2592000
        )

        val registration = OAuthProperties.Registration(
            clientId = "id",
            clientSecret = "secret",
            redirectUri = "uri",
            authorizationGrantType = "type",
            scope = listOf("profile", "openid")
        )
        whenever(mockOAuthProperties.registration).thenReturn(mapOf(provider to registration))

        val providerInfo = OAuthProperties.Provider(
            authorizationUri = "https://dummy.auth.uri",
            tokenUri = mockWebServer.url("/token").toString()
        )
        whenever(mockOAuthProperties.provider).thenReturn(mapOf(provider to providerInfo))

        mockWebServer.enqueue(
            MockResponse()
                .setBody(objectMapper.writeValueAsString(mockTokenDto))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        )

        // When
        val userInfo = authService.exchangeUserInfo(provider, code)

        // Then
        assertEquals(provider, userInfo.provider)
        assertEquals(mockClaimsNoName.subject, userInfo.id.id)
        assertEquals("Unknown", userInfo.name)
    }
}
