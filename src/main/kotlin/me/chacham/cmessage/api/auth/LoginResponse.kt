package me.chacham.cmessage.api.auth

sealed class LoginResponse

data class LoginSuccessResponse(val token: String) : LoginResponse()
data class LoginFailureResponse(val error: String) : LoginResponse()
