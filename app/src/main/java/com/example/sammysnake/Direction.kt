package com.example.sammysnake

enum class Direction(val dCol: Int, val dRow: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0)
}
