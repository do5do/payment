package com.zerobase.payment.domain

import com.zerobase.payment.OrderStatus
import jakarta.persistence.*

@Table(name = "orders")
@Entity
class Order(
    val orderId: String,

    @ManyToOne
    val paymentUser: PaymentUser,

    @Enumerated(EnumType.STRING)
    val orderStatus: OrderStatus,

    val orderTitle: String,
    val orderAmount: Long,
    var paidAmount: Long, // 결제된 금액
    var refundedAmount: Long // 환불된 금액
) : BaseEntity()