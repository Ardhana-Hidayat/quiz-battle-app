package id.ac.pnm.quizbattleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import id.ac.pnm.quizbattleapp.ui.navigation.NavGraph
import id.ac.pnm.quizbattleapp.ui.theme.QuizBattleAppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { QuizBattleAppTheme { NavGraph() } }
    }
}
