package com.zerobase.payment.repository

import com.zerobase.payment.TransactionType
import com.zerobase.payment.domain.Order
import com.zerobase.payment.domain.OrderTransaction
import com.zerobase.payment.domain.PaymentUser
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRepositories : JpaRepository<PaymentUser, Long> {
    fun findByPayUserId(payUserId: String): PaymentUser? // kotlin에서는 optional보다 이게 유용함
}

interface OrderRepository : JpaRepository<Order, Long> {

}

interface OrderTransactionRepository : JpaRepository<OrderTransaction, Long> {
    fun findByOrderAndTransactionType(
        order: Order,
        transactionType: TransactionType
    ): List<OrderTransaction>

    fun findByTransactionId(transactionId: String): OrderTransaction?
}