package com.example.paymentservice.payment.application.service

import com.example.paymentservice.payment.adapter.out.persistent.repository.PaymentOutboxRepository
import com.example.paymentservice.payment.application.port.out.DispatchEventMessagePort
import com.example.paymentservice.payment.application.port.out.LoadPendingPaymentEventMessagePort
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand
import com.example.paymentservice.payment.domain.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Hooks
import java.time.LocalDateTime
import java.util.*
import kotlin.test.Test

@SpringBootTest
@Tag("ExternalIntegration")
class PaymentEventMessageRelayServiceTest(
    @Autowired private val paymentOutboxRepository: PaymentOutboxRepository,
    @Autowired private val loadPendingPaymentEventMessagePort: LoadPendingPaymentEventMessagePort,
    @Autowired private val dispatchEventMessagePort: DispatchEventMessagePort,
) {

    @Test
    @DisplayName("전송 대기 중인 결제 이벤트 메시지를 조회하여 카프카로 재전송(Relay)한다")
    fun test1() {
        Hooks.onOperatorDebug()

        val paymentEventMessageRelayUseCase = PaymentEventMessageRelayService(loadPendingPaymentEventMessagePort, dispatchEventMessagePort)

        val command = PaymentStatusUpdateCommand(
            paymentExecutionResult = PaymentExecutionResult(
                paymentKey = UUID.randomUUID().toString(),
                orderId = UUID.randomUUID().toString(),
                extraDetails = PaymentExtraDetails(
                    type = PaymentType.NORMAL,
                    method = PaymentMethod.EASY_PAY,
                    approvedAt = LocalDateTime.now(),
                    orderName = "test_order_name",
                    pspConfirmationStatus = PSPConfirmationStatus.DONE,
                    totalAmount = 50000L,
                    pspRawData = "{}"
                ),
                isSuccess = true,
                isFailure = false,
                isUnknown = false,
                isRetryable = false
            )
        )

        paymentOutboxRepository.insertOutbox(command).block()
        paymentEventMessageRelayUseCase.relay()

        Thread.sleep(10000)
    }
}