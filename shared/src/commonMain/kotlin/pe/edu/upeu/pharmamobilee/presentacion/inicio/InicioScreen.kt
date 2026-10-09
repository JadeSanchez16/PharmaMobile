package pe.edu.upeu.pharmamobilee.presentacion.inicio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobilee.theme.AzulClaro
import pe.edu.upeu.pharmamobilee.theme.AzulInstitucional
import pe.edu.upeu.pharmamobilee.theme.AzulNoche

@Composable
fun InicioScreen(
    totalProductos: Int,
    productosActivos: Int,
    productosInactivos: Int,
    productosBajoStock: Int,
    onProductosClick: () -> Unit,
    onClientesClick: () -> Unit,
    onPedidosClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        HeroPanel(onProductosClick)
        ResumenInventario(
            total = totalProductos,
            activos = productosActivos,
            inactivos = productosInactivos,
            bajoStock = productosBajoStock,
            onClick = onProductosClick
        )
        AccesosRapidos(onProductosClick, onClientesClick, onPedidosClick)
    }
}

@Composable
private fun HeroPanel(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = AzulInstitucional
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.14f)) {
                Icon(
                    imageVector = Icons.Default.LocalPharmacy,
                    contentDescription = null,
                    tint = AzulClaro,
                    modifier = Modifier.padding(15.dp).size(36.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "PharmaMobile",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Sistema de gestión farmacéutica",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
private fun ResumenInventario(
    total: Int,
    activos: Int,
    inactivos: Int,
    bajoStock: Int,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Resumen del inventario", style = MaterialTheme.typography.titleLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IndicadorCard(total, "Productos", Icons.Default.Inventory2, Modifier.weight(1f), onClick)
            IndicadorCard(activos, "Activos", Icons.Default.CheckCircle, Modifier.weight(1f), onClick)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IndicadorCard(inactivos, "Inactivos", Icons.Default.Block, Modifier.weight(1f), onClick)
            IndicadorCard(bajoStock, "Bajo stock", Icons.Default.WarningAmber, Modifier.weight(1f), onClick)
        }
    }
}

@Composable
private fun IndicadorCard(
    valor: Int,
    etiqueta: String,
    icono: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.heightIn(min = 108.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icono, null, tint = AzulInstitucional, modifier = Modifier.size(23.dp))
            Text(valor.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(etiqueta, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccesosRapidos(
    onProductosClick: () -> Unit,
    onClientesClick: () -> Unit,
    onPedidosClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Accesos rápidos", style = MaterialTheme.typography.titleLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccesoCard("Productos", Icons.Default.Medication, onProductosClick, Modifier.weight(1f))
            AccesoCard("Clientes", Icons.Default.Groups, onClientesClick, Modifier.weight(1f))
            AccesoCard("Pedidos", Icons.Default.ShoppingCartCheckout, onPedidosClick, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AccesoCard(
    titulo: String,
    icono: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 104.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = AzulNoche) {
                Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Icon(icono, null, tint = Color.White, modifier = Modifier.size(23.dp))
                }
            }
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
