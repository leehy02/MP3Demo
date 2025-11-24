package com.example.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import com.example.demo.ui.screen.AudioScreen
import com.example.demo.ui.theme.DemoTheme
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.demo.ble.BleViewModel

class MainActivity : ComponentActivity() {

    // Context를 넣어주는 Factory
    private val vm: BleViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BleViewModel(this@MainActivity) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DemoTheme {
                MaterialTheme {
                    AudioScreen(vm = vm)
                }
            }
        }
    }
}

