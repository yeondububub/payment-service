package com.example.walletservice.wallet.application.service

import com.example.walletservice.wallet.adapter.out.persistence.entity.JpaWalletEntity
import com.example.walletservice.wallet.adapter.out.persistence.repository.SpringDataJpaWalletRepository
import com.example.walletservice.wallet.adapter.out.persistence.repository.SpringDataJpaWalletTransactionRepository
import com.example.walletservice.wallet.application.port.out.DuplicateMessageFilterPort
import com.example.walletservice.wallet.application.port.out.LoadPaymentOrderPort
import com.example.walletservice.wallet.application.port.out.LoadWalletPort
import com.example.walletservice.wallet.application.port.out.SaveWalletPort
import com.example.walletservice.wallet.domain.PaymentEventMessage
import com.example.walletservice.wallet.domain.PaymentEventMessageType
import com.example.walletservice.wallet.domain.PaymentOrder
import com.example.walletservice.wallet.domain.WalletEventMessageType
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.util.*

@SpringBootTest
class SettlementServiceTest (
    @Autowired private val duplicateMessageFilterPort: DuplicateMessageFilterPort,
    @Autowired private val loadWalletPort: LoadWalletPort,
    @Autowired private val saveWalletPort: SaveWalletPort,
    @Autowired private val springDataJpaWalletRepository: SpringDataJpaWalletRepository,
    @Autowired private val springDataJpaWalletTransactionRepository: SpringDataJpaWalletTransactionRepository
) {

    @BeforeEach
    fun clean() {
        springDataJpaWalletTransactionRepository.deleteAll()
        springDataJpaWalletRepository.deleteAll()
    }

    @Test
    @DisplayName("결제 이벤트 메시지를 수신하여 판매자별 정산 금액을 지갑에 정상 적립한다")
    fun test1() {
        val jpaWalletEntities = listOf(
            JpaWalletEntity(
                userId = 1L,
                balance = BigDecimal.ZERO,
                version = 0,
            ),
            JpaWalletEntity(
                userId = 2L,
                balance = BigDecimal.ZERO,
                version = 0,
            )
        )

        springDataJpaWalletRepository.saveAll(jpaWalletEntities)

        val paymentEventMessage = PaymentEventMessage(
            type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
            payload = mapOf(
                "orderId" to UUID.randomUUID().toString()
            )
        )

        val mockLoadPaymentOrderPort = mockk<LoadPaymentOrderPort>()

        every { mockLoadPaymentOrderPort.getPaymentOrders(paymentEventMessage.orderId()) } returns listOf(
            PaymentOrder(
                id = 1,
                sellerId = 1,
                amount = 3000L,
                orderId = paymentEventMessage.orderId()
            ),
            PaymentOrder(
                id = 2,
                sellerId = 2,
                amount = 4000L,
                orderId = paymentEventMessage.orderId()
            )
        )

        val settlementService = SettlementService(
            duplicateMessageFilterPort = duplicateMessageFilterPort,
            loadWalletPort = loadWalletPort,
            loadPaymentOrderPort = mockLoadPaymentOrderPort,
            saveWalletPort = saveWalletPort
        )

        val walletEventMessage = settlementService.processSettlement(paymentEventMessage)

        val updatedWallets = loadWalletPort.getWallets(jpaWalletEntities.map { it.userId }.toSet())
            .sortedBy { it.userId }

        assertThat(walletEventMessage.payload["orderId"]).isEqualTo(paymentEventMessage.orderId())
        assertThat(walletEventMessage.type).isEqualTo(WalletEventMessageType.SUCCESS)
        assertThat(updatedWallets[0].balance.toInt()).isEqualTo(3000)
        assertThat(updatedWallets[1].balance.toInt()).isEqualTo(4000)
    }

    @Test
    @DisplayName("동일한 결제 이벤트 메시지가 중복 인입되어도 지갑 정산은 1회만 처리된다 (멱등성 보장)")
    fun test2() {
        val jpaWalletEntities = listOf(
            JpaWalletEntity(
                userId = 1L,
                balance = BigDecimal.ZERO,
                version = 0,
            ),
            JpaWalletEntity(
                userId = 2L,
                balance = BigDecimal.ZERO,
                version = 0,
            )
        )

        springDataJpaWalletRepository.saveAll(jpaWalletEntities)

        val paymentEventMessage = PaymentEventMessage(
            type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
            payload = mapOf(
                "orderId" to UUID.randomUUID().toString()
            )
        )

        val mockLoadPaymentOrderPort = mockk<LoadPaymentOrderPort>()

        every { mockLoadPaymentOrderPort.getPaymentOrders(paymentEventMessage.orderId()) } returns listOf(
            PaymentOrder(
                id = 1,
                sellerId = 1,
                amount = 3000L,
                orderId = paymentEventMessage.orderId()
            ),
            PaymentOrder(
                id = 2,
                sellerId = 2,
                amount = 4000L,
                orderId = paymentEventMessage.orderId()
            )
        )

        val settlementService = SettlementService(
            duplicateMessageFilterPort = duplicateMessageFilterPort,
            loadWalletPort = loadWalletPort,
            loadPaymentOrderPort = mockLoadPaymentOrderPort,
            saveWalletPort = saveWalletPort
        )

        settlementService.processSettlement(paymentEventMessage)
        settlementService.processSettlement(paymentEventMessage)

        val updatedWallets = loadWalletPort.getWallets(jpaWalletEntities.map { it.userId }.toSet())
            .sortedBy { it.userId }

        assertThat(updatedWallets[0].balance.toInt()).isEqualTo(3000)
        assertThat(updatedWallets[1].balance.toInt()).isEqualTo(4000)
    }
}