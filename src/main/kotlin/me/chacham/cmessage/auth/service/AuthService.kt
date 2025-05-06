package me.chacham.cmessage.auth.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import me.chacham.cmessage.auth.config.OAuthProperties
import me.chacham.cmessage.auth.domain.OAuthOpenIdTokenDto
import me.chacham.cmessage.auth.domain.UserInfo
import me.chacham.cmessage.auth.infra.IdTokenClaims
import me.chacham.cmessage.user.domain.UserId
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.net.URLEncoder

@Service
class AuthService(
    private val oAuthProperties: OAuthProperties,
    webClientBuilder: WebClient.Builder,
) {
    private val webClient = webClientBuilder.build()

    fun getLineLoginUri(): String {
        val registration = oAuthProperties.registration["line"]
            ?: throw IllegalArgumentException("LINE registration not found")
        val provider = oAuthProperties.provider["line"]
            ?: throw IllegalArgumentException("LINE provider not found")
        val clientId = registration.clientId
        val redirectUri = URLEncoder.encode(registration.redirectUri, Charsets.UTF_8)
        val scope = registration.scope.joinToString("%20")
        val state = 12345 // TODO: Generate and store a random state value

        return provider.authorizationUri +
                "?response_type=code" +
                "&client_id=$clientId" +
                "&redirect_uri=$redirectUri" +
                "&scope=$scope" +
                "&state=$state"
    }

    suspend fun exchangeUserInfo(provider: String, code: String): UserInfo {
        val oAuthTokenDto = exchangeAccessToken(provider, code)
        val idTokenClaims = parseIdTokenClaims(oAuthTokenDto.idToken)
        return UserInfo(
            provider = provider,
            id = UserId(idTokenClaims.subject),
            name = idTokenClaims.name ?: "Unknown",
        )
    }

    suspend fun exchangeAccessToken(provider: String, code: String): OAuthOpenIdTokenDto {
        val registration = oAuthProperties.registration[provider]!!
        val providerInfo = oAuthProperties.provider[provider]!!

        val requestBody = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", registration.authorizationGrantType)
            add("client_id", registration.clientId)
            add("client_secret", registration.clientSecret)
            add("redirect_uri", registration.redirectUri)
            add("code", code)
        }

        return webClient.post()
            .uri(providerInfo.tokenUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromFormData(requestBody))
            .retrieve()
            .awaitBody<OAuthOpenIdTokenDto>()
    }

    suspend fun parseIdTokenClaims(idToken: String): IdTokenClaims {
        val parts = idToken.split(".")
        if (parts.size == 3) {
            val payloadBase64 = parts[1]
            val payloadBytes = java.util.Base64.getUrlDecoder().decode(payloadBase64)
            val payloadJson = String(payloadBytes, Charsets.UTF_8)

            val objectMapper = ObjectMapper().registerModule(kotlinModule())
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            try {
                return objectMapper.readValue(payloadJson, IdTokenClaims::class.java)
            } catch (e: Exception) {
                throw e
            }
        } else {
            throw IllegalArgumentException("INVALID_ID_TOKEN_FORMAT")
        }
    }
}
