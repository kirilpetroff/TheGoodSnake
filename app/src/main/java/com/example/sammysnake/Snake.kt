package com.example.sammysnake

/** Тялото на змията - първият сегмент в списъка е главата, а последният е опашката. */
class Snake(initialBody: List<Cell>, initialDirection: Direction) {

    var direction: Direction = initialDirection
        private set

    private val _segments = initialBody.map { Segment(it) }.toMutableList()
    val segments: List<Segment> get() = _segments
    val body: List<Cell> get() = _segments.map { it.cell }

    fun changeDirection(newDirection: Direction) {
        val isOppositeDirection = newDirection.dCol == -direction.dCol && newDirection.dRow == -direction.dRow
        if (!isOppositeDirection) {
            direction = newDirection
        }
    }

    fun nextHead(): Cell {
        val head = _segments.first().cell
        return Cell(head.col + direction.dCol, head.row + direction.dRow)
    }

    /** Опашката се мести на всяка стъпка, затова не се брои за сблъсък. */
    fun willHitSelf(next: Cell): Boolean = _segments.dropLast(1).any { it.cell == next }

    fun move(grow: Boolean = false) {
        _segments.add(0, Segment(nextHead()))
        if (!grow) {
            _segments.removeAt(_segments.size - 1)
        }
    }

    /**
     * Извиква се веднага след move(grow = true) при златна ябълка: сегментът до главата
     * (старата глава, вече избутана на позиция 1) става златен, а [tailRemoveCount]
     * стандартни сегмента се махат от опашката.
     */
    fun applyGoldenBonus(tailRemoveCount: Int) {
        if (_segments.size > 1) {
            _segments[1] = _segments[1].copy(golden = true)
        }
        repeat(tailRemoveCount) {
            if (_segments.size > 2) {
                _segments.removeAt(_segments.size - 1)
            }
        }
    }

    /** Връща тялото към зададено състояние - използва се, за да "пренаправим" последната стъпка. */
    fun setBody(newSegments: List<Segment>) {
        _segments.clear()
        _segments.addAll(newSegments)
    }
}
