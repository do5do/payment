package com.zerobase.payment.service

import com.zerobase.payment.OrderStatus
import com.zerobase.payment.OrderStatus.*
import com.zerobase.payment.TransactionStatus.*
import com.zerobase.payment.TransactionType.PAYMENT
import com.zerobase.payment.TransactionType.REFUND
import com.zerobase.payment.domain.Order
import com.zerobase.payment.domain.OrderTransaction
import com.zerobase.payment.exception.ErrorCode
import com.zerobase.payment.exception.ErrorCode.*
import com.zerobase.payment.exception.PaymentException
import com.zerobase.payment.repository.OrderRepository
import com.zerobase.payment.repository.OrderTransactionRepository
import com.zerobase.payment.repository.PaymentRepositories
import com.zerobase.payment.util.generateRefundTransactionId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 환불의 요청 저장, 성공, 실패 저장
 */
@Service
class RefundStatusService(
    private val paymentRepositories: PaymentRepositories,
    private val orderRepository: OrderRepository,
    private val orderTransactionRepository: OrderTransactionRepository
) {
    @Transactional
    fun saveRefundRequest(
        originalTransactionId: String,
        merchantRefundId: String,
        refundAmount: Long,
        refundReason: String,
    ): Long {
        // 결제(orderTransaction) 확인
        // 환불이 가능한지 확인
        // 환불 트랜잭션(orderTransaction) 저장
        val originalTransaction =
            orderTransactionRepository.findByTransactionId(originalTransactionId)
                ?: throw PaymentException(ORDER_NOT_FOUND)

        val order = originalTransaction.order

        validationRefund(order, refundAmount)

        return orderTransactionRepository.save(
            OrderTransaction(
                transactionId = generateRefundTransactionId(),
                order = order,
                transactionType = REFUND,
                transactionStatus = RESERVE,
                transactionAmount = refundAmount,
                merchantTransactionId = merchantRefundId,
                description = refundReason
            )
        ).id ?: throw PaymentException(INTERNAL_SERVER_ERROR)
    }

    private fun validationRefund(order: Order, refundAmount: Long) {
        if (order.orderStatus !in listOf(PAID, PARTIAL_REFUNDED)) {
            throw PaymentException(CANNOT_REFUND)
        }

        if (order.refundedAmount + refundAmount > order.paidAmount) {
            throw PaymentException(EXCEED_REFUNDABLE_AMOUNT)
        }
    }

    @Transactional
    fun saveAsSuccess(
        refundTxId: Long,
        refundMethodTransactionId: String
    ): Pair<String, LocalDateTime> {
        val orderTransaction = orderTransactionRepository.findById(refundTxId)
            .orElseThrow { throw PaymentException(INTERNAL_SERVER_ERROR) }
            .apply {
                transactionStatus = SUCCESS
                this.payMethodTransactionId = refundMethodTransactionId
                transactedAt = LocalDateTime.now()
            }

        val order = orderTransaction.order
        val totalRefundedAmount = getTotalRefundedAmount(order)

        order.apply {
            orderStatus = getNewOrderStatus(this, totalRefundedAmount)
            refundedAmount = totalRefundedAmount
        }

        return Pair(
            orderTransaction.transactionId,
            orderTransaction.transactedAt ?: throw PaymentException(INTERNAL_SERVER_ERROR)
        )
    }

    private fun getNewOrderStatus(
        order: Order,
        totalRefundedAmount: Long
    ): OrderStatus =
        if (order.orderAmount == totalRefundedAmount) REFUNDED
        else PARTIAL_REFUNDED

    private fun getTotalRefundedAmount(order: Order): Long =
        orderTransactionRepository.findByOrderAndTransactionType(
            order = order,
            transactionType = REFUND
        )
            .filter { it.transactionStatus == SUCCESS }
            .sumOf { it.transactionAmount } // filter한 값의 sum

    fun saveAsFailure(
        orderId: Long,
        errorCode: ErrorCode
    ) {
        val order: Order = getOrderByOrderId(orderId).apply {
            orderStatus = OrderStatus.FAILED
        }

        getFirstOrderTransactionByOrder(order).apply {
            transactionStatus = FAILURE
            failureCode = errorCode.name
            description = errorCode.errorMessage
        }
    }

    private fun getFirstOrderTransactionByOrder(order: Order) =
        orderTransactionRepository.findByOrderAndTransactionType(
            order = order,
            transactionType = PAYMENT
        ).first()

    private fun getOrderByOrderId(orderId: Long): Order =
        orderRepository.findById(orderId)
            .orElseThrow { throw PaymentException(ErrorCode.ORDER_NOT_FOUND) }
}