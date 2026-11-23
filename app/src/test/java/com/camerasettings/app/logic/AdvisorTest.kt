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
            val lens = Gear.lensesFor(body).first()
            val rec = Advisor.recommend(scene, body, lens, lens.minFocal, support)
            assertTrue("${scene.id} ${body.id}", rec.settings.isNotEmpty())
            assertTrue(rec.modeLabel.isNotBlank())
            assertTrue(rec.modeHow.isNotBlank())
            assertTrue("${scene.id}: aperture", rec.aperture.isNotBlank())
            // Every non-trivial setting tells a beginner how to set it.
            rec.settings.filter { it.kind != Kind.FOCAL }.forEach { assertTrue("${scene.id} ${it.name} has no how-to", it.how.isNotBlank()) }
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

    @Test
    fun brandWordsAppearInTheInstructions() {
        val nikon = Gear.camera("z50")!!
        val lens = Gear.lensesFor(nikon).first { it.id == "nzdx16-50" }
        val rec = Advisor.recommend(Scenes.byId("landscape")!!, nikon, lens, 16, Support.HANDHELD)
        assertTrue(rec.modeLabel, rec.modeLabel.startsWith("A "))
        assertTrue(rec.settings.any { it.how.contains("i button") })
        val sony = Gear.camera("a6400")!!
        val rec2 = Advisor.recommend(Scenes.byId("wildlife")!!, sony, Gear.lensesFor(sony).first { it.id == "e70-350" }, 350, Support.HANDHELD)
        assertTrue(rec2.modeLabel, rec2.modeLabel.startsWith("S "))
        assertTrue(rec2.settings.any { it.value.contains("AF-C") })
    }

    @Test
    fun lensesFitTheirCameras() {
        val r50 = Gear.camera("r50")!!
        val fits = Gear.lensesFor(r50).map { it.mount }.toSet()
        assertTrue("RF-S" in fits && "EF-S" in fits)
        assertTrue("Z" !in fits && "X" !in fits)
        val g7x = Gear.camera("g7xiii")!!
        assertEquals(listOf("g7x"), Gear.lensesFor(g7x).map { it.id })
    }

    @Test
    fun scenesCoverTheWorld() {
        assertEquals(20, Scenes.all.size)
        assertEquals(Scenes.all.size, Scenes.all.map { it.id }.toSet().size)
        assertTrue(Scenes.search("dog").any { it.id == "pets" })
        assertTrue(Scenes.search("northern lights").any { it.id == "aurora" })
    }

    @Test
    fun wildlifeShutterIsAlwaysFast() {
        for (body in Gear.cameras) for (lens in Gear.lensesFor(body)) {
            val rec = Advisor.recommend(Scenes.byId("wildlife")!!, body, lens, lens.maxFocal, Support.HANDHELD)
            val t = rec.shutter.removePrefix("1/").removeSuffix(" s").toDouble()
            assertTrue("${body.id} ${lens.id}: ${rec.shutter}", rec.shutter.startsWith("1/") && t >= 1000)
        }
    }
}
