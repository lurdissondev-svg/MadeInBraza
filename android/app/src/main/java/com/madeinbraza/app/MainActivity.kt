package com.madeinbraza.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.madeinbraza.app.ui.BrazaNavHost
import com.madeinbraza.app.ui.theme.BrazaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrazaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BrazaNavHost()
                }
            }
        }
    }
}
