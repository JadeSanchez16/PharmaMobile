package pe.edu.upeu.pharmamobilee.presentacion.inicio

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobilee.theme.AzulClaro
import pe.edu.upeu.pharmamobilee.theme.AzulInstitucional
import pe.edu.upeu.pharmamobilee.theme.AzulNoche

@Composable
fun InicioScreen(
    totalProductos: Int,
    productosActivos: Int,
    productosBajoStock: Int,
    onProductosClick: () -> Unit,
    onClientesClick: () -> Unit,
    onPedidosClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        HeroPanel(totalProductos, onProductosClick)
        Indicadores(productosActivos, productosBajoStock, onProductosClick)
        AccesosOperativos(onProductosClick, onClientesClick, onPedidosClick)
        SincronizacionCard()
    }
}

@Composable
private fun HeroPanel(totalProductos: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(AzulNoche, AzulInstitucional)
                )
            )
            .clickable(onClick = onClick)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(8.dp), color = AzulClaro) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(15.dp), tint = AzulNoche)
                        Text("RESUMEN EJECUTIVO", style = MaterialTheme.typography.labelMedium, color = AzulNoche)
                    }
                }
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.14f)) {
                    Icon(
                        Icons.Default.LocalPharmacy,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(11.dp).size(26.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Control de inventario\ny operaciones",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
                Text(
                    text = "Consulta existencias y gestiona la operación desde un solo lugar.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.82f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = totalProductos.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.headlineLarge,
                        color = AzulClaro
                    )
                    Text("productos registrados", color = Color.White.copy(alpha = 0.78f))
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Abrir inventario", tint = Color.White)
            }
        }
    }
}

@Composable
private fun Indicadores(activos: Int, bajoStock: Int, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Indicadores de inventario", style = MaterialTheme.typography.titleLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IndicadorCard(
                valor = activos,
                etiqueta = "Disponibles",
                detalle = "listos para vender",
                icono = Icons.Default.Inventory2,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f),
                onClick = onClick
            )
            IndicadorCard(
                valor = bajoStock,
                etiqueta = "Por reponer",
                detalle = if (bajoStock == 0) "todo en orden" else "requieren atención",
                icono = Icons.Default.WarningAmber,
                color = if (bajoStock > 0) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.weight(1f),
                onClick = onClick
            )
        }
    }
}

@Composable
private fun IndicadorCard(
    valor: Int,
    etiqueta: String,
    detalle: String,
    icono: ImageVector,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.heightIn(min = 142.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = color
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icono, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(25.dp))
            Text(valor.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(etiqueta, style = MaterialTheme.typography.titleMedium)
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccesosOperativos(
    onProductosClick: () -> Unit,
    onClientesClick: () -> Unit,
    onPedidosClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Centro de operaciones", style = MaterialTheme.typography.titleLarge)
            Text("Elige una tarea para comenzar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AccesoFila("Inventario", "Productos, precios y existencias", Icons.Default.Inventory2, onProductosClick, true)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccesoCompacto("Clientes", "Contactos", Icons.Default.Groups, onClientesClick, Modifier.weight(1f))
            AccesoCompacto("Pedidos", "Ventas", Icons.Default.ShoppingCartCheckout, onPedidosClick, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AccesoFila(
    titulo: String,
    detalle: String,
    icono: ImageVector,
    onClick: () -> Unit,
    destacado: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (destacado) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = AzulNoche) {
                Icon(icono, null, tint = Color.White, modifier = Modifier.padding(12.dp).size(25.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Abrir $titulo")
        }
    }
}

@Composable
private fun AccesoCompacto(
    titulo: String,
    detalle: String,
    icono: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 128.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(29.dp))
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SincronizacionCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Sync, null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text("Conectado con PharmaSoft", fontWeight = FontWeight.Bold)
                Text("Inventario actualizado desde el servidor", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
