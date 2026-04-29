package com.billfolder.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.billfolder.android.ui.navigation.BillFolderNavHost
import com.billfolder.android.ui.theme.BillFolderTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity raiz. Monta o NavHost depois que o MainViewModel resolve
 * o estado inicial de auth (se há ou não token salvo).
 *
 * Enquanto isLoggedIn é null, renderiza apenas o background — evita
 * o flash da tela de login se o usuário tem sessão válida e teria
 * sido redirecionado pra Home num frame depois. Em prática a leitura
 * do DataStore acontece em ms, então o usuário vê só o background
 * por uma fração de segundo.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BillFolderTheme {
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        isLoggedIn?.let { logged ->
                            BillFolderNavHost(isLoggedIn = logged)
                        }
                    }
                }
            }
        }
    }
}
