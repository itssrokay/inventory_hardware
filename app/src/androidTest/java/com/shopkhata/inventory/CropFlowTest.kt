package com.shopkhata.inventory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shopkhata.inventory.data.PhotoStore
import com.shopkhata.inventory.ui.ImageCropDialog
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CropFlowTest {
    @get:Rule val compose=createComposeRule()
    @Test fun cropRotateAndZoomSaveOnlyTheChosenPixels() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val original=Bitmap.createBitmap(100,200,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        val input=File(context.cacheDir,"crop-fixture.png")
        input.outputStream().use { original.compress(Bitmap.CompressFormat.PNG,100,it) };original.recycle()
        var result:Bitmap?=null
        compose.setContent {
            var show by remember { mutableStateOf(true) }
            MaterialTheme { if(show) ImageCropDialog(Uri.fromFile(input),{show=false}) {result=it;show=false} }
        }
        compose.waitUntil(10000){compose.onAllNodesWithTag("crop-preview").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Square").performClick()
        compose.onNodeWithText("Rotate 90°").performScrollTo().performClick()
        compose.onNodeWithTag("crop-zoom").performSemanticsAction(SemanticsActions.SetProgress){it(2f)}
        compose.onNodeWithText("Use crop").performScrollTo().performClick()
        compose.runOnIdle {
            val cropped=requireNotNull(result)
            assertEquals(50,cropped.width);assertEquals(50,cropped.height)
            assertEquals(Color.GREEN,cropped.getPixel(25,25))
            val path=runBlocking { PhotoStore(context).save(cropped) }
            val saved=BitmapFactory.decodeFile(File(context.filesDir,path).path)
            assertEquals(50,saved.width);assertEquals(50,saved.height)
            saved.recycle();File(context.filesDir,path).delete();input.delete()
        }
    }
}
