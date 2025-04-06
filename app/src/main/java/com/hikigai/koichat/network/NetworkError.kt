package com.hikigai.koichat.network

sealed class NetworkError : Exception() {
    object InvalidURL : NetworkError()
    data class InvalidResponse(val responseCode: Int) : NetworkError()
    object DecodingError : NetworkError()
    data class ServerError(override val message: String) : NetworkError()
    object NoData : NetworkError()
    object NetworkConnectionError : NetworkError()
    
    override val message: String
        get() = when (this) {
            is InvalidURL -> "Invalid URL configuration"
            is InvalidResponse -> "Server returned error code: $responseCode"
            is DecodingError -> "Failed to process server response"
            is ServerError -> message
            is NoData -> "Failed to generate suggested diagnoses. Please verify patient data."
            is NetworkConnectionError -> "Network connection error. Please check your internet connection."
        }
} 