package com.example.ledgerservice.ledger.adapter.out.persistence.entity

import com.example.ledgerservice.ledger.domain.LedgerEntryType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "ledger_entries")
class JpaLedgerEntryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val amount: BigDecimal,

    @Column(name = "account_id")
    val accountId: Long,

    @ManyToOne(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    val transaction: JpaLedgerTransactionEntity,

    @Enumerated(value = EnumType.STRING)
    val type: LedgerEntryType
)