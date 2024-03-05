package com.zerobase.payment.service

import com.zerobase.payment.OrderStatus
import com.zerobase.payment.TransactionStatus.*
import com.zerobase.payment.TransactionType.PAYMENT
import com.zerobase.payment.domain.Order
import com.zerobase.payment.domain.OrderTransaction
import com.zerobase.payment.exception.ErrorCode
import com.zerobase.payment.exception.ErrorCode.INTERNAL_SERVER_ERROR
import com.zerobase.payment.exception.ErrorCode.INVALID_REQUEST
import com.zerobase.payment.exception.PaymentException
import com.zerobase.payment.repository.OrderRepository
import com.zerobase.payment.repository.OrderTransactionRepository
import com.zerobase.payment.repository.PaymentRepositories
import com.zerobase.payment.util.generateOrderId
import com.zerobase.payment.util.generateTransactionId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 결제의 요청 저장, 성공, 실패 저장
 */
@Service
class PaymentStatusService(
    private val paymentRepositories: PaymentRepositories,
    private val orderRepository: OrderRepository,
    private val orderTransactionRepository: OrderTransactionRepository
) {
    @Transactional
    fun savePayRequest(
        payUserId: String,
        amount: Long,
        orderTitle: String,
        merchantTransactionId: String
    ): Long {
        // order, orderTransaction 저장
        val paymentUser = paymentRepositories.findByPayUserId(
            payUserId
        ) ?: throw PaymentException(INVALID_REQUEST, "사용자 없음 : $payUserId")

        val order = orderRepository.save(
            Order(
                orderId = generateOrderId(),
                paymentUser = paymentUser,
                orderStatus = OrderStatus.CREATED,
                orderTitle = orderTitle,
                orderAmount = amount
            )
        )

        orderTransactionRepository.save(
            OrderTransaction(
                transactionId = generateTransactionId(),
                order = order,
                transactionType = PAYMENT,
                transactionStatus = RESERVE,
                transactionAmount = amount,
                merchantTransactionId = merchantTransactionId,
                description = orderTitle
            )
        )

        return order.id ?: throw PaymentException(INTERNAL_SERVER_ERROR)
    }

    @Transactional
    fun saveAsSuccess(
        orderId: Long,
        payMethodTransactionId: String
    ): Pair<String, LocalDateTime> {
        val order: Order = getOrderByOrderId(orderId)
            .apply {
                orderStatus = OrderStatus.PAID
                paidAmount = orderAmount
            }

        val orderTransaction = getFirstOrderTransactionByOrder(order)
            .apply {
                transactionStatus = SUCCESS
                this.payMethodTransactionId = payMethodTransactionId
                transactedAt = LocalDateTime.now()
            }

        return Pair(
            orderTransaction.transactionId,
            orderTransaction.transactedAt ?: throw PaymentException(INTERNAL_SERVER_ERROR)
        )
    }

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