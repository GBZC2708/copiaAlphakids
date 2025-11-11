package com.example.alphakids.domain

import com.example.alphakids.domain.models.ServiceStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ServiceStatusTest {

    @Test
    fun `service status stores message`() {
        val status = ServiceStatus(isHealthy = false, lastCheckedAtMillis = 10L, message = "fallo")
        assertEquals("fallo", status.message)
    }
}
