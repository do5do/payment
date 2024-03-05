package com.zerobase.payment.service

import com.zerobase.payment.exception.ErrorCode
import com.zerobase.payment.exception.PaymentException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class RefundService(
    private val refundStatusService: RefundStatusService,
    private val accountService: AccountService,
) {
    fun refund(
        request: RefundServiceRequest
    ): RefundServiceResponse {
        // 요청을 저장 (요청됨으로 저장)
        val refundTxId = refundStatusService.saveRefundRequest(
            originalTransactionId = request.transactionId,
            merchantRefundId = request.refundId,
            refundAmount = request.refundAmount,
            refundReason = request.refundReason
        )

        return try {
            // 계죄에 금액 사용 취소 요청
            val refundAccountTransactionId =
                accountService.cancelUseAccount(refundTxId)

            // 성공 : 거래를 성공으로 저장
            val (transactionId, transactedAt) = refundStatusService.saveAsSuccess(
                refundTxId,
                refundAccountTransactionId
            )

            RefundServiceResponse(
                refundTransactionId = transactionId,
                refundAmount = request.refundAmount,
                refundedAt = transactedAt
            )
        } catch (e: Exception) {
            // 실패 : 거래를 실패로 저장 -> 실패 사유를 같이 저장해야 함
            val errorCode = getErrorCode(e)
            refundStatusService.saveAsFailure(refundTxId, errorCode)

            throw e
        }
    }

    private fun getErrorCode(e: Exception) =
        if (e is PaymentException) e.errorCode
        else ErrorCode.INTERNAL_SERVER_ERROR
}

// layer 별로 별도의 dto를 구성 하는 것을 추천
data class RefundServiceRequest(
    val transactionId: String,
    val refundId: String,
    val refundAmount: Long,
    val refundReason: String
)

data class RefundServiceResponse(
    val refundTransactionId: String,
    val refundAmount: Long,
    val refundedAt: LocalDateTime
)