package com.myshelf.app.features.home

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/** Static vector doodles stay at the edges so the greeting remains easy to read. */
@Composable
fun HomeWelcomeDoodle(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.tertiary
    Canvas(modifier.clipToBounds().clearAndSetSemantics {}) {
        val w = size.width
        val h = size.height
        val u = minOf(w / 400f, h / 160f)
        val ink = accent.copy(alpha = .3f)
        val pen = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round)
        drawRect(Brush.horizontalGradient(listOf(accent.copy(alpha = .045f), accent.copy(alpha = 0f), secondary.copy(alpha = .045f))))
        drawCircle(accent.copy(alpha = .055f), h * .48f, Offset(w * .03f, h * .2f))
        drawCircle(secondary.copy(alpha = .06f), h * .42f, Offset(w * .98f, h * .85f))
        fun line(a: Offset, b: Offset) = drawLine(ink, a, b, pen.width, StrokeCap.Round)
        fun star(x: Float, y: Float, r: Float) {
            val center = Offset(w * x, h * y)
            line(center - Offset(r, 0f), center + Offset(r, 0f))
            line(center - Offset(0f, r), center + Offset(0f, r))
        }
        // A tilted clapperboard.
        withTransform({ translate(w * .07f, h * .12f); rotate(-12f, Offset.Zero) }) {
            drawRoundRect(ink, Offset.Zero, Size(46f * u, 32f * u), CornerRadius(4f * u), style = pen)
            line(Offset(0f, 10f * u), Offset(46f * u, 10f * u))
            for (i in 0..3) line(Offset((4f + i * 11f) * u, 0f), Offset((10f + i * 11f) * u, 10f * u))
            line(Offset(10f * u, 20f * u), Offset(34f * u, 20f * u))
        }
        // An open book, with curved pages and a stitched spine.
        withTransform({ translate(w * .1f, h * .73f); rotate(9f, Offset.Zero) }) {
            val book = Path().apply {
                moveTo(0f, 0f); quadraticTo(13f * u, -7f * u, 27f * u, 0f)
                quadraticTo(40f * u, -7f * u, 54f * u, 0f)
                lineTo(54f * u, 29f * u); quadraticTo(40f * u, 22f * u, 27f * u, 29f * u)
                quadraticTo(13f * u, 22f * u, 0f, 29f * u); close()
            }
            drawPath(book, ink, style = pen)
            line(Offset(27f * u, 1f * u), Offset(27f * u, 29f * u))
            line(Offset(7f * u, 10f * u), Offset(20f * u, 10f * u))
            line(Offset(34f * u, 10f * u), Offset(47f * u, 10f * u))
            line(Offset(7f * u, 17f * u), Offset(19f * u, 18f * u))
            line(Offset(34f * u, 18f * u), Offset(47f * u, 17f * u))
        }
        // A film reel in the opposite corner.
        val reel = Offset(w * .9f, h * .24f)
        drawCircle(ink, 22f * u, reel, style = pen)
        drawCircle(ink, 3f * u, reel, style = pen)
        listOf(Offset(-9f, -9f), Offset(9f, -9f), Offset(-9f, 9f), Offset(9f, 9f)).forEach {
            drawCircle(ink, 4f * u, reel + it * u, style = pen)
        }
        // Comic speech bubble.
        withTransform({ translate(w * .83f, h * .7f); rotate(-8f, Offset.Zero) }) {
            val bubble = Path().apply {
                moveTo(7f * u, 0f); lineTo(40f * u, 0f); quadraticTo(47f * u, 0f, 47f * u, 7f * u)
                lineTo(47f * u, 22f * u); quadraticTo(47f * u, 29f * u, 40f * u, 29f * u)
                lineTo(18f * u, 29f * u); lineTo(8f * u, 38f * u); lineTo(9f * u, 29f * u)
                quadraticTo(0f, 29f * u, 0f, 22f * u); lineTo(0f, 7f * u); quadraticTo(0f, 0f, 7f * u, 0f); close()
            }
            drawPath(bubble, ink, style = pen)
            for (i in 0..2) drawCircle(ink, 1.5f * u, Offset((14f + i * 9f) * u, 14f * u))
        }
        star(.32f, .13f, 5f * u)
        star(.68f, .83f, 5f * u)
        star(.96f, .49f, 4f * u)
        star(.04f, .57f, 4f * u)
        listOf(Offset(.22f, .35f), Offset(.73f, .19f), Offset(.3f, .88f), Offset(.76f, .58f)).forEach {
            drawCircle(ink.copy(alpha = .18f), 1.8f * u, Offset(w * it.x, h * it.y))
        }
    }
}
