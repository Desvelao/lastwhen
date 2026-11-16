package app.lasttime.data

/** Swatches offered for events and categories (ARGB). Stored as ints so palettes can change later. */
object Palette {
    val colors: List<Int> = listOf(
        0xFF4F8EF7.toInt(), 0xFF2BB673.toInt(), 0xFFF2A20C.toInt(), 0xFFE5534B.toInt(),
        0xFF9B6BE8.toInt(), 0xFF14A9B8.toInt(), 0xFFE86FA6.toInt(), 0xFF8C8F98.toInt(),
    )
    val default: Int get() = colors.first()
}
