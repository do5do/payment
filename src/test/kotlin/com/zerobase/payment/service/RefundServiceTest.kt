package com.zerobase.payment.service

import com.zerobase.payment.exception.ErrorCode.EXCEED_REFUNDABLE_AMOUNT
import com.zerobase.payment.exception.PaymentException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

/**
 * kotest framework을 사용한 테스트 작성 방법
 * intellij kotest plugin 설치, test class를 생성할 때 BehviorSpec으로 선택
 */
class RefundServiceTest : BehaviorSpec({
    // relaxed = true : 이전에 사용한 @RelaxedMockK와 같은 의미
    val refundStatusService = mockk<RefundStatusService>(relaxed = true)
    val accountService = mockk<AccountService>(relaxed = true)

    // 테스트 할 서비스 객체를 직접 생성
    val refundService = RefundService(refundStatusService, accountService)

    Given("환불 요청이 정상적으로 저장됨") {
        val request = RefundServiceRequest(
            transactionId = "originalTransactionId",
            refundId = "merchantRefundId",
            refundAmount = 100,
            refundReason = "refundReason"
        )

        val refundTxId = 1L
        val accountTxId = "accountTxId"

        every {
            refundStatusService.saveRefundRequest(
                originalTransactionId = "originalTransactionId",
                merchantRefundId = "merchantRefundId",
                refundAmount = 100,
                refundReason = "refundReason"
            )
        } returns refundTxId

        When("계좌 시스템이 정상적으로 환불") {
            every {
                accountService.cancelUseAccount(refundTxId)
            } returns accountTxId

            every {
                refundStatusService.saveAsSuccess(
                    refundTxId,
                    accountTxId
                )
            } returns Pair("transactionId", LocalDateTime.now())

            val result = refundService.refund(request)

            // then block 단위로 실행할 수 있다. wow
            Then("트랜잭션ID, 금액이 응답으로 온다.") {
                result.refundTransactionId shouldBe "transactionId"
                result.refundAmount shouldBe 100
            }

            Then("saveAsSuccess 호출, saveFailure 미호출") {
                verify(exactly = 1) {
                    refundStatusService.saveAsSuccess(any(), any())
                }

                verify(exactly = 0) {
                    refundStatusService.saveAsFailure(any(), any())
                }
            }
        }

        When("계좌 시스템 환불이 실패") {
            every {
                accountService.cancelUseAccount(refundTxId)
            } throws PaymentException(EXCEED_REFUNDABLE_AMOUNT)

            // refundStatusService.saveAsFailure()는 Unit형이라서 생략 가능

            val result = shouldThrow<PaymentException> {
                refundService.refund(request)
            }

            Then("실패로 저장") {
                result.errorCode shouldBe EXCEED_REFUNDABLE_AMOUNT
                verify(exactly = 1) {
                    refundStatusService.saveAsFailure(any(), any())
                }
            }
        }
    }
})
