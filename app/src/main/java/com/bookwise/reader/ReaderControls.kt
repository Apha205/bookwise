package com.bookwise.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.bookwise.R

@Composable
fun ReaderControls(
    progression: Float,
    zoomPercent: Int = 100,
    onProgressionChange: (Float) -> Unit,
    onZoomChange: (Int) -> Unit = {},
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenToc: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Slider(
            value = progression,
            onValueChange = onProgressionChange,
            modifier = Modifier.fillMaxWidth().height(32.dp).semantics {
                contentDescription = "Reading progress"
                stateDescription = "${(progression * 100).toInt()} percent"
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPrevious) { Text(text = stringResource(R.string.reader_previous)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onZoomChange(zoomPercent - 10) }, modifier = Modifier.semantics { stateDescription = "$zoomPercent percent" }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Zoom out")
                }
                Text(text = "$zoomPercent%", style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = { onZoomChange(zoomPercent + 10) }, modifier = Modifier.semantics { stateDescription = "$zoomPercent percent" }) {
                    Icon(Icons.Filled.Add, contentDescription = "Zoom in")
                }
            }
            TextButton(onClick = onNext) { Text(text = stringResource(R.string.reader_next)) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = onOpenToc) { Text(text = stringResource(R.string.reader_contents)) }
        }
    }
}
