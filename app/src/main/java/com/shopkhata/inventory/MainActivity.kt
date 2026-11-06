package com.shopkhata.inventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.shopkhata.inventory.ui.DukaanApp
import com.shopkhata.inventory.ui.InventoryViewModel

class MainActivity : ComponentActivity() {
    private val inventory: InventoryViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DukaanApp(inventory) }
    }
}
