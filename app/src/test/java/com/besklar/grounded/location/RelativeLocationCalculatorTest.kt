package com.besklar.grounded.location

import com.besklar.grounded.model.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelativeLocationCalculatorTest {
    @Test
    fun `calculates known Denver to Boulder distance and direction`() {
        val result =
            RelativeLocationCalculator.calculate(
                Coordinates(39.7392, -104.9903),
                Coordinates(40.0150, -105.2705),
            )

        assertTrue(result.distanceKilometers in 38.0..42.0)
        assertEquals(CompassDirection.NORTHWEST, result.direction)
    }

    @Test
    fun `same coordinate has zero distance`() {
        val point = Coordinates(10.0, 20.0)
        assertEquals(0.0, RelativeLocationCalculator.calculate(point, point).distanceKilometers, 0.001)
    }
}
