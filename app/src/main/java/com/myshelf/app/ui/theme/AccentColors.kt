package com.myshelf.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.myshelf.app.core.model.Accent

val Accent.swatch: Color get() = when (this) {
    Accent.ROSE -> Color(0xFFBD7A9B)
    Accent.LAVENDER -> Color(0xFF9E8BEA)
    Accent.LEAF -> Color(0xFF79C478)
    Accent.TEAL -> Color(0xFF5BBDBD)
    Accent.GOLD -> Color(0xFFF0C448)
    Accent.SLATE -> Color(0xFF6D80A6)
}

val Accent.light: Color get() = when (this) {
    Accent.ROSE -> Color(0xFF944366)
    Accent.LAVENDER -> Color(0xFF6550AA)
    Accent.LEAF -> Color(0xFF3B713D)
    Accent.TEAL -> Color(0xFF216F70)
    Accent.GOLD -> Color(0xFF7D6017)
    Accent.SLATE -> Color(0xFF4B6086)
}
