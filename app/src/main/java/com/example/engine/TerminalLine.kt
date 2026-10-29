package com.example.engine

/** One line in the terminal window, tagged with how it should be coloured. */
data class TerminalLine(
    val text: String,
    val type: LineType
) {
    enum class LineType { INPUT, OUTPUT, ERROR, SUCCESS, SYSTEM, ACCENT }
}
