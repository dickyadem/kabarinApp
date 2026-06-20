package com.kabarinpacar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kabarinpacar.app.data.repository.StatusRepository
import com.kabarinpacar.app.service.PartnerStatusService
import com.kabarinpacar.app.ui.navigation.KabarinNavGraph
import com.kabarinpacar.app.ui.theme.KabarinPacarTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var statusRepository: StatusRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (statusRepository.isPaired) {
            PartnerStatusService.start(this)
        }

        setContent {
            KabarinPacarTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    KabarinNavGraph(
                        navController = navController,
                        onPairingComplete = { PartnerStatusService.start(this) }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
