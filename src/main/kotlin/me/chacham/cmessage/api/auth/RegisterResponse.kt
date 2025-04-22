package me.chacham.cmessage.api.auth

sealed class RegisterResponse

data class RegisterSuccessResponse(
    val id: String,
    val username: String,
) : RegisterResponse()

data class RegisterFailureResponse(
    val error: String,
) : RegisterResponse()
