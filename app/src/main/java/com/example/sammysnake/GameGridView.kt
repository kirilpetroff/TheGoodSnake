package com.example.sammysnake

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils

/** Мрежа от квадратчета, върху която рисуваме змията - помага за ориентация, докато пишем играта. */
class GameGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val COLUMNS = 19
        const val ROWS = 19
    }

    private val gridPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.grid_line)
        strokeWidth = 1f
    }

    private val snakeBodyPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.snake_body)
    }

    private val snakeHeadPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.snake_head)
    }

    private val snakeGoldBodyPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.snake_gold_body)
    }

    private val snakeGoldHeadPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.snake_gold_head)
    }

    /** Използва се за плавно преливане на цвета на слот, когато статусът му "златно" се смени този ход. */
    private val blendedSegmentPaint = Paint()

    private val eyePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.black)
    }

    private val tonguePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.snake_tongue)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val foodPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.food)
    }

    private val goldFoodPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.food_gold)
    }

    private val foodStemPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.food_stem)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val foodLeafPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.food_leaf)
    }

    private var previousSnakeSegments: List<Segment> = emptyList()
    private var currentSnakeSegments: List<Segment> = emptyList()
    private var moveProgress = 1f
    private var headDirection: Direction = Direction.DOWN

    private var food: Food? = null

    /** Поставя змията веднага, без анимация (за начало на игра/рестарт). */
    fun setSnakeCells(segments: List<Segment>, direction: Direction = Direction.DOWN) {
        previousSnakeSegments = segments
        currentSnakeSegments = segments
        moveProgress = 1f
        headDirection = direction
        invalidate()
    }

    /**
     * Обновява откъде-докъде се плъзга всеки сегмент и колко напреднал е ходът (0..1).
     * Извиква се на всеки кадър от единствения игров цикъл (виж MainActivity), за да няма
     * разминаване между часовника на логиката и часовника на анимацията.
     */
    fun setMoveState(previous: List<Segment>, current: List<Segment>, progress: Float, direction: Direction) {
        previousSnakeSegments = previous
        currentSnakeSegments = current
        moveProgress = progress.coerceIn(0f, 1f)
        headDirection = direction
        invalidate()
    }

    fun setFoodCell(food: Food?) {
        this.food = food
        invalidate()
    }

    fun setSnakeColors(bodyColor: Int, headColor: Int) {
        snakeBodyPaint.color = bodyColor
        snakeHeadPaint.color = headColor
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cellWidth = width.toFloat() / COLUMNS
        val cellHeight = height.toFloat() / ROWS
        val cellSize = minOf(cellWidth, cellHeight)

        currentSnakeSegments.forEachIndexed { index, segment ->
            // Ново сегментче от опашката (при растеж) няма предишна позиция - остава на място.
            val sourceCell = if (index < previousSnakeSegments.size) previousSnakeSegments[index].cell else segment.cell
            val cell = segment.cell
            val col = sourceCell.col + (cell.col - sourceCell.col) * moveProgress
            val row = sourceCell.row + (cell.row - sourceCell.row) * moveProgress

            val left = col * cellWidth
            val top = row * cellHeight
            val right = left + cellWidth
            val bottom = top + cellHeight

            if (index == 0) {
                drawHead(canvas, left, top, right, bottom, cellSize, segment.golden)
            } else {
                val cornerRadius = cellSize * 0.25f
                // Този слот показваше друг сегмент предния ход - ако статусът "златно" се е
                // сменил междувременно, преливаме плавно цвета вместо да го прещракваме рязко.
                val wasGolden = if (index < previousSnakeSegments.size) previousSnakeSegments[index].golden else segment.golden
                val paint = if (wasGolden == segment.golden) {
                    if (segment.golden) snakeGoldBodyPaint else snakeBodyPaint
                } else {
                    val fromColor = if (wasGolden) snakeGoldBodyPaint.color else snakeBodyPaint.color
                    val toColor = if (segment.golden) snakeGoldBodyPaint.color else snakeBodyPaint.color
                    blendedSegmentPaint.color = ColorUtils.blendARGB(fromColor, toColor, moveProgress)
                    blendedSegmentPaint
                }
                canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, paint)
            }
        }

        food?.let { food ->
            drawApple(canvas, food, cellWidth, cellHeight, cellSize)
        }

        for (col in 0..COLUMNS) {
            val x = col * cellWidth
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
        }
        for (row in 0..ROWS) {
            val y = row * cellHeight
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }
    }

    private fun drawApple(canvas: Canvas, food: Food, cellWidth: Float, cellHeight: Float, cellSize: Float) {
        val cell = food.cell
        val centerX = cell.col * cellWidth + cellWidth / 2
        val centerY = cell.row * cellHeight + cellHeight / 2

        val bodyRadius = cellSize * 0.32f
        val bodyCenterY = centerY + cellSize * 0.08f
        canvas.drawCircle(centerX, bodyCenterY, bodyRadius, if (food.golden) goldFoodPaint else foodPaint)

        // дръжка
        foodStemPaint.strokeWidth = cellSize * 0.06f
        val stemBottom = bodyCenterY - bodyRadius * 0.7f
        val stemTop = bodyCenterY - bodyRadius - cellSize * 0.14f
        canvas.drawLine(centerX, stemBottom, centerX, stemTop, foodStemPaint)

        // листенце
        val leafCenterX = centerX + cellSize * 0.13f
        val leafCenterY = stemTop + cellSize * 0.05f
        canvas.save()
        canvas.rotate(35f, leafCenterX, leafCenterY)
        canvas.drawOval(
            leafCenterX - cellSize * 0.13f, leafCenterY - cellSize * 0.07f,
            leafCenterX + cellSize * 0.13f, leafCenterY + cellSize * 0.07f,
            foodLeafPaint
        )
        canvas.restore()
    }

    private fun drawHead(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, cellSize: Float, golden: Boolean) {
        val cornerRadius = cellSize * 0.4f
        canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, if (golden) snakeGoldHeadPaint else snakeHeadPaint)

        // Посоката на движение като вектор "напред" и перпендикулярният му вектор "встрани".
        val forwardX = headDirection.dCol.toFloat()
        val forwardY = headDirection.dRow.toFloat()
        val sideX = -forwardY
        val sideY = forwardX

        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2

        val eyeForwardOffset = cellSize * 0.10f
        val eyeSideOffset = cellSize * 0.22f
        val eyeSize = cellSize * 0.16f

        val eyeBaseX = centerX + forwardX * eyeForwardOffset
        val eyeBaseY = centerY + forwardY * eyeForwardOffset

        for (sign in floatArrayOf(1f, -1f)) {
            val eyeX = eyeBaseX + sideX * eyeSideOffset * sign
            val eyeY = eyeBaseY + sideY * eyeSideOffset * sign
            canvas.drawRect(
                eyeX - eyeSize / 2, eyeY - eyeSize / 2,
                eyeX + eyeSize / 2, eyeY + eyeSize / 2,
                eyePaint
            )
        }

        // Разцепено езиче, което се показва от предния край на главата.
        tonguePaint.strokeWidth = cellSize * 0.05f
        val tongueBaseX = centerX + forwardX * (cellSize * 0.5f)
        val tongueBaseY = centerY + forwardY * (cellSize * 0.5f)
        val tongueTipX = centerX + forwardX * (cellSize * 0.8f)
        val tongueTipY = centerY + forwardY * (cellSize * 0.8f)
        canvas.drawLine(tongueBaseX, tongueBaseY, tongueTipX, tongueTipY, tonguePaint)

        val forkLength = cellSize * 0.15f
        val forkSpread = cellSize * 0.1f
        canvas.drawLine(
            tongueTipX, tongueTipY,
            tongueTipX + forwardX * forkLength + sideX * forkSpread,
            tongueTipY + forwardY * forkLength + sideY * forkSpread,
            tonguePaint
        )
        canvas.drawLine(
            tongueTipX, tongueTipY,
            tongueTipX + forwardX * forkLength - sideX * forkSpread,
            tongueTipY + forwardY * forkLength - sideY * forkSpread,
            tonguePaint
        )
    }
}
