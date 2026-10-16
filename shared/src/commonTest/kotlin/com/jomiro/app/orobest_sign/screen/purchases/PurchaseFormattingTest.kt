package com.jomiro.app.orobest_sign.screen.purchases

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PurchaseFormattingTest {
    @Test
    fun datePickerConversionPreservesIsoDayInUtc() {
        listOf("1970-01-01", "2024-02-29", "2026-10-15", "2000-01-01").forEach { date ->
            assertEquals(date, utcMillisToDate(checkNotNull(dateToUtcMillis(date))))
        }
        assertNull(dateToUtcMillis("2026-02-29"))
        assertEquals("15/10/2026", formatDateForPicker("2026-10-15"))
    }
}
