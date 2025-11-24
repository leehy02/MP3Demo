package com.example.demo.ui.screen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.demo.ui.theme.GmarketSans
import kotlin.math.roundToInt

@Composable
fun <T> SlideStepperWithLabels(
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

    val density = LocalDensity.current
    val sidePaddingPx = with(density) { 8.dp.toPx() }
    var trackWidth by remember { mutableStateOf(0f) }

    // x좌표 → 눈금 인덱스
    fun xToIndex(x: Float): Int {
        val clamped = x.coerceIn(sidePaddingPx, trackWidth - sidePaddingPx)
        val usable = (trackWidth - 2 * sidePaddingPx).coerceAtLeast(1f)
        val ratio = if (steps == 1) 0f else (clamped - sidePaddingPx) / usable
        return ((ratio * (steps - 1)).toDouble()).roundToInt().coerceIn(0, steps - 1)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ▶︎ 상단: 스텝퍼 바 (화살표 제거)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            // 제스처 레이어 (탭/드래그)
            if (enableSwipe) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .onSizeChanged { trackWidth = it.width.toFloat() }
                        // 탭으로 점프
                        .pointerInput(steps) {
                            detectTapGestures { offset ->
                                onChange(xToIndex(offset.x))
                            }
                        }
                        // 드래그로 이동
                        .pointerInput(steps) {
                            detectDragGestures(
                                onDragStart = { start -> onChange(xToIndex(start.x)) },
                                onDrag = { change, _ ->
                                    onChange(xToIndex(change.position.x))
                                }
                            )
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

        Spacer(Modifier.height(6.dp))

        // 하단 라벨
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
