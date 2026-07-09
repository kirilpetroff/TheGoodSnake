package com.example.sammysnake

/** Едно поле от тялото на змията - злато отбелязва сегмент, получен от златна ябълка. */
data class Segment(val cell: Cell, val golden: Boolean = false)
