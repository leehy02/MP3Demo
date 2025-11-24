package com.example.demo.ui.screen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.demo.ui.theme.GmarketSans
import kotlin.math.max
import kotlin.math.min

@Composable
fun <T> DiscreteStepperWithLabels(
    items: List<T>,
    selectedIndex: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tickHeight: Dp = 10.dp,
    enableSwipe: Boolean = true
) {
    require(items.isNotEmpty())
    val steps = items.size

    val animPos by animateFloatAsState(
        targetValue = selectedIndex.coerceIn(0, steps - 1).toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "discreteStepperAnim"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단: Stepper 바 + 버튼
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { onChange(max(0, selectedIndex - 1)) }) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_media_previous),
                    contentDescription = "Prev"
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                if (enableSwipe) {
                    var acc by remember { mutableStateOf(0f) }
                    val trigger = 36f
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(steps) {
                                detectHorizontalDragGestures(
                                    onDragCancel = { acc = 0f },
                                    onDragEnd = { acc = 0f }
                                ) { _, drag ->
                                    acc += drag
                                    if (acc <= -trigger) {
                                        onChange(min(steps - 1, selectedIndex + 1))
                                        acc = 0f
                                    } else if (acc >= trigger) {
                                        onChange(max(0, selectedIndex - 1))
                                        acc = 0f
                                    }
                                }
                            }
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .align(Alignment.Center)
                        .padding(horizontal = 8.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val y = h / 2f

                    // 트랙
                    drawLine(
                        color = Color(0xFFDDDDDD),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 6f
                    )

                    // 눈금
                    for (i in 0 until steps) {
                        val x = if (steps == 1) 0f else w * i / (steps - 1)
                        drawLine(
                            color = Color(0xFFA1A1A1),
                            start = Offset(x, y - tickHeight.toPx()),
                            end = Offset(x, y + tickHeight.toPx()),
                            strokeWidth = 4f
                        )
                    }

                    // 노브
                    val knobX = if (steps == 1) 0f else w * (animPos / (steps - 1))
                    val rOuter = 8.dp.toPx()
                    val rInner = 6.dp.toPx()
                    drawCircle(color = Color(0xFFB0B0B0), radius = rOuter, center = Offset(knobX, y))
                    drawCircle(color = Color.White, radius = rInner, center = Offset(knobX, y))
                }
            }

            IconButton(onClick = { onChange(min(steps - 1, selectedIndex + 1)) }) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_media_next),
                    contentDescription = "Next"
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // 하단: 라벨 (Voice, Music 등 표시)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items.forEachIndexed { i, label ->
                Text(
                    text = label.toString(),
                    color = if (i == selectedIndex) Color(0xFF333333) else Color.Gray,
                    fontFamily = GmarketSans,
                    fontWeight = if (i == selectedIndex) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
