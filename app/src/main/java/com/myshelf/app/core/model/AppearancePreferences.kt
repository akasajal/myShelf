package com.myshelf.app.core.model

enum class ThemeMode(val label: String) { SYSTEM("System"), DARK("Dark"), LIGHT("Light") }

enum class Accent(val label: String) {
    ROSE("Rose"),
    LAVENDER("Lavender"),
    LEAF("Leaf"),
    TEAL("Teal"),
    GOLD("Gold"),
    SLATE("Slate")
}

enum class CardDetails(val label: String) { COMFORTABLE("Comfortable"), COMPACT("Compact") }
