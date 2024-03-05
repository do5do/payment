package com.zerobase.payment.domain

import com.zerobase.payment.TransactionStatus
import com.zerobase.payment.TransactionType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
class OrderTransaction(
    val transactionId: String,

    @ManyToOne
    @JoinColumn(name = "order_id")
    val order: Order,

    @Enumerated(EnumType.STRING)
    val transactionType: TransactionType,

    @Enumerated(EnumType.STRING)
    var transactionStatus: TransactionStatus,

    val transactionAmount: Long,
    val merchantTransactionId: String, // 가맹점 거래 번호
    var payMethodTransactionId: String? = null, // 결제수단 거래 번호 -> 해당 객체가 생성되는 시점에는 알 수 없는 값이라서 null로 초기화
    var transactedAt: LocalDateTime? = null, // 거래가 완료된 시간 -> 처음에는 생성만 해둔다.(거래 요청이 들어왔다는 것을 저장하기 위함) 실제 거래가 진행된 다음에 시간을 업데이트 해줌
    var failureCode: String? = null,
    var description: String? = null
) : BaseEntity()