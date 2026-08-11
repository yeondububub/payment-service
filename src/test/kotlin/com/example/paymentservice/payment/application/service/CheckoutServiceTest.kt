package com.example.paymentservice.payment.application.service

import com.example.paymentservice.payment.application.port.`in`.CheckoutCommand
import com.example.paymentservice.payment.application.port.`in`.CheckoutUseCase
import com.example.paymentservice.payment.test.PaymentDatabaseHelper
import com.example.paymentservice.payment.test.PaymentTestConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import reactor.test.StepVerifier
import java.util.*

@Import(PaymentTestConfiguration::class)
@SpringBootTest
class CheckoutServiceTest @Autowired constructor(
    private val checkoutUseCase: CheckoutUseCase,
    private val paymentDatabaseHelper: PaymentDatabaseHelper
) {
    @BeforeEach
    fun setup() {
        paymentDatabaseHelper.clean().block()
    }

    @Test
    @DisplayName("체크아웃(결제 준비)에 성공한다.")
    fun test1() {
        val orderId = UUID.randomUUID().toString()
        val checkoutCommand = CheckoutCommand(
            cartId = 1,
            buyerId = 1,
            productIds = listOf(1, 2, 3),
            idempotencyKey = orderId
        )

        StepVerifier.create(checkoutUseCase.checkout(checkoutCommand))
            .expectNextMatches {
                it.amount.toInt() == 60000 && it.orderId == orderId
            }
            .verifyComplete()

        val paymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        assertThat(paymentEvent.orderId).isEqualTo(orderId)
        assertThat(paymentEvent.totalAmount()).isEqualTo(60000)
        assertThat(paymentEvent.paymentOrders.size).isEqualTo(checkoutCommand.productIds.size)
        assertFalse(paymentEvent.isPaymentDone())
        assertThat(paymentEvent.paymentOrders.all { !it.isLedgerUpdated() })
        assertThat(paymentEvent.paymentOrders.all { !it.isWalletUpdated() })
    }

    @Test
    @DisplayName("동일한 주문(중복 요청)이 들어오면 에러가 발생한다. (멱등성 테스트)")
    fun test2() {
        val orderId = UUID.randomUUID().toString()
        val checkoutCommand = CheckoutCommand(
            cartId = 1,
            buyerId = 1,
            productIds = listOf(1, 2, 3),
            idempotencyKey = orderId
        )

        checkoutUseCase.checkout(checkoutCommand).block()

        assertThrows<DataIntegrityViolationException> {
            checkoutUseCase.checkout(checkoutCommand).block()
        }
    }
}