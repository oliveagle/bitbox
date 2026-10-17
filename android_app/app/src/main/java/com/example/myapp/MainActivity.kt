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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.textClickHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painterPainter
import androidx.compose.ui.platform.clipToBounds
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = materialColorScheme(
                    primary = mdBlue,
                    secondary = mdPurple,
                    background = mdBackground,
                    surface = mdSurface
                )
            ) {
                HarnessApp()
            }
        }
    }
}

// Color scheme matching harness aesthetic
private val mdBlue = androidx.compose.material3.Color(0xFF1976D2)
private val mdPurple = androidx.compose.material3.Color(0xFF6200EE)
private val mdBackground = androidx.compose.material3.Color(0xFFF5F5F7)
private val mdSurface = androidx.compose.material3.Color(0xFFFFFFFF)

@Composable
fun HarnessApp() {
    val viewModel: HarnessViewModel = viewModel()
    var clipboardImage by remember { mutableStateOf<Uri?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Harness", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Device Info Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Device Information", fontSize = 18.sp, fontWeight = FontWeight.Semibold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Device: ${Build.DEVICE}", style = MaterialTheme.typography.bodyMedium)
                    Text("Model: ${Build.MODEL}", style = MaterialTheme.typography.bodyMedium)
                    Text("Manufacturer: ${Build.MANUFACTURER}", style = MaterialTheme.typography.bodyMedium)
                    Text("Android Version: ${Build.VERSION.RELEASE}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Clipboard Image Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Clipboard Image", fontSize = 18.sp, fontWeight = FontWeight.Semibold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (clipboardImage != null) {
                        Image(
                            painter = painterPainter(uri = clipboardImage!!),
                            contentDescription = "Clipboard image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clipToBounds()
                        )
                    } else {
                        Text("No image in clipboard", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    Button(
                        onClick = { 
                            viewModel.checkClipboardImage { uri ->
                                clipboardImage = uri
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Check Clipboard for Image")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // App Tasks Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("App Tasks", fontSize = 18.sp, fontWeight = FontWeight.Semibold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = { viewModel.moveToBack() }) {
                            Text("Move to Back")
                        }
                        Button(onClick = { viewModel.openSettings() }) {
                            Text("Open Settings")
                        }
                    }
                }
            }
        }
    }
}

// ViewModel to handle harness-specific logic
class HarnessViewModel : androidx.lifecycle.ViewModel() {
    fun checkClipboardImage(onResult: (Uri?) -> Unit) {
        // This would need Context, so we'll handle it in the Activity
        // For now, just a placeholder
        onResult(null)
    }
    
    fun moveToBack() {
        // Move task to back
    }
    
    fun openSettings() {
        // Open app settings
    }
}

// Helper functions
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
