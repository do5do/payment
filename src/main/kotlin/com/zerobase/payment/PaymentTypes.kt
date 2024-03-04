package com.zerobase.payment

enum class OrderStatus {
    CREATED,
    FAILED,
    PAID,
    CANCELED,
    PARTIAL_REFUNDED, // 부분 환불
    REFUNDED // 환불
}

enum class TransactionType {
    PAYMENT,
    REFUND,
    CANCEL
}

enum class TransactionStatus {
    RESERVE, // 에약
    SUCCESS,
    FAILURE
}
