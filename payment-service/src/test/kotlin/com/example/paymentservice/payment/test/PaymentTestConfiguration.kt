package com.example.paymentservice.payment.test

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import

@TestConfiguration
@Import(R2DBCPaymentDatabaseHelper::class)
class PaymentTestConfiguration {
}