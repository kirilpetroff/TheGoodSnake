package com.example.sammysnake

/** Възможните цветове на змията - всеки чифт е (цвят на тялото, цвят на главата). */
object SnakeColorPalette {
    val presets: List<Pair<Int, Int>> = listOf(
        R.color.snake_green_body to R.color.snake_green_head,
        R.color.snake_blue_body to R.color.snake_blue_head,
        R.color.snake_orange_body to R.color.snake_orange_head,
        R.color.snake_purple_body to R.color.snake_purple_head,
        R.color.snake_red_body to R.color.snake_red_head
    )
}
