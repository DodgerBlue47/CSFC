package com.jhaiian.csfc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jhaiian.csfc.ui.CsfcNavHost
import com.jhaiian.csfc.ui.theme.CSFCTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CSFCTheme {
                CsfcNavHost()
            }
        }
    }
}
