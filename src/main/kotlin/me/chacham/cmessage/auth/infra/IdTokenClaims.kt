package me.chacham.cmessage.auth.infra

import com.fasterxml.jackson.annotation.JsonProperty

data class IdTokenClaims(
    @JsonProperty("iss") val issuer: String,
    @JsonProperty("sub") val subject: String,
    @JsonProperty("aud") val audience: List<String>,
    @JsonProperty("exp") val expiresAt: Long,
    @JsonProperty("iat") val issuedAt: Long,

    val nonce: String? = null,
    @JsonProperty("amr") val authenticationMethodsReference: List<String>? = null,
    val name: String? = null,
)
