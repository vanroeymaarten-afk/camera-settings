package com.camerasettings.app.logic

import com.camerasettings.app.data.Brand
import com.camerasettings.app.data.Checklist
import com.camerasettings.app.data.Gear
import com.camerasettings.app.data.Learn
import com.camerasettings.app.data.Photos
import com.camerasettings.app.data.Place
import com.camerasettings.app.data.Storage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ContentTest {

    @Test
    fun photosAreCompleteAndLicensed() {
        assertTrue(Photos.all.size >= 35)
        for (p in Photos.all) {
            assertNotNull("scene for ${p.id}", Scenes.byId(p.sceneId))
            assertTrue(p.id, p.aperture in 0.9..32.0 && p.shutter in 1e-5..400.0 && p.iso in 25..102400 && p.focal > 0)
            assertTrue(p.id, p.license.startsWith("CC") || p.license.startsWith("Public"))
            assertTrue(p.id, p.author.isNotBlank() && p.source.startsWith("https://commons.wikimedia.org/"))
            assertFalse("${p.id} looks non-commercial", p.license.contains("NC"))
        }
        // Almost every situation has at least one example photo.
        assertTrue(Scenes.all.count { s -> Photos.forScene(s.id).isNotEmpty() } >= 19)
    }

    @Test
    fun recreatePlanFitsTheUsersLens() {
        val body = Gear.camera("r50")!!
        val bag = listOf(Gear.lenses.first { it.id == "rfs18-45" }, Gear.lenses.first { it.id == "rfs55-210" })
        for (p in Photos.all) {
            val plan = Recreate.plan(p, body, bag)!!
            assertTrue(p.id, plan.focal in plan.lens.minFocal..plan.lens.maxFocal)
            assertTrue(p.id, plan.aperture >= plan.lens.apertureAt(plan.focal) - 0.05)
            assertTrue(p.id, plan.iso in 100..25600)
        }
        // A 600 mm bird shot needs the telephoto.
        val bird = Photos.all.first { it.sceneId == "birds" && it.focal >= 200 }
        assertEquals("rfs55-210", Recreate.plan(bird, body, bag)!!.lens.id)
    }

    @Test
    fun lessonsAreWellFormed() {
        assertTrue(Learn.lessons.size >= 15)
        assertEquals(Learn.lessons.size, Learn.lessons.map { it.id }.toSet().size)
        for (l in Learn.lessons) {
            assertTrue(l.id, l.sections.isNotEmpty() && l.tryIt.isNotEmpty() && l.quiz.isNotEmpty())
            l.quiz.forEach { q -> assertTrue(q.question, q.answer in q.options.indices) }
            l.practiceScene?.let { assertNotNull(it, Scenes.byId(it)) }
        }
        // Placeholders are replaced with the brand's words.
        val txt = Learn.fill("Use {AV} and {Q}", Brand.NIKON)
        assertEquals("Use A and i button", txt)
        Learn.problems.forEach { assertTrue(it.fixes.isNotEmpty()) }
        assertTrue(Learn.glossary.size >= 30)
    }

    @Test
    fun placeEncodingRoundTrips() {
        val p = Place("Reykjavík", "Iceland", 64.1466, -21.9426, "Atlantic/Reykjavik", 118918, false)
        assertEquals(p, Place.decode(p.encode()))
    }

    @Test
    fun citiesAssetIsValid() {
        val f = File("src/main/assets/cities.tsv")
        assertTrue(f.exists())
        val lines = f.readLines()
        assertTrue(lines.size > 30000)
        val brussels = lines.first { it.startsWith("Brussels\t") }.split('\t')
        assertEquals("Europe/Brussels", brussels[5])
        val lm = File("src/main/assets/landmarks.tsv").readLines()
        assertTrue(lm.size >= 60)
        lm.forEach { java.time.ZoneId.of(it.split('\t')[5]) }
    }

    @Test
    fun dofMatchesKnownValues() {
        // 50 mm, f/8, 5 m, full frame: about 3.4 m to 9.6 m; hyperfocal ~10.5 m.
        val r = Dof.compute(50.0, 8.0, 5.0, 1.0)
        assertTrue("${r.near}", r.near in 3.2..3.6)
        assertTrue("${r.far}", r.far in 8.8..10.5)
        assertTrue("${r.hyperfocal}", r.hyperfocal in 10.0..11.0)
    }

    @Test
    fun xmpPresetsAreValidXml() {
        val f = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        for (p in Presets.all) {
            val doc = f.newDocumentBuilder().parse(Presets.toXmp(p).byteInputStream())
            val desc = doc.getElementsByTagNameNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "Description").item(0) as org.w3c.dom.Element
            assertEquals("True", desc.getAttributeNS("http://ns.adobe.com/camera-raw-settings/1.0/", "HasSettings"))
        }
    }

    @Test
    fun storageAndChecklist() {
        val e = Storage.estimate(Gear.camera("a7iv")!!, 150, 14, rawPlusJpeg = true, craw = false)
        assertTrue("${e.gbTotal}", e.gbTotal in 60.0..140.0)
        assertEquals("ARW", Gear.camera("a7iv")!!.rawFormat)
        assertEquals(Checklist.allIds.size, Checklist.allIds.toSet().size)
    }

    @Test
    fun defaultCameraIsTheOwnersEos750D() {
        val body = Gear.camera(com.camerasettings.app.data.AppState.DEFAULT_CAMERA)!!
        assertEquals("EOS 750D / Rebel T6i", body.name)
        assertEquals("CR2", body.rawFormat)
        val fits = Gear.lensesFor(body).map { it.id }
        com.camerasettings.app.data.AppState.DEFAULT_LENSES.forEach { assertTrue(it, it in fits) }
    }
}
