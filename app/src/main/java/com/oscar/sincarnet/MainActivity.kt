package com.oscar.sincarnet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import kotlinx.coroutines.delay

private const val CASES_ROUTE = "cases"
private const val EXPIRED_VALIDITY_ROUTE = "expired_validity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SinCarnetTheme {
                var showSplash by remember { mutableStateOf(true) }
                var currentRoute by rememberSaveable { mutableStateOf(CASES_ROUTE) }

                LaunchedEffect(Unit) {
                    delay(3000)
                    showSplash = false
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (showSplash) {
                        SplashScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    } else {
                        when (currentRoute) {
                            EXPIRED_VALIDITY_ROUTE -> ExpiredValidityScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                onBackClick = { currentRoute = CASES_ROUTE }
                            )

                            else -> CasesScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                onExpiredValidityClick = { currentRoute = EXPIRED_VALIDITY_ROUTE }
                            )
                        }
                    }
                }
            }
        }
    }
}

