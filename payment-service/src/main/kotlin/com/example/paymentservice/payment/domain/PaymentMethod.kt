package com.example.paymentservice.payment.domain

enum class PaymentMethod(val method: String) {
    TRANSFER("계좌이체"),
    CARD("카드"),
    EASY_PAY("간편결제");

    companion object {
        fun get(method: String): PaymentMethod {
            return entries.find { it.method == method } ?: error("Payment Method (methpd: $method) 는 올바르이 않은 결제 방법입니다.")
        }
    }
}