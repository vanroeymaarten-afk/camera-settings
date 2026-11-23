package com.camerasettings.app.data

import kotlin.math.ceil

/** Trip checklist, grouped. Item ids are stable so ticks survive app updates. */
object Checklist {
    data class Item(val id: String, val text: String, val detail: String = "")
    data class Group(val title: String, val items: List<Item>)

    val groups = listOf(
        Group(
            "Before you fly",
            listOf(
                Item("bat", "Charge all batteries, bring at least one spare", "Cold weather drains batteries quickly."),
                Item("format", "Format memory cards in the camera", "Menu → Format card. Do it in the camera, not on a computer."),
                Item("raw", "Set image quality to RAW + JPEG (L)", "JPEG to share, RAW to edit after the trip."),
                Item("clock", "Set the camera clock to local time at your destination", "So photos sort correctly and match your phone's."),
                Item("clean", "Clean lenses and check the sensor for dust", "Take a photo of a clear sky at f/16 and zoom in on the back screen."),
                Item("fw", "Update camera firmware (optional)", "Canon's website, or Camera Connect for some models."),
                Item("connect", "Install Canon Camera Connect on your phone and pair it", "To send JPEGs to your phone over Wi-Fi."),
            ),
        ),
        Group(
            "In the camera bag",
            listOf(
                Item("cloth", "Microfibre cloths and a blower", "For dust, sand and raindrops."),
                Item("rain", "Rain cover or a plastic bag + rubber band", "Cheap and it works."),
                Item("tripod", "Small travel tripod", "For stars, waterfalls and blue hour."),
                Item("filters", "Polarising filter (and an ND filter if you have one)", "Deep blue skies and lagoons; silky waterfalls."),
                Item("lamp", "Headlamp with a red-light mode", "For star photography and early-morning walks."),
                Item("cards", "Spare memory cards", "See the storage calculator for how many."),
                Item("adapter", "Plug adapter for your destination", "Most camera chargers work on 100–240 V."),
                Item("ziplock", "Zip bags", "Seal the camera in one before entering a warm car after the cold (stops condensation)."),
                Item("backup", "Card reader or a way to back up", "Laptop, phone with USB-C reader, or portable SSD."),
            ),
        ),
        Group(
            "Every evening",
            listOf(
                Item("eve_backup", "Back up today's photos (keep 2 copies)", "Never format a card until it's backed up twice."),
                Item("eve_charge", "Charge all batteries"),
                Item("eve_wipe", "Wipe the lens and check for dust or spray"),
                Item("eve_plan", "Check tomorrow's light in the Plan tab", "Golden hour, moon and the star calendar."),
            ),
        ),
        Group(
            "Tough conditions",
            listOf(
                Item("cold", "Cold: spare battery in an inside pocket", "And a zip bag to stop condensation when you go back inside."),
                Item("dust", "Dust and sand: change lenses pointing down, out of the wind", "Dust on the sensor shows up as dark spots."),
                Item("heat", "Heat: don't leave the camera in a hot car or in the sun", ""),
                Item("alt", "High altitude: take it slow, drink water", "Your body needs time; so do you to think about settings."),
            ),
        ),
    )

    val allIds: List<String> = groups.flatMap { g -> g.items.map { it.id } }
}

object Storage {
    data class Estimate(
        val gbTotal: Double,
        val gbRawOnly: Double,
        val perShotMb: Double,
        val cards: String,
    )

    fun estimate(body: CameraBody, shotsPerDay: Int, days: Int, rawPlusJpeg: Boolean, craw: Boolean): Estimate {
        val raw = body.rawMb * (if (craw && body.craw) 0.6 else 1.0)
        val per = raw + if (rawPlusJpeg) body.jpegMb else 0.0
        val total = per * shotsPerDay * days / 1024.0
        val cards = when {
            total <= 48 -> "1 × 64 GB card is enough, bring a spare"
            total <= 100 -> "2 × 64 GB or 1 × 128 GB + a spare"
            total <= 220 -> "2 × 128 GB cards"
            else -> "${ceil(total / 256.0).toInt()} × 256 GB cards"
        }
        return Estimate(total, raw * shotsPerDay * days / 1024.0, per, cards)
    }
}

/** Editing software overview. */
object Software {
    data class App(
        val name: String,
        val platform: String,
        val price: String,
        val bestFor: String,
        val readsRaw: Boolean,
        val usesPresets: Boolean,
        val note: String,
    )

    val apps = listOf(
        App(
            "Adobe Lightroom Classic", "Windows · Mac", "Subscription (Photography plan)",
            "The standard for organising and editing a whole trip.", readsRaw = true, usesPresets = true,
            "Imports the .xmp presets from this app directly (Develop → Presets → + → Import).",
        ),
        App(
            "Adobe Lightroom", "Windows · Mac · Android · iOS", "Subscription; basic mobile editing is free",
            "Simpler, cloud-synced; edit on the phone and the computer.", readsRaw = true, usesPresets = true,
            "Desktop: Edit → Presets → ⋯ → Import presets. They then sync to the phone.",
        ),
        App(
            "Canon Digital Photo Professional 4", "Windows · Mac", "Free from Canon",
            "Free RAW editor that knows your camera's Picture Styles and lenses.", readsRaw = true, usesPresets = false,
            "Use the recipe values by hand; DPP has its own (simpler) controls.",
        ),
        App(
            "darktable", "Windows · Mac · Linux", "Free, open source",
            "Powerful free alternative to Lightroom.", readsRaw = true, usesPresets = false,
            "Follow the recipe step by step; module names differ slightly.",
        ),
        App(
            "Snapseed", "Android · iOS", "Free",
            "Quick edits to the JPEGs on your phone during the trip.", readsRaw = false, usesPresets = false,
            "Use Tune Image + White Balance; great for sharing on the go.",
        ),
        App(
            "Canon Camera Connect", "Android · iOS", "Free",
            "Copy JPEGs from the camera to your phone over Wi-Fi.", readsRaw = false, usesPresets = false,
            "Not an editor. Transfers and remote shutter (handy for star shots).",
        ),
    )
}
