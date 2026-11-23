package com.camerasettings.app.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.camerasettings.app.logic.EditPreset
import com.camerasettings.app.logic.Presets
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Shares Lightroom presets as files (email them to yourself, save to Drive, etc.). */
object Share {
    private fun dir(ctx: Context) = File(ctx.cacheDir, "presets").apply { mkdirs() }

    private fun send(ctx: Context, file: File, mime: String, subject: String) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(
                Intent.EXTRA_TEXT,
                "Lightroom presets from Camera Coach. In Lightroom Classic: Develop → Presets → + → Import Presets. " +
                    "In Lightroom: Edit → Presets → ⋯ → Import Presets.",
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, subject).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun preset(ctx: Context, p: EditPreset) {
        val f = File(dir(ctx), Presets.fileName(p))
        f.writeText(Presets.toXmp(p))
        send(ctx, f, "application/octet-stream", "Lightroom preset: Camera Coach - ${p.name}")
    }

    fun allPresets(ctx: Context) {
        val f = File(dir(ctx), "Camera Coach presets.zip")
        ZipOutputStream(f.outputStream()).use { zip ->
            Presets.all.forEach { p ->
                zip.putNextEntry(ZipEntry(Presets.fileName(p)))
                zip.write(Presets.toXmp(p).toByteArray())
                zip.closeEntry()
            }
        }
        send(ctx, f, "application/zip", "Camera Coach: all Lightroom presets")
    }
}
