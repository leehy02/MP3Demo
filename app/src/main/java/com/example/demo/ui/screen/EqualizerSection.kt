package com.example.demo.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.demo.ble.BleViewModel
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun EqualizerSection(vm: BleViewModel) {
    SettingSection(title = "Graphic Equalizer") {

        val eqIdMap = remember {
            mapOf(31 to 0x60, 63 to 0x61, 125 to 0x62, 250 to 0x63, 500 to 0x64,
                1000 to 0x65, 2000 to 0x66, 4000 to 0x67, 8000 to 0x68, 16000 to 0x69)
        }
        fun encodeDb(db: Int): Int = (db + 16).coerceIn(0x04, 0x1C)

        // 🔸 직전에 보낸 스냅샷을 저장해두고, 바뀐 것만 보냄
        val prevSnap = remember { mutableStateMapOf<Int, Int>() } // freq -> int dB

        EqualizerContent(
            onValueChangeLive = { freq, dbInt ->
                eqIdMap[freq]?.let { id ->
                    vm.sendCommandQueued(id, encodeDb(dbInt)) // ❗️큐 전송(아래 2번 패치 참고)
                    prevSnap[freq] = dbInt
                }
            },
            onValueChange = {   }
        )
    }
}


/** 이퀄라이저 UI (10밴드, -12~+12 dB, 1 dB 스텝) */
@Composable
private fun EqualizerContent(
    freqs: List<Int> = listOf(31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000),
    minDb: Float = -12f,
    maxDb: Float = 12f,
    onValueChange: (Map<Int, Int>) -> Unit = {},
    onValueChangeLive: ((freq: Int, dbInt: Int) -> Unit)? = null
) {
    val values = remember { mutableStateListOf<Float>().apply { addAll(List(freqs.size) { 0f }) } }
    val lastSentInt = remember { IntArray(freqs.size) { Int.MIN_VALUE } }      // 밴드별 마지막 전송 dB
    val lastSentAt  = remember { LongArray(freqs.size) { 0L } }               // 밴드별 마지막 전송 시각(ms)
    val throttleMs = 50L                                                      // ✅ 50ms 스로틀

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        // ===== 배경: 0 dB 가이드 라인만 =====
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .padding(start = 6.dp, end = 28.dp)
        ) {
            val w = size.width
            val h = size.height

            val corner = CornerRadius(20f, 20f)
            val rr = RoundRect(Rect(0f, 0f, w, h), corner)
            val clipPath = Path().apply { addRoundRect(rr) }

            val t = (0f - minDb) / (maxDb - minDb)
            val y0 = h * (1f - t)

            withTransform({ clipPath(clipPath) }) {
                drawLine(
                    color = Color(0xFFB9B9B9),
                    start = Offset(0f, y0),
                    end = Offset(w, y0),
                    strokeWidth = 1.4f,
                    cap = StrokeCap.Butt
                )
            }
        }

        // ===== 10밴드 페이더 =====
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 6.dp, end = 28.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            freqs.forEachIndexed { idx, f ->
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when (f) {
                            1000 -> "1k"
                            2000 -> "2k"
                            4000 -> "4k"
                            8000 -> "8k"
                            16000 -> "16k"
                            else -> "$f"
                        },
                        color = Color(0xFF606060)
                    )

                    // ★ 드래그 중: 연속값 유지 / 드래그 끝: 정수 스냅 + 콜백
                    VerticalFader(
                        value = values[idx],
                        onChange = { v ->
                            val clamped = v.coerceIn(minDb, maxDb)
                            values[idx] = clamped
                            // 🔴 드래그 중 실시간 전송(정수 dB로만, 스로틀/중복 방지)
                            onValueChangeLive?.let { cb ->
                                val now = System.currentTimeMillis()
                                val intDb = round(clamped).toInt()
                                if (intDb != lastSentInt[idx] && now - lastSentAt[idx] >= throttleMs) {
                                    lastSentInt[idx] = intDb
                                    lastSentAt[idx] = now
                                    cb(f, intDb)
                                }
                            }
                        },
                        onChangeFinished = {
                            onValueChange(freqs.zip(values.map { round(it).toInt() }).toMap())
                        },
                        min = minDb,
                        max = maxDb,
                        height = 170.dp,
                        trackWidth = 6.dp,
                        knobOuter = 10.dp,
                        knobInner = 8.dp
                    )

                    Text(
                        text = "%+d".format(values[idx].roundToInt()),
                        color = Color(0xFF707070)
                    )
                }
            }
        }

        // 우측 dB 숫자 눈금
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .width(28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            listOf(12, 8, 4, 0, -4, -8, -12).forEach {
                Text(text = if (it in listOf(12, 0, -12)) "$it" else "", color = Color(0xFF707070), fontSize = 14.sp)
            }
        }
    }
}

/** 커스텀 세로 페이더: 드래그 중 연속값, 종료 시 콜백 */
@Composable
private fun VerticalFader(
    value: Float,
    onChange: (Float) -> Unit,
    onChangeFinished: () -> Unit, // ★ 추가
    min: Float,
    max: Float,
    height: Dp,
    trackWidth: Dp,
    knobOuter: Dp,
    knobInner: Dp
) {
    val padding = 8.dp
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(height)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val boxH = size.height.toFloat()
                        val top = padding.toPx()
                        val bottom = boxH - padding.toPx()
                        val y = offset.y.coerceIn(top, bottom)
                        val v = mapYToValue(y, min, max, top, bottom)
                        onChange(v)
                    },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        val boxH = size.height.toFloat()
                        val top = padding.toPx()
                        val bottom = boxH - padding.toPx()
                        val y = change.position.y.coerceIn(top, bottom)
                        val v = mapYToValue(y, min, max, top, bottom)
                        onChange(v)
                    },
                    onDragEnd = { onChangeFinished() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val top = padding.toPx()
            val bottom = h - padding.toPx()

            // 0 dB와 현재 값의 y 좌표
            val zeroY = mapValueToY(0f.coerceIn(min, max), min, max, top, bottom)
            val knobY = mapValueToY(value, min, max, top, bottom)

            // 1) 전체 트랙 (연한 회색)
            drawLine(
                color = Color(0xFFE0E0E0),
                start = Offset(cx, top),
                end   = Offset(cx, bottom),
                strokeWidth = trackWidth.toPx(),
                cap = StrokeCap.Round
            )

            // 2) 활성 구간 (0 ↔ 현재 값, 진한 회색)
            drawLine(
                color = Color(0xFF9C9C9C),
                start = Offset(cx, zeroY),
                end   = Offset(cx, knobY),
                strokeWidth = trackWidth.toPx(),
                cap = StrokeCap.Round
            )

            // 3) 노브 (위에 덮어그리기)
            drawCircle(
                color = Color(0xFFC0C0C0),
                radius = knobOuter.toPx(),
                center = Offset(cx, knobY)
            )
            drawCircle(
                color = Color.White,
                radius = knobInner.toPx(),
                center = Offset(cx, knobY)
            )
        }

    }
}

private fun mapValueToY(v: Float, min: Float, max: Float, top: Float, bottom: Float): Float {
    val t = ((v - min) / (max - min)).coerceIn(0f, 1f)
    return bottom - (bottom - top) * t
}

private fun mapYToValue(y: Float, min: Float, max: Float, top: Float, bottom: Float): Float {
    val t = ((bottom - y) / (bottom - top)).coerceIn(0f, 1f)
    return min + (max - min) * t
}
