package com.cydoniancitizen.mindora.core.database.converter

import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SessionTypeConvertersTest {
    private val converters = SessionTypeConverters()

    @Test
    fun `session types use stable strings and round trip`() {
        MindfulnessSessionType.entries.forEach { type ->
            val stored = converters.sessionTypeToString(type)

            assertEquals(type.name, stored)
            assertEquals(type, converters.stringToSessionType(stored))
        }
    }

    @Test
    fun `session statuses use stable strings and round trip`() {
        MindfulnessSessionStatus.entries.forEach { status ->
            val stored = converters.sessionStatusToString(status)

            assertEquals(status.name, stored)
            assertEquals(status, converters.stringToSessionStatus(stored))
        }
    }

    @Test
    fun `unknown stored enum values fail clearly`() {
        assertThrows(IllegalArgumentException::class.java) {
            converters.stringToSessionType("UNKNOWN")
        }
        assertThrows(IllegalArgumentException::class.java) {
            converters.stringToSessionStatus("UNKNOWN")
        }
    }
}
