package com.shopkhata.inventory.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shopkhata.inventory.data.PhotoStore
import com.shopkhata.inventory.domain.cropRegion
import kotlin.math.max

@Composable
fun ImageCropDialog(uri: Uri, dismiss: () -> Unit, confirm: (Bitmap) -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var error by remember(uri) { mutableStateOf<String?>(null) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var aspect by remember { mutableStateOf<Float?>(null) }
    var frame by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(uri) { try { bitmap = PhotoStore(context).load(uri) } catch (_: Exception) { error = "Could not open this image. Choose another photo." } }
    // Compose's renderer can retain the previous image after disposal/rotation.
    // Let GC reclaim displayed bitmaps once rendering has released them.
    fun reset() { zoom = 1f; pan = Offset.Zero }
    fun move(delta: Offset, newZoom: Float) {
        val image = bitmap ?: return
        zoom = newZoom.coerceIn(1f, 5f)
        val base = max(frame.width.toFloat() / image.width, frame.height.toFloat() / image.height)
        val maxX = ((image.width * base * zoom - frame.width) / 2).coerceAtLeast(0f)
        val maxY = ((image.height * base * zoom - frame.height) / 2).coerceAtLeast(0f)
        pan = Offset((pan.x + delta.x).coerceIn(-maxX, maxX), (pan.y + delta.y).coerceIn(-maxY, maxY))
    }
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Crop your image", style = MaterialTheme.typography.titleLarge)
                Text("Drag to position, pinch or use Zoom. Only this crop is used.", style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                bitmap?.let { image ->
                    val ratio = aspect ?: (image.width.toFloat() / image.height)
                    Box(Modifier.fillMaxWidth().aspectRatio(ratio).clipToBounds().background(Color(0xFF202820))
                        .onSizeChanged { frame = it }
                        .pointerInput(image, frame) { detectTransformGestures { _, delta, scale, _ -> move(delta, zoom * scale) } }
                        .testTag("crop-preview")) {
                        Image(image.asImageBitmap(), "Crop preview", Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = pan.x; translationY = pan.y }, contentScale = ContentScale.Crop)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("Original" to null, "Square" to 1f, "4:3" to 4f/3f).forEach { (label, value) ->
                            FilterChip(selected = aspect == value, onClick = { aspect = value; reset() }, label = { Text(label) })
                        }
                    }
                    Text("Zoom ${"%.1f".format(zoom)}×", style = MaterialTheme.typography.bodySmall)
                    Slider(value = zoom, onValueChange = { move(Offset.Zero, it) }, valueRange = 1f..5f, modifier = Modifier.testTag("crop-zoom"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { bitmap = Bitmap.createBitmap(image, 0, 0, image.width, image.height, Matrix().apply { postRotate(90f) }, true); reset() }) { Text("Rotate 90°") }
                        TextButton(onClick = { reset() }) { Text("Reset crop") }
                    }
                } ?: run { if (error == null) CircularProgressIndicator(); Unit }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = dismiss) { Text("Cancel") }
                    Button(enabled = bitmap != null && frame.width > 0 && frame.height > 0, onClick = {
                        val image = bitmap ?: return@Button
                        val region = cropRegion(image.width, image.height, frame.width.toDouble(), frame.height.toDouble(), zoom.toDouble(), pan.x.toDouble(), pan.y.toDouble())
                        val cropped = Bitmap.createBitmap(image, region.left, region.top, region.width, region.height)
                        val result = requireNotNull(cropped.copy(Bitmap.Config.ARGB_8888, false))
                        if (cropped !== image) cropped.recycle()
                        confirm(result)
                    }) { Text("Use crop") }
                }
            }
        }
    }
}
