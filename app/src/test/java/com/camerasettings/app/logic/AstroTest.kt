package com.camerasettings.app.logic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class AstroTest {
    private val santiago = ZoneId.of("America/Santiago")

    private fun near(actual: ZonedDateTime?, h: Int, m: Int, toleranceMin: Long = 6) {
        assertNotNull(actual)
        val expected = actual!!.withHour(h).withMinute(m).withSecond(0)
        val diff = Duration.between(expected, actual).abs().toMinutes()
        assertTrue("expected ~$h:$m, got $actual", diff <= toleranceMin)
    }

    @Test
    fun santiagoSummerSunriseAndSunset() {
        // Published times for Santiago on 15 Dec: sunrise ~06:26, sunset ~20:52 (UTC-3).
        val p = Astro.planDay(LocalDate.of(2026, 12, 15), santiago, -33.45, -70.67)
        near(p.sun.sunrise, 6, 26)
        near(p.sun.sunset, 20, 52)
    }

    @Test
    fun patagoniaNeverFullyDarkInDecember() {
        val p = Astro.planDay(LocalDate.of(2026, 12, 15), ZoneId.of("America/Punta_Arenas"), -50.94, -73.41)
        assertFalse(p.night.fullyDark)
    }

    @Test
    fun atacamaInDecemberCoreHiddenButCarinaUp() {
        // New moon ~9 Dec 2026. In December the galactic centre is behind the sun,
        // but Carina / Southern Cross rise during the night.
        val p = Astro.planDay(LocalDate.of(2026, 12, 9), santiago, -22.91, -68.20)
        assertTrue(p.night.fullyDark)
        assertTrue(p.moon.illumination < 0.1)
        assertTrue("core windows: ${p.night.milkyWayCore}", p.night.milkyWayCore.isEmpty())
        assertTrue(p.night.targets[1].windows.isNotEmpty())
        assertTrue(p.night.targets[2].windows.isNotEmpty())
    }

    @Test
    fun atacamaInJuneCoreHighAtMidnight() {
        // New moon ~15 Jun 2026: the core is almost overhead around midnight.
        val p = Astro.planDay(LocalDate.of(2026, 6, 15), santiago, -22.91, -68.20)
        val core = p.night.targets[0]
        assertTrue(core.windows.isNotEmpty())
        assertTrue("alt ${core.bestAlt}", core.bestAlt > 70)
    }

    @Test
    fun fullMoonOnChristmasEve2026() {
        val p = Astro.planDay(LocalDate.of(2026, 12, 24), santiago, -33.45, -70.67)
        assertTrue("illum ${p.moon.illumination}", p.moon.illumination > 0.95)
    }
}
