package pe.edu.upeu.pharmamobilee

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.koin.core.context.GlobalContext
import pe.edu.upeu.pharmamobilee.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobilee.permiso.GestorPermisosAndroid

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        (GlobalContext.get().get<GestorPermisos>() as? GestorPermisosAndroid)?.adjuntar(this)

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        (GlobalContext.get().get<GestorPermisos>() as? GestorPermisosAndroid)?.separar(this)
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
