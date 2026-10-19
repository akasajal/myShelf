package com.myshelf.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/** A small, hand-drawn picture frame with a sun, hills, and sparkles. */
@Composable
fun CoverDoodle() {
    val ink = MaterialTheme.colorScheme.primary
    val paper = MaterialTheme.colorScheme.secondaryContainer
    Canvas(Modifier.fillMaxSize().background(paper).padding(22.dp)) {
        val w = size.width
        val h = size.height
        val pen = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(ink, Offset(w * x1, h * y1), Offset(w * x2, h * y2), pen.width, StrokeCap.Round)
        }
        rotate(-8f) {
            val frame = Path().apply {
                moveTo(w * .17f, h * .24f)
                cubicTo(w * .36f, h * .23f, w * .63f, h * .25f, w * .82f, h * .24f)
                lineTo(w * .83f, h * .73f)
                quadraticTo(w * .48f, h * .75f, w * .18f, h * .73f)
                close()
            }
            drawPath(frame, ink, style = pen)
            drawCircle(ink, radius = w * .065f, center = Offset(w * .63f, h * .37f), style = pen)
            val hills = Path().apply {
                moveTo(w * .25f, h * .62f)
                lineTo(w * .39f, h * .45f)
                quadraticTo(w * .42f, h * .43f, w * .45f, h * .48f)
                lineTo(w * .55f, h * .58f)
                lineTo(w * .64f, h * .51f)
                lineTo(w * .76f, h * .62f)
            }
            drawPath(hills, ink, style = pen)
            line(.26f, .67f, .74f, .67f)
        }
        line(.10f, .12f, .10f, .20f)
        line(.05f, .16f, .15f, .16f)
        line(.91f, .73f, .91f, .83f)
        line(.85f, .78f, .97f, .78f)
        line(.38f, .84f, .52f, .86f)
        drawCircle(ink, radius = 1.8.dp.toPx(), center = Offset(w * .84f, h * .14f))
        drawCircle(ink, radius = 1.4.dp.toPx(), center = Offset(w * .16f, h * .83f))
    }
}
