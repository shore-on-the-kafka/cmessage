package me.chacham.cmessage.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "oauth2.client")
data class OAuthProperties(
    val registration: Map<String, Registration>,
    val provider: Map<String, Provider>,
) {
    data class Registration(
        val clientId: String,
        val clientSecret: String,
        val redirectUri: String,
        val authorizationGrantType: String,
        val scope: List<String>,
    )

    data class Provider(
        val authorizationUri: String,
        val tokenUri: String,
    )
}
