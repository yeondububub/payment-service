package com.example.paymentservice.payment.application.port.out

import com.example.paymentservice.payment.domain.PaymentEvent
import reactor.core.publisher.Mono

interface LoadPaymentPort {
    fun getPayment(orderId: String): Mono<PaymentEvent>
}