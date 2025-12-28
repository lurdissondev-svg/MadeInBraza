package com.madeinbraza.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.madeinbraza.app.ui.BrazaNavHost
import com.madeinbraza.app.ui.theme.BrazaTheme
import com.madeinbraza.app.util.LanguageManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val languageCode = runBlocking {
            LanguageManager.getLanguageFlow(newBase).first()
        }
        val context = LanguageManager.applyLanguage(newBase, languageCode)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val languageCode by LanguageManager.getLanguageFlow(this).collectAsState(initial = "pt")

            BrazaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BrazaNavHost(
                        onLanguageChanged = {
                            recreate()
                        }
                    )
                }
            }
        }
    }
}
