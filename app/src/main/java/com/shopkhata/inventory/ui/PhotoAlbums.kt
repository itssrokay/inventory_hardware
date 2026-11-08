package com.shopkhata.inventory.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

@Composable
fun PhotoAlbumEditor(title: String, paths: List<String>, busy: Boolean, add: () -> Unit,
    crop: (Int) -> Unit, remove: (Int) -> Unit, cover: ((Int) -> Unit)? = null) {
    val context=LocalContext.current
    Text(ui(title)+" (${paths.size}/6)",style=MaterialTheme.typography.titleSmall)
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        paths.forEachIndexed { index,path -> Column(Modifier.width(110.dp)) {
            AsyncImage(File(context.filesDir,path),ui(title)+" ${index+1}",Modifier.size(96.dp))
            if(cover!=null)Text(ui(if(index==0)"Cover photo" else "Product photo"),style=MaterialTheme.typography.labelSmall)
            TextButton(onClick={crop(index)},enabled=!busy){Text(ui("Crop photo"))}
            TextButton(onClick={remove(index)},enabled=!busy){Text(ui("Remove photo"))}
            if(cover!=null&&index>0)TextButton(onClick={cover(index)},enabled=!busy){Text(ui("Make cover"))}
        } }
    }
    OutlinedButton(onClick=add,enabled=!busy&&paths.size<6,modifier=Modifier.testTag("add-$title")){Text(ui("Add photos"))}
}

@Composable
fun PhotoGallery(title: String, paths: List<String>) {
    if(paths.isEmpty())return
    val context=LocalContext.current
    var selected by remember { mutableStateOf<Int?>(null) }
    Text(ui(title)+" (${paths.size})",style=MaterialTheme.typography.titleSmall)
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        paths.forEachIndexed { index,path -> AsyncImage(File(context.filesDir,path),"${ui(title)} ${index+1}",Modifier.size(82.dp).clickable {selected=index}) }
    }
    selected?.let { index -> AlertDialog(onDismissRequest={selected=null},title={Text(ui(title)+" ${index+1}/${paths.size}")},text={
        Column {AsyncImage(File(context.filesDir,paths[index]),ui(title),Modifier.fillMaxWidth().height(300.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TextButton(onClick={selected=index-1},enabled=index>0){Text(ui("Previous"))}
                TextButton(onClick={selected=index+1},enabled=index<paths.lastIndex){Text(ui("Next"))}
            }
        }
    },confirmButton={TextButton(onClick={selected=null}){Text(ui("Close"))}}) }
}
