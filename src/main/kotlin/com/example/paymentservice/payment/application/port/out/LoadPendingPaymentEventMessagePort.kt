package com.example.paymentservice.payment.application.port.out

import com.example.paymentservice.payment.domain.PaymentEventMessage
import reactor.core.publisher.Flux

interface LoadPendingPaymentEventMessagePort {
    fun getPendingPaymentEventMessage(): Flux<PaymentEventMessage>
}