package com.example.paymentservice.payment.adapter.out.stream

import com.example.paymentservice.payment.domain.PaymentEventMessage
import com.example.paymentservice.payment.domain.PaymentEventMessageType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.*
import kotlin.test.Test

@SpringBootTest
@Tag("ExternalIntegration")
class PaymentEventMessageSenderTest(
    @Autowired private val paymentEventMessageSender: PaymentEventMessageSender,
) {

    @Test
    @DisplayName("결제 이벤트 메시지를 수신하여 카프카의 각 파티션으로 정상 발송한다")
    fun test1() {
        val paymentEventMessages = listOf(
            PaymentEventMessage(
                type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
                payload = mapOf(
                    "orderId" to UUID.randomUUID().toString()
                ),
                metadata = mapOf(
                    "partitionKey" to 0
                )
            ),
            PaymentEventMessage(
                type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
                payload = mapOf(
                    "orderId" to UUID.randomUUID().toString()
                ),
                metadata = mapOf(
                    "partitionKey" to 1
                )
            ),
            PaymentEventMessage(
                type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
                payload = mapOf(
                    "orderId" to UUID.randomUUID().toString()
                ),
                metadata = mapOf(
                    "partitionKey" to 2
                )
            ),
            PaymentEventMessage(
                type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
                payload = mapOf(
                    "orderId" to UUID.randomUUID().toString()
                ),
                metadata = mapOf(
                    "partitionKey" to 3
                )
            ),
            PaymentEventMessage(
                type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
                payload = mapOf(
                    "orderId" to UUID.randomUUID().toString()
                ),
                metadata = mapOf(
                    "partitionKey" to 4
                )
            ), PaymentEventMessage(
                type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
                payload = mapOf(
                    "orderId" to UUID.randomUUID().toString()
                ),
                metadata = mapOf(
                    "partitionKey" to 5
                )
            )
        )

        paymentEventMessages.forEach {
            paymentEventMessageSender.dispatch(it)
        }

        Thread.sleep(10000)
    }
}