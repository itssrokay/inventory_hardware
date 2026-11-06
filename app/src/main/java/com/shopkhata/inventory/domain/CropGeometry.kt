package com.shopkhata.inventory.domain

import kotlin.math.*

data class CropRegion(val left: Int, val top: Int, val width: Int, val height: Int)
/** Same cover/zoom/pan transform as the preview; clamps every crop to source pixels. */
fun cropRegion(width: Int, height: Int, frameWidth: Double, frameHeight: Double, zoom: Double, panX: Double, panY: Double): CropRegion {
    require(width > 0 && height > 0 && frameWidth > 0 && frameHeight > 0 && zoom.isFinite() && zoom >= 1)
    val scale = max(frameWidth / width, frameHeight / height) * zoom
    val visibleWidth = min(width, max(1, (frameWidth / scale).roundToInt()))
    val visibleHeight = min(height, max(1, (frameHeight / scale).roundToInt()))
    val left = ((width - frameWidth / scale) / 2 - panX / scale).roundToInt().coerceIn(0, width - visibleWidth)
    val top = ((height - frameHeight / scale) / 2 - panY / scale).roundToInt().coerceIn(0, height - visibleHeight)
    return CropRegion(left, top, visibleWidth, visibleHeight)
}
