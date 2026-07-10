package com.example.sammysnake

/** Тялото на змията - първият сегмент в списъка е главата, а последният е опашката. */
class Snake(initialBody: List<Cell>, initialDirection: Direction) {

    var direction: Direction = initialDirection
        private set

    /** Брой сегменти зад главата, които трябва да са златни - расте трайно, никога не намалява. */
    private var goldenSegmentCount = 0

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
        refreshGoldenFlags()
    }

    /**
     * Извиква се веднага след move(grow = true) при златна ябълка: златният "префикс" зад
     * главата расте трайно с 1 сегмент (никога не се отдалечава към опашката), а
     * [tailRemoveCount] стандартни сегмента се махат от опашката, за да не расте безкрайно.
     */
    fun applyGoldenBonus(tailRemoveCount: Int) {
        goldenSegmentCount++
        repeat(tailRemoveCount) {
            if (_segments.size > 2) {
                _segments.removeAt(_segments.size - 1)
            }
        }
        refreshGoldenFlags()
    }

    /** Връща тялото към зададено състояние - използва се, за да "пренаправим" последната стъпка. */
    fun setBody(newSegments: List<Segment>) {
        _segments.clear()
        _segments.addAll(newSegments)
        refreshGoldenFlags()
    }

    /**
     * "Златно" не е свойство на конкретен физически сегмент, а на позицията му спрямо главата -
     * винаги първите [goldenSegmentCount] сегмента зад главата (индекси 1..goldenSegmentCount)
     * са златни. Така златната отсечка винаги следва главата и никога не изчезва към опашката.
     */
    private fun refreshGoldenFlags() {
        for (i in _segments.indices) {
            val shouldBeGolden = i in 1..goldenSegmentCount
            if (_segments[i].golden != shouldBeGolden) {
                _segments[i] = _segments[i].copy(golden = shouldBeGolden)
            }
        }
    }
}
