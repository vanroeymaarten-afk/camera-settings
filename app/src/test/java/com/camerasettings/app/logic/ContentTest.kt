package com.camerasettings.app.logic

import com.camerasettings.app.data.Checklist
import com.camerasettings.app.data.Gear
import com.camerasettings.app.data.Places
import com.camerasettings.app.data.Shots
import com.camerasettings.app.data.Storage
import com.camerasettings.app.ui.Art
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import javax.xml.parsers.DocumentBuilderFactory

class ContentTest {

    @Test
    fun everyShotIsComplete() {
        assertEquals(13, Shots.all.size)
        for (s in Shots.all) {
            assertNotNull("place for ${s.id}", Places.byId(s.placeId))
            assertNotNull("scene for ${s.id}", Scenes.byId(s.sceneId))
            assertNotNull("art for ${s.id}", Art[s.id])
            Presets.byId(s.presetId)
            assertTrue(s.howTo.isNotEmpty())
        }
        assertEquals(Shots.all.map { it.id }.toSet(), Art.ids)
    }

    @Test
    fun scenesReferenceExistingArt() {
        Scenes.all.mapNotNull { it.art }.forEach { assertNotNull(it, Art[it]) }
    }

    @Test
    fun noPatagoniaLeftInThePlaces() {
        assertTrue(Places.all.none { it.lat < -45 })
        Places.all.forEach { ZoneId.of(it.zone) }
    }

    @Test
    fun bestTimesResolveAtTheirPlace() {
        for (s in Shots.all) {
            val p = s.place
            val plan = Astro.planDay(LocalDate.of(2026, 12, 15), ZoneId.of(p.zone), p.lat, p.lon)
            val w = s.bestTime.window(plan)
            if (w != null) assertTrue("${s.id}: $w", w.first.isBefore(w.second))
        }
    }

    @Test
    fun xmpPresetsAreValidXml() {
        val f = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        for (p in Presets.all) {
            val xml = Presets.toXmp(p)
            val doc = f.newDocumentBuilder().parse(xml.byteInputStream())
            val desc = doc.getElementsByTagNameNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "Description").item(0) as org.w3c.dom.Element
            val crs = "http://ns.adobe.com/camera-raw-settings/1.0/"
            assertEquals("True", desc.getAttributeNS(crs, "HasSettings"))
            assertEquals(32, desc.getAttributeNS(crs, "UUID").length)
            assertTrue(desc.getAttributeNS(crs, "Contrast2012").isNotEmpty())
            assertTrue(p.steps().isNotEmpty())
        }
        // Optional export to the repository: ./gradlew testDebugUnitTest -PexportPresets
        System.getProperty("exportPresetsDir")?.takeIf { it.isNotBlank() }?.let { dir ->
            File(dir).mkdirs()
            Presets.all.forEach { File(dir, Presets.fileName(it)).writeText(Presets.toXmp(it)) }
        }
    }

    @Test
    fun storageEstimateIsSensible() {
        val r50 = Gear.camera("r50")!!
        val e = Storage.estimate(r50, 150, 21, rawPlusJpeg = true, craw = false)
        assertTrue("${e.gbTotal}", e.gbTotal in 80.0..130.0)
        assertEquals("CR3", r50.rawFormat)
        assertEquals("CR2", Gear.camera("2000d")!!.rawFormat)
    }

    @Test
    fun checklistIdsAreUnique() {
        assertEquals(Checklist.allIds.size, Checklist.allIds.toSet().size)
    }

    @Test
    fun nextEventIsInTheFuture() {
        val z = ZoneId.of("America/Santiago")
        val now = LocalDate.of(2026, 12, 15).atTime(12, 0).atZone(z)
        val e = Astro.nextEvent(now, -22.91, -68.20)!!
        assertTrue(e.time.isAfter(now))
        assertEquals("Golden hour", e.label)
    }
}
