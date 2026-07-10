package com.partitionsoft.stacksy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.partitionsoft.stacksy.core.design.StacksyTheme
import com.partitionsoft.stacksy.core.preferences.PreferencesStore

class MainActivity : ComponentActivity() {
    private val preferencesStore by lazy { PreferencesStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StacksyTheme {
                StacksyApp(preferencesStore)
            }
        }
    }
}
