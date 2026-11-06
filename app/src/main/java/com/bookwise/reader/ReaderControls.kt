package com.bookwise.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bookwise.R

@Composable
fun ReaderControls(
    progression: Float?,
    onProgressionChange: (Float) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenToc: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderFraction by remember(progression) { mutableFloatStateOf(progression ?: 0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = stringResource(R.string.reader_progress, (sliderFraction * 100).toInt()))
        Slider(
            value = sliderFraction,
            onValueChange = { sliderFraction = it },
            onValueChangeFinished = { onProgressionChange(sliderFraction) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.ui.Alignment.SpaceBetween
        ) {
            Button(onClick = onPrevious) { Text(text = stringResource(R.string.reader_previous)) }
            Button(onClick = onOpenToc) { Text(text = stringResource(R.string.reader_contents)) }
            Button(onClick = onNext) { Text(text = stringResource(R.string.reader_next)) }
        }
    }
}
