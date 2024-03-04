package com.zerobase.payment.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

private val log = KotlinLogging.logger {} // 이름을 지정하지 않으면 자동으로 클래스명이 할당된다.

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(PaymentException::class)
    fun handlePaymentException(
        e: PaymentException
    ): ErrorResponse {
        log.error(e) { "${e.errorCode} is occurred." } // error(e) : stacktrace 출력
        return ErrorResponse(e.errorCode)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        e: Exception
    ): ErrorResponse {
        log.error(e) { "Exception is occurred." }
        return ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR)
    }
}

class ErrorResponse(
    private val errorCode: ErrorCode,
    val errorMessage: String = errorCode.errorMessage
)