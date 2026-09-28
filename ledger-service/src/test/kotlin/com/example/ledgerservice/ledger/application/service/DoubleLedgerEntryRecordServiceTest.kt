package com.example.ledgerservice.ledger.application.service

import com.example.ledgerservice.ledger.adapter.out.persistence.repository.SpringDataJpaLedgerEntryRepository
import com.example.ledgerservice.ledger.adapter.out.persistence.repository.SpringDataJpaLedgerTransactionRepository
import com.example.ledgerservice.ledger.application.port.out.DuplicateMessageFilterPort
import com.example.ledgerservice.ledger.application.port.out.LoadAccountPort
import com.example.ledgerservice.ledger.application.port.out.LoadPaymentOrderPort
import com.example.ledgerservice.ledger.application.port.out.SaveDoubleLedgerEntryPort
import com.example.ledgerservice.ledger.domain.*
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.util.*

@SpringBootTest
class DoubleLedgerEntryRecordServiceTest(
    @Autowired private val springDataJpaLedgerEntryRepository: SpringDataJpaLedgerEntryRepository,
    @Autowired private val springDataJpaLedgerTransactionRepository: SpringDataJpaLedgerTransactionRepository,
    @Autowired private val duplicateMessageFilterPort: DuplicateMessageFilterPort,
    @Autowired private val loadAccountPort: LoadAccountPort,
    @Autowired private val saveDoubleLedgerEntryPort: SaveDoubleLedgerEntryPort,
    @Autowired private val jdbcTemplate: JdbcTemplate
) {

    @BeforeEach
    fun clean() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0")
        jdbcTemplate.execute("TRUNCATE TABLE ledger_entries")
        jdbcTemplate.execute("TRUNCATE TABLE ledger_transactions")
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1")
    }

    @Test
    @DisplayName("결제 완료 이벤트 메시지를 받으면 차변과 대변의 합이 일치하는 복식부기 장부 항목을 성공적으로 기록한다.")
    fun test1() {
        val paymentEventMessage = PaymentEventMessage(
            type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
            payload = mapOf(
                "orderId" to UUID.randomUUID().toString()
            )
        )

        val mockLoadPaymentOrderRepository = mockk<LoadPaymentOrderPort>()

        every { mockLoadPaymentOrderRepository.getPaymentOrders(paymentEventMessage.orderId()) } returns listOf(
            PaymentOrder(
                id = 1L,
                amount = 200L,
                orderId = paymentEventMessage.orderId()
            ),
            PaymentOrder(
                id = 2L,
                amount = 300L,
                orderId = paymentEventMessage.orderId()
            )
        )

        val doubleLedgerRecordService = DoubleLedgerEntryRecordService(
            duplicateMessageFilterPort = duplicateMessageFilterPort,
            loadAccountPort = loadAccountPort,
            loadPaymentOrderPort = mockLoadPaymentOrderRepository,
            saveDoubleLedgerEntryPort = saveDoubleLedgerEntryPort
        )

        val ledgerEventMessage = doubleLedgerRecordService.recordDoubleLedgerEntry(paymentEventMessage)

        val jpaLedgerEntryList = springDataJpaLedgerEntryRepository.findAll()

        val sumOf = jpaLedgerEntryList.sumOf {
            when (it.type) {
                LedgerEntryType.CREDIT -> it.amount
                LedgerEntryType.DEBIT -> it.amount.negate()
            }
        }

        Assertions.assertThat(ledgerEventMessage.type).isEqualTo(LedgerEventMessageType.SUCCESS)
        Assertions.assertThat(jpaLedgerEntryList.size).isEqualTo(4)
        Assertions.assertThat(sumOf.toLong()).isEqualTo(0)
    }
}