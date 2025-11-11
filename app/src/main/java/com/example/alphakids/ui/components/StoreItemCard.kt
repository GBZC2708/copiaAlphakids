package com.example.alphakids.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alphakids.ui.theme.AlphakidsTheme
import com.example.alphakids.ui.theme.dmSansFamily

@Composable
fun CompactPriceChip(
    modifier: Modifier = Modifier,
    price: Int,
    icon: ImageVector = Icons.Rounded.Savings,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    val shape = RoundedCornerShape(28.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Precio",
            modifier = Modifier.size(12.dp),
            tint = iconColor
        )

        Text(
            text = price.toString(),
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = iconColor
        )
    }
}

@Composable
fun StoreItemCard(
    modifier: Modifier = Modifier,
    title: String,
    price: Int,
    itemImage: Painter,
    onClickBuy: () -> Unit,
    enabled: Boolean = true,
    isProcessing: Boolean = false,
    supportingText: String? = null
) {
    val shape = RoundedCornerShape(28.dp)
    val fixedWidth = 171.dp
    val fixedHeight = 220.dp

    Column(
        modifier = modifier
            .width(fixedWidth)
            .height(fixedHeight)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .padding(all = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Image(
                painter = itemImage,
                contentDescription = title,
                modifier = Modifier
                    .size(91.dp)
                    .align(Alignment.Center)
            )

            CompactPriceChip(
                price = price,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = title,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(5.dp))

        // Botón Comprar (usando PrimaryButton estándar, pero con padding y colores deshabilitados)
        Button(
            onClick = onClickBuy,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && !isProcessing,
            shape = RoundedCornerShape(28.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 5.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "Comprar",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }

        if (!supportingText.isNullOrBlank()) {
            Text(
                text = supportingText,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

// --- PREVIEWS ---

// Simulación de recursos, ya que no tengo acceso a tus drawable
@Composable
private fun SampleImage(contentDescription: String): Painter {
    return painterResource(id = android.R.drawable.ic_menu_gallery)
}


@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun StoreItemCardPreview() {
    AlphakidsTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
            // 1. Estado Normal (Suficientes fondos)
            StoreItemCard(
                title = "Croquetas",
                price = 30,
                itemImage = SampleImage("Croquetas"),
                onClickBuy = {},
                enabled = true
            )

            // 2. Estado Deshabilitado (Faltan fondos)
            StoreItemCard(
                title = "Pescado",
                price = 300,
                itemImage = SampleImage("Pescado"),
                onClickBuy = {},
                enabled = false,
                supportingText = "Monedas insuficientes"
            )
        }
    }
}
