package com.camerasettings.app.logic

import com.camerasettings.app.data.Gear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvisorTest {
    private val r50 = Gear.camera("r50")!!
    private val r6 = Gear.camera("r6ii")!!
    private val kit = Gear.lenses.first { it.id == "rfs18-45" }
    private val tele = Gear.lenses.first { it.id == "rfs55-210" }

    @Test
    fun everySceneProducesAdviceForEveryCamera() {
        for (body in Gear.cameras) for (scene in Scenes.all) for (support in Support.entries) {
            val lens = Gear.lenses.first()
            val rec = Advisor.recommend(scene, body, lens, lens.minFocal, support)
            assertTrue(rec.settings.isNotEmpty())
            assertTrue(rec.mode.isNotBlank())
        }
    }

    @Test
    fun picksLongLensForWildlifeAndWideForAstro() {
        val bag = listOf(kit, tele)
        assertEquals(tele, Advisor.pickLens(bag, LensPref.LONG, 1.6))
        assertEquals(kit, Advisor.pickLens(bag, LensPref.ASTRO, 1.6))
    }

    @Test
    fun npfIsSensible() {
        val t = Advisor.npfSeconds(r50, kit, 18)
        assertTrue("npf $t", t in 10.0..20.0)
        val ff = Advisor.npfSeconds(r6, Gear.lenses.first { it.id == "rf15-35l" }, 15)
        assertTrue("npf $ff", ff in 10.0..20.0)
    }

    @Test
    fun meterMathSunnySixteen() {
        // Sunny 16: EV15 at ISO 100, f/16 -> about 1/125
        val t = Exposure.shutterFor(15.0, 16.0, 100)
        assertEquals(1 / 125.0, Exposure.nearestShutter(t), 1e-9)
    }

    @Test
    fun zoomApertureInterpolates() {
        assertEquals(4.5, kit.apertureAt(18), 1e-9)
        assertEquals(6.3, kit.apertureAt(45), 1e-9)
    }
}
