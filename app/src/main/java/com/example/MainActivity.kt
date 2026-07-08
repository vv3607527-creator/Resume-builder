package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.api.GeminiRepository
import com.example.data.database.ResumeDatabase
import com.example.data.repository.ResumeRepository
import com.example.data.repository.UserRepository
import com.example.ui.HomeScreen
import com.example.ui.LandingScreen
import com.example.ui.PreviewScreen
import com.example.ui.SignInScreen
import com.example.ui.SignUpScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.Screen
import com.example.viewmodel.ResumeViewModel
import com.example.viewmodel.ResumeViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room database & repositories for ResuMe (AI Resume Builder)
        val database = ResumeDatabase.getDatabase(this)
        val resumeRepository = ResumeRepository(database.resumeDao())
        val userRepository = UserRepository(database.userDao())
        val geminiRepository = GeminiRepository()
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: ResumeViewModel = viewModel(
                    factory = ResumeViewModelFactory(resumeRepository, userRepository, geminiRepository)
                )
                
                val currentScreen by viewModel.currentScreen.collectAsState()
                
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (val screen = currentScreen) {
                        is Screen.Landing -> {
                            LandingScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        is Screen.SignIn -> {
                            SignInScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        is Screen.SignUp -> {
                            SignUpScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        is Screen.Home, is Screen.Edit -> {
                            HomeScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        is Screen.Preview -> {
                            PreviewScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
