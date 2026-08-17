package com.example.paymentservice.payment.application.port.out

import com.example.paymentservice.payment.domain.PaymentEventMessage

interface DispatchEventMessagePort {
    fun dispatch(paymentEventMessage: PaymentEventMessage)
}