package com.example.paymentservice.payment.application.port.out

import com.example.paymentservice.payment.domain.PaymentEvent
import reactor.core.publisher.Mono

interface CompletePaymentPort {
    fun complete(paymentEvent: PaymentEvent): Mono<Void>
}