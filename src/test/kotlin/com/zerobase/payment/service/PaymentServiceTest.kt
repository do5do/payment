package com.zerobase.payment.service

import com.zerobase.payment.exception.ErrorCode.LACK_BALANCE
import com.zerobase.payment.exception.PaymentException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class PaymentServiceTest {
    @RelaxedMockK
    lateinit var paymentStatusService: PaymentStatusService

    @MockK
    lateinit var accountService: AccountService

    @InjectMockKs
    lateinit var paymentService: PaymentService

    @Test
    fun `결제 성공`() {
        // given
        val request = PayServiceRequest(
            payUserId = "payUserId",
            amount = 100,
            merchantTransactionId = "merchantTransactionId",
            orderTitle = "orderTitle"
        )

        every {
            paymentStatusService.savePayRequest(
                payUserId = "payUserId",
                amount = 100,
                merchantTransactionId = "merchantTransactionId",
                orderTitle = "orderTitle"
            )
        } returns 1L

        every {
            accountService.useAccount(1L)
        } returns "payMethodTransactionId"

        every {
            paymentStatusService.saveAsSuccess(
                1L,
                "payMethodTransactionId"
            )
        } returns Pair("transactionId", LocalDateTime.MIN)

        // when
        val result = paymentService.pay(request)

        // then
        result.amount shouldBe 100

        verify(exactly = 1) {
            paymentStatusService.saveAsSuccess(any(), any())
        }

        verify(exactly = 0) {
            paymentStatusService.saveAsFailure(any(), any())
        }
    }

    @Test
    fun `결제 실패 - 잔액 부족`() {
        // given
        val request = PayServiceRequest(
            payUserId = "payUserId",
            amount = 1000000000,
            merchantTransactionId = "merchantTransactionId",
            orderTitle = "orderTitle"
        )

        every {
            paymentStatusService.savePayRequest(
                payUserId = "payUserId",
                amount = 100,
                merchantTransactionId = "merchantTransactionId",
                orderTitle = "orderTitle"
            )
        } returns 1L

        every {
            accountService.useAccount(1L)
        } throws PaymentException(LACK_BALANCE)

        // @RelaxedMockK로 모킹해주면 Unit 형 메서드는 지워줄 수 있다.
//        every {
//            paymentStatusService.saveAsFailure(
//                1L,
//                LACK_BALANCE
//            )
//        } returns Unit // Unit : void를 대체함

        // when
        val result = shouldThrow<PaymentException> {
            paymentService.pay(request)
        }

        // then
        result.errorCode shouldBe LACK_BALANCE

        verify(exactly = 0) {
            paymentStatusService.saveAsSuccess(any(), any())
        }

        verify(exactly = 1) {
            paymentStatusService.saveAsFailure(any(), any())
        }
    }
}