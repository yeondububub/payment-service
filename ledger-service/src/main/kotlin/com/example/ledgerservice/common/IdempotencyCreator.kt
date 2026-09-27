package com.example.ledgerservice.common

import java.util.UUID

object IdempotencyCreator {
    fun createIdempotencyKey(data: Any): String {
        return UUID.nameUUIDFromBytes(data.toString().toByteArray()).toString()
    }
}