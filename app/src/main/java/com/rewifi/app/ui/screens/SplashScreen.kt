package com.rewifi.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Snow
import com.rewifi.app.ui.theme.Yellow
import kotlin.random.Random

/** Total runtime of the animated splash before it auto-advances. */
private const val SPLASH_TOTAL_MS = 4000L

/**
 * Uniform time-stretch applied to every segment. The timeline was authored to
 * end ~2.1s; scaling it spreads the same motion across the longer 4s splash so
 * it never freezes into a long static hold before advancing.
 */
private const val TIME_SCALE = 1.7f

/** Back-ease-out (overshoot) — gives the snappy mechanical bounce. */
private fun overshoot(t: Float): Float {
    val s = 1.70158f
    val x = t.coerceIn(0f, 1f) - 1f
    return x * x * ((s + 1) * x + s) + 1f
}

/** Local 0..1 progress for a segment of the timeline that starts at [start] ms and lasts [dur] ms. */
private fun seg(elapsed: Long, start: Float, dur: Float): Float =
    ((elapsed - start * TIME_SCALE) / (dur * TIME_SCALE)).coerceIn(0f, 1f)

/**
 * Animated brutalist splash. Timeline (ms), all fired off a single frame clock:
 *  - 0–650   QR squares scatter in and snap into a grid           (#4)
 *  - 0–520   logo card stamps in with a hard offset shadow        (#1)
 *  - 650–950 QR crossfades into the launcher icon
 *  - 700–1200 WiFi signal arcs draw outward                       (#2)
 *  - 1050–1700 "REWIFI" letters drop in one by one, mechanical    (#5)
 *  - 1500–2100 signal-bar loader fills left→right + tagline       (#6)
 *
 * [onFinish] fires when the timeline ends or the user taps SKIP.
 */
@Composable
fun SplashScreen(onFinish: () -> Unit = {}) {
    var elapsed by remember { mutableLongStateOf(0L) }
    var done by remember { mutableStateOf(false) }
    fun finish() { if (!done) { done = true; onFinish() } }

    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        var now = start
        while (now - start < SPLASH_TOTAL_MS) {
            now = withFrameMillis { it }
            elapsed = now - start
        }
        finish()
    }

    Box(Modifier.fillMaxSize().background(Yellow), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WifiArcs(elapsed)
            Spacer(Modifier.height(18.dp))
            LogoStamp(elapsed)
            Spacer(Modifier.height(26.dp))
            Wordmark(elapsed)
            Spacer(Modifier.height(22.dp))
            SignalLoader(elapsed)
        }
    }
}

/** #2 — three WiFi arcs that draw outward from a dot. */
@Composable
private fun WifiArcs(elapsed: Long) {
    Canvas(Modifier.size(width = 76.dp, height = 46.dp)) {
        val cx = size.width / 2f
        val cy = size.height
        val sw = size.width * 0.055f

        val dotP = seg(elapsed, 640f, 180f)
        if (dotP > 0f) {
            drawCircle(Ink, radius = size.width * 0.05f * dotP, center = Offset(cx, cy - sw))
        }
        for (k in 0..2) {
            val p = seg(elapsed, 700f + k * 170f, 300f)
            if (p <= 0f) continue
            val r = size.width * (0.17f + k * 0.15f)
            drawArc(
                color = Ink,
                startAngle = 270f - 45f * p,
                sweepAngle = 90f * p,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(2 * r, 2 * r),
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
        }
    }
}

/** #1 + #4 — hard-shadow logo card; QR assembles inside then crossfades to the icon. */
@Composable
private fun LogoStamp(elapsed: Long) {
    val stamp = overshoot(seg(elapsed, 0f, 520f))
    val scale = 0.6f + 0.4f * stamp
    val rot = -16f + 8f * seg(elapsed, 0f, 520f)
    val shadow = 10f * seg(elapsed, 0f, 520f)

    val qrAlpha = 1f - seg(elapsed, 660f, 260f)
    val logoAlpha = seg(elapsed, 700f, 300f)

    Box(Modifier.scale(scale).rotate(rot), contentAlignment = Alignment.Center) {
        // Hard offset shadow.
        Box(
            Modifier.size(96.dp)
                .offset(x = shadow.dp, y = shadow.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Ink)
        )
        // Card.
        Box(
            Modifier.size(96.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Snow)
                .border(4.dp, Ink, RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (qrAlpha > 0f) {
                Box(Modifier.alpha(qrAlpha)) { QrGrid(elapsed) }
            }
            Image(
                painter = painterResource(com.rewifi.app.R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(92.dp).alpha(logoAlpha)
            )
        }
    }
}

/** A 5×5 QR-ish pattern whose filled cells scatter in and snap to the grid. */
@Composable
private fun QrGrid(elapsed: Long) {
    val pattern = remember {
        intArrayOf(
            1, 1, 1, 0, 1,
            1, 0, 1, 1, 1,
            1, 1, 0, 0, 1,
            0, 1, 1, 1, 0,
            1, 0, 1, 0, 1,
        )
    }
    val offsets = remember {
        val rnd = Random(42)
        List(25) { Pair((rnd.nextFloat() * 2 - 1) * 130f, (rnd.nextFloat() * 2 - 1) * 130f) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (r in 0 until 5) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (c in 0 until 5) {
                    val i = r * 5 + c
                    if (pattern[i] == 0) {
                        Spacer(Modifier.size(10.dp))
                    } else {
                        val p = overshoot(seg(elapsed, i * 11f, 360f))
                        val (dx, dy) = offsets[i]
                        Box(
                            Modifier.size(10.dp)
                                .offset(x = (dx * (1f - p)).dp, y = (dy * (1f - p)).dp)
                                .alpha((p * 3f).coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(2.dp))
                                .background(Ink)
                        )
                    }
                }
            }
        }
    }
}

/** #5 — "REWIFI" letters drop in one at a time as little stamped cards. */
@Composable
private fun Wordmark(elapsed: Long) {
    val letters = "REWIFI"
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        letters.forEachIndexed { idx, ch ->
            StampLetter(ch, seg(elapsed, 1050f + idx * 90f, 420f))
        }
    }
}

@Composable
private fun StampLetter(ch: Char, p: Float) {
    val o = overshoot(p)
    val y = (1f - o) * -70f
    val a = (p * 4f).coerceIn(0f, 1f)
    Box(
        Modifier.size(width = 30.dp, height = 52.dp).alpha(a).offset(y = y.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(width = 30.dp, height = 52.dp)
                .offset(x = 5.dp, y = 5.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Ink)
        )
        Box(
            Modifier.size(width = 30.dp, height = 52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Snow)
                .border(3.dp, Ink, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(ch.toString(), fontWeight = FontWeight.Black, fontSize = 30.sp, color = Ink)
        }
    }
}

/** #6 — four bars fill left→right as a brutalist loader, with the tagline below. */
@Composable
private fun SignalLoader(elapsed: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 0 until 4) {
                val p = seg(elapsed, 1500f + i * 120f, 300f)
                val h = (14 + i * 7).dp
                Box(
                    Modifier.width(12.dp).height(h)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Snow)
                        .border(3.dp, Ink, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        Modifier.fillMaxWidth().fillMaxHeight(p)
                            .align(Alignment.BottomCenter)
                            .background(Ink)
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "YOUR WIFI, FOREVER.",
            fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink,
            letterSpacing = 2.sp,
            modifier = Modifier.alpha(seg(elapsed, 1700f, 400f))
        )
    }
}
