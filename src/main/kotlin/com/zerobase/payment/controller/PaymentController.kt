package com.zerobase.payment.controller

import com.zerobase.payment.service.PayServiceRequest
import com.zerobase.payment.service.PayServiceResponse
import com.zerobase.payment.service.PaymentService
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RequestMapping("/api/v1")
@RestController
class PaymentController(
    private val paymentService: PaymentService
) {
    @PostMapping("/pay")
    fun pay(
        @Valid @RequestBody
        payRequest: PayRequest
    ): PayResponse = PayResponse.from(
        paymentService.pay(
            payRequest.toPayServiceRequest()
        )
    )
}

data class PayResponse(
    val payUserId: String,
    val amount: Long,
    val transactionId: String,
    val transactedAt: LocalDateTime
) {
    companion object {
        fun from(response: PayServiceResponse) =
            PayResponse(
                payUserId = response.payUserId,
                amount = response.amount,
                transactionId = response.transactionId,
                transactedAt = response.transactedAt
            )
    }
}

data class PayRequest(
    @field:NotBlank // 이 구간은 생성자 선언이기 때문에 클래스를 생성할 때의 값만 체크하고, 필드에 대해서는 validation이 되지 않기 때문에 field라고 명시해 준다.
    val payUserId: String,

    @field:Min(100)
    val amount: Long,

    @field:NotBlank
    val merchantTransactionId: String,

    @field:NotBlank
    val orderTitle: String
) {
    fun toPayServiceRequest() = PayServiceRequest(
        payUserId = payUserId,
        amount = amount,
        merchantTransactionId = merchantTransactionId,
        orderTitle = orderTitle
    )
}