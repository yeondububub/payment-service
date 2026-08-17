package com.example.paymentservice.payment.application.service

import com.example.paymentservice.payment.adapter.out.persistent.exception.PaymentAlreadyProcessedException
import com.example.paymentservice.payment.adapter.out.persistent.exception.PaymentValidationException
import com.example.paymentservice.payment.adapter.out.web.toss.exception.PSPConfirmationException
import com.example.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdatePort
import com.example.paymentservice.payment.domain.PaymentConfirmationResult
import com.example.paymentservice.payment.domain.PaymentFailure
import com.example.paymentservice.payment.domain.PaymentStatus
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.concurrent.TimeoutException

@Service
class PaymentErrorHandler (
    private val paymentStatusUpdatePort: PaymentStatusUpdatePort
) {

    fun handlePaymentConfirmationError(
        error: Throwable,
        command: PaymentConfirmCommand
    ): Mono<PaymentConfirmationResult> {
        val (status, failure) = when (error) {
            is PSPConfirmationException -> Pair(
                error.paymentStatus(),
                PaymentFailure(error.errorCode, error.errorMessage)
            )

            is PaymentValidationException -> Pair(
                PaymentStatus.FAILURE,
                PaymentFailure(error::class.simpleName ?: "", error.message ?: "")
            )

            is PaymentAlreadyProcessedException -> return Mono.just(
                PaymentConfirmationResult(
                    status = error.status,
                    failure = PaymentFailure(
                        message = error.message ?: "",
                        errorCode = error::class.simpleName ?: "")
                )
            )

            is TimeoutException -> Pair(
                PaymentStatus.UNKNOWN,
                PaymentFailure(
                    error::class.simpleName ?: "",
                    error.message ?: ""
                )
            )

            else -> Pair(
                PaymentStatus.UNKNOWN,
                PaymentFailure(
                    error::class.simpleName ?: "",
                    error.message ?: "")
            )
        }

        val paymentStatusUpdateCommand = PaymentStatusUpdateCommand(
            paymentKey = command.paymentKey,
            orderId = command.orderId,
            status = status,
            failure = failure
        )

        return paymentStatusUpdatePort.updatePaymentStatus(paymentStatusUpdateCommand)
            .map { PaymentConfirmationResult(status, failure) }
    }
}