package com.example.myapp

import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                HarnessApp()
            }
        }
    }
}

@Composable
fun HarnessApp() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Harness Android App")
        Text(text = "Device: ${Build.DEVICE}")
        Text(text = "Model: ${Build.MODEL}")
        Text(text = "Manufacturer: ${Build.MANUFACTURER}")
    }
}

// Helper functions for harness core features
private fun clipboardHasImage(context: Context): Boolean {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
    val description = try { clipboard.primaryClipDescription } catch (e: Exception) { null }
    return description?.hasMimeType("image/*") == true
}

private fun readAsPng(resolver: ContentResolver, uri: Uri): ByteArray? {
    return try {
        resolver.openInputStream(uri)?.use { it.readBytes() }
            ?.let { encoded ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(encoded, 0, encoded.size, bounds)
                val longEdge = maxOf(bounds.outWidth, bounds.outHeight)
                var sample = 1
                while (longEdge / (sample * 2) >= 1600) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeByteArray(encoded, 0, encoded.size, options)?.let { decoded ->
                    val bitmap = Bitmap.createScaledBitmap(decoded, 
                        (decoded.width * (1600f / longEdge)).toInt().coerceAtLeast(1),
                        (decoded.height * (1600f / longEdge)).toInt().coerceAtLeast(1), true)
                    if (bitmap !== decoded) decoded.recycle()
                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    bitmap.recycle()
                    out.toByteArray()
                }
            }
    } catch (e: Exception) {
        null
    }
}
