package com.messledger.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.messledger.app.data.repository.AuthRepository
import com.messledger.app.ui.navigation.MessLedgerNavGraph
import com.messledger.app.ui.navigation.Routes
import com.messledger.app.ui.theme.MessLedgerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val startDestination = if (authRepository.isLoggedIn()) Routes.HOME else Routes.AUTH

        setContent {
            MessLedgerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    MessLedgerNavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
