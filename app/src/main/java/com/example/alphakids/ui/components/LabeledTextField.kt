package com.example.alphakids.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alphakids.ui.theme.AlphakidsTheme
import com.example.alphakids.ui.theme.dmSansFamily

@Composable
fun LabeledTextField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    error: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = label,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        BaseTextField(
            value = value,
            onValueChange = onValueChange,
            placeholderText = placeholderText,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            isError = error != null
        )

        if (error != null) {
            Text(
                text = error,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun LabeledTextFieldPreview() {
    var text by remember { mutableStateOf("") }
    AlphakidsTheme {
        LabeledTextField(
            label = "Correo Electrónico",
            value = text,
            onValueChange = { text = it },
            placeholderText = "Escribe tu correo"
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun LabeledTextFieldFilledPreview() {
    var text by remember { mutableStateOf("hola@email.com") }
    AlphakidsTheme {
        LabeledTextField(
            label = "Correo Electrónico",
            value = text,
            onValueChange = { text = it },
            placeholderText = "Escribe tu correo"
        )
    }
}
