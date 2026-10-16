package com.example.myupcchat

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Evidencia visual de las pruebas, con datos ficticios y sin llamadas a Gemini. */
internal fun captureWorkshopScreenshot(name: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "qa")
    directory.mkdirs()
    val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
    File(directory, "$name.png").outputStream().use {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
    }
    bitmap.recycle()
}
