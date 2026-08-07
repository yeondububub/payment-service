package com.example.paymentservice.payment.adapter.out.web.toss.executor

import com.example.paymentservice.payment.adapter.out.web.toss.exception.PSPConfirmationException
import com.example.paymentservice.payment.adapter.out.web.toss.exception.TossPaymentError
import com.example.paymentservice.payment.adapter.out.web.toss.response.TossFailureResponse
import com.example.paymentservice.payment.adapter.out.web.toss.response.TossPaymentConfirmationResponse
import com.example.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import com.example.paymentservice.payment.domain.PSPConfirmationStatus
import com.example.paymentservice.payment.domain.PaymentExecutionResult
import com.example.paymentservice.payment.domain.PaymentExtraDetails
import com.example.paymentservice.payment.domain.PaymentMethod
import com.example.paymentservice.payment.domain.PaymentType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeoutException

@Component
class TossPaymentExecutor(
    private val tossPaymentWebClient: WebClient,
    private val uri: String = "/v1/payments/confirm"
): PaymentExecutor {

    override fun execute(command: PaymentConfirmCommand): Mono<PaymentExecutionResult> {
        return tossPaymentWebClient
            .post()
            .uri(uri)
            .header("Idempotency-Key", command.orderId)
            .bodyValue("""
                {
                  "paymentKey": "${command.paymentKey}",
                  "orderId": "${command.orderId}", 
                  "amount": ${command.amount}
                }
            """.trimIndent())
            .retrieve()
            .onStatus({ statusCode: HttpStatusCode -> statusCode.is4xxClientError || statusCode.is5xxServerError }) {
                response -> response.bodyToMono(TossFailureResponse::class.java)
                .flatMap {
                    val error = TossPaymentError.get(it.code)
                    Mono.error<PSPConfirmationException>(
                        PSPConfirmationException(
                            errorCode = error.name,
                            errorMessage = error.description,
                            isSuccess = error.isSuccess(),
                            isFailure = error.isFailure(),
                            isUnknown = error.isUnknown(),
                            isRetryableError = error.isRetryableError()
                        )
                    )
                }
            }
            .bodyToMono(TossPaymentConfirmationResponse::class.java)
            .map {
                val objectMapper = jacksonObjectMapper()
                val jsonString = objectMapper.writeValueAsString(it)

                PaymentExecutionResult(
                    paymentKey = command.paymentKey,
                    orderId = command.orderId,
                    extraDetails = PaymentExtraDetails(
                        type = PaymentType.get(it.type),
                        method = PaymentMethod.get(it.method),
                        approvedAt = LocalDateTime.parse(it.approvedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        pspRawData = jsonString,
                        orderName = it.orderName,
                        pspConfirmationStatus = PSPConfirmationStatus.get(it.status),
                        totalAmount = it.totalAmount.toLong()
                    ),
                    isSuccess = true,
                    isFailure = false,
                    isUnknown = false,
                    isRetryable = false
                )
            }
            .retryWhen(
                Retry.backoff(2, Duration.ofSeconds(1)).jitter(0.1)
                    .filter { (it is PSPConfirmationException && it.isRetryableError) || it is TimeoutException }
                    .onRetryExhaustedThrow { _, retrySignal -> retrySignal.failure() }
            )
    }
}