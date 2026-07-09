package com.example.sammysnake

import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val musicVolume = 10.0.pow(-12.0 / 20.0).toFloat() // -12dB спрямо пълна сила
    private val startingIntervalMs = 350L
    private val minIntervalMs = 100L
    private val speedUpStepMs = 10L
    private val flashStepMs = 150L
    private val flashCount = 3
    private val turnForgivenessThreshold = 0.2f // до 1/5 от хода - завоят се "връща" в предната клетка
    private val startingLives = 3
    private val pointsPerApple = 100
    private val pointsPerGoldenApple = 500
    private val goldenTailRemoveCount = 4
    private val goldenAppleChanceDenominator = 8 // 1 от 8 ябълки е златна
    private val minSnakeLengthForGoldenApple = 5 // поне 4 стандартни сегмента извън главата

    private var moveIntervalMs = startingIntervalMs
    private var intervalBeforeLastTick = startingIntervalMs

    private val handler = Handler(Looper.getMainLooper())
    private val choreographer = Choreographer.getInstance()

    private lateinit var snake: Snake
    private lateinit var food: Food
    private lateinit var gameGridView: GameGridView
    private lateinit var deathFlashOverlay: View
    private lateinit var gameOverText: TextView
    private lateinit var scoreText: TextView
    private lateinit var livesText: TextView
    private lateinit var pauseButton: Button
    private lateinit var settingsButton: Button
    private lateinit var controlsArea: View
    private lateinit var dpadButtons: List<View>
    private lateinit var swipeHints: List<View>
    private lateinit var backgroundMusicPlayer: MediaPlayer
    private lateinit var soundPool: SoundPool
    private var eatSoundId = 0
    private var deathSoundId = 0

    private var isGameOver = false
    private var isPaused = false
    private var isLoopRunning = false
    private var pendingFoodRespawn = false
    private var hasRedirectedThisTick = false
    private var score = 0
    private var lives = startingLives
    private var controlType = ControlType.BUTTONS
    private var snakeColorIndex = 0
    private var swipeStartX = 0f
    private var swipeStartY = 0f

    // Едно и също време управлява кога змията прави следваща стъпка И колко плавно се
    // изчертава преходът към нея - затова логиката и анимацията никога не се разминават.
    private var lastTickTimeNanos = 0L
    private var previousSegments: List<Segment> = emptyList()

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isLoopRunning) return

            val intervalNanos = moveIntervalMs * 1_000_000L
            if (frameTimeNanos - lastTickTimeNanos >= intervalNanos) {
                lastTickTimeNanos += intervalNanos
                performTick()
            }

            if (isLoopRunning) {
                val progress = (frameTimeNanos - lastTickTimeNanos).toFloat() / intervalNanos
                gameGridView.setMoveState(previousSegments, snake.segments, progress, snake.direction)
                choreographer.postFrameCallback(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applyLanguage(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        controlType = AppPreferences.getControlType(this)
        snakeColorIndex = AppPreferences.getSnakeColorIndex(this)

        val root = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        root.setOnClickListener {
            if (isGameOver) restartGame()
        }

        gameGridView = findViewById(R.id.gameGrid)
        applySnakeColor()
        deathFlashOverlay = findViewById(R.id.deathFlashOverlay)
        gameOverText = findViewById(R.id.gameOverText)
        scoreText = findViewById(R.id.scoreText)
        livesText = findViewById(R.id.livesText)
        pauseButton = findViewById(R.id.pauseButton)
        pauseButton.setOnClickListener { togglePause() }
        settingsButton = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener { showSettingsDialog() }
        controlsArea = findViewById(R.id.controlsArea)
        dpadButtons = listOf(
            findViewById(R.id.btnUp),
            findViewById(R.id.btnDown),
            findViewById(R.id.btnLeft),
            findViewById(R.id.btnRight)
        )
        swipeHints = listOf(
            findViewById(R.id.swipeHintUp),
            findViewById(R.id.swipeHintDown),
            findViewById(R.id.swipeHintLeft),
            findViewById(R.id.swipeHintRight)
        )
        applyControlType()

        gameGridView.setOnTouchListener(swipeTouchListener)
        controlsArea.setOnTouchListener(swipeTouchListener)

        backgroundMusicPlayer = MediaPlayer.create(this, R.raw.three_lives_remaining).apply {
            isLooping = true
            setVolume(musicVolume, musicVolume)
        }

        val soundAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(soundAttributes)
            .build()
        eatSoundId = soundPool.load(this, R.raw.eat_apple, 1)
        deathSoundId = soundPool.load(this, R.raw.death_crash, 1)

        findViewById<Button>(R.id.btnUp).setOnClickListener { if (!isGameOver) onDirectionButtonPressed(Direction.UP) }
        findViewById<Button>(R.id.btnDown).setOnClickListener { if (!isGameOver) onDirectionButtonPressed(Direction.DOWN) }
        findViewById<Button>(R.id.btnLeft).setOnClickListener { if (!isGameOver) onDirectionButtonPressed(Direction.LEFT) }
        findViewById<Button>(R.id.btnRight).setOnClickListener { if (!isGameOver) onDirectionButtonPressed(Direction.RIGHT) }

        startNewGame()
    }

    override fun onResume() {
        super.onResume()
        if (!isGameOver && !isPaused) {
            startLoop()
        }
    }

    /** Обща логика за плъзгане - закачена и за игровото поле, и за полето с бутоните. */
    private val swipeTouchListener = View.OnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                swipeStartX = event.x
                swipeStartY = event.y
                true // трябва да "поемем" ACTION_DOWN, иначе не получаваме следващия ACTION_UP
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - swipeStartX
                val dy = event.y - swipeStartY
                val swipeThresholdPx = 24 * resources.displayMetrics.density
                val distance = maxOf(abs(dx), abs(dy))
                if (isGameOver) {
                    if (distance < swipeThresholdPx) restartGame()
                } else if (controlType == ControlType.SWIPE && distance >= swipeThresholdPx) {
                    val direction = if (abs(dx) > abs(dy)) {
                        if (dx > 0) Direction.RIGHT else Direction.LEFT
                    } else {
                        if (dy > 0) Direction.DOWN else Direction.UP
                    }
                    onDirectionButtonPressed(direction)
                }
                true
            }
            else -> false
        }
    }

    private fun applyControlType() {
        val isSwipe = controlType == ControlType.SWIPE
        dpadButtons.forEach { it.visibility = if (isSwipe) View.GONE else View.VISIBLE }
        swipeHints.forEach { it.visibility = if (isSwipe) View.VISIBLE else View.GONE }
    }

    private fun applySnakeColor() {
        val (bodyColorRes, headColorRes) = SnakeColorPalette.presets[snakeColorIndex]
        gameGridView.setSnakeColors(
            ContextCompat.getColor(this, bodyColorRes),
            ContextCompat.getColor(this, headColorRes)
        )
    }

    private fun showSettingsDialog() {
        val wasPausedBeforeDialog = isPaused
        if (!isGameOver && !isPaused) {
            togglePause()
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.setOnDismissListener {
            if (!isGameOver && !wasPausedBeforeDialog && isPaused) {
                togglePause()
            }
        }

        val rgControlType = dialogView.findViewById<RadioGroup>(R.id.rgControlType)
        val rbControlButtons = dialogView.findViewById<RadioButton>(R.id.rbControlButtons)
        val rbControlSwipe = dialogView.findViewById<RadioButton>(R.id.rbControlSwipe)
        if (controlType == ControlType.SWIPE) rbControlSwipe.isChecked = true else rbControlButtons.isChecked = true
        rgControlType.setOnCheckedChangeListener { _, checkedId ->
            controlType = if (checkedId == R.id.rbControlSwipe) ControlType.SWIPE else ControlType.BUTTONS
            AppPreferences.setControlType(this, controlType)
            applyControlType()
        }

        val rgLanguage = dialogView.findViewById<RadioGroup>(R.id.rgLanguage)
        val rbLangBg = dialogView.findViewById<RadioButton>(R.id.rbLangBg)
        val rbLangEn = dialogView.findViewById<RadioButton>(R.id.rbLangEn)
        if (AppPreferences.getLanguage(this) == "en") rbLangEn.isChecked = true else rbLangBg.isChecked = true
        rgLanguage.setOnCheckedChangeListener { _, checkedId ->
            val languageTag = if (checkedId == R.id.rbLangEn) "en" else "bg"
            AppPreferences.setLanguage(this, languageTag)
        }

        val swatchContainer = dialogView.findViewById<LinearLayout>(R.id.colorSwatchContainer)
        val swatchViews = mutableListOf<View>()
        val density = resources.displayMetrics.density
        val swatchSizePx = (40 * density).toInt()
        val swatchMarginPx = (8 * density).toInt()

        fun updateSwatchSelection() {
            swatchViews.forEachIndexed { index, swatch ->
                val strokeWidthDp = if (index == snakeColorIndex) 4 else 2
                val strokeColorRes = if (index == snakeColorIndex) R.color.title_text else R.color.white
                (swatch.background as GradientDrawable).setStroke(
                    (strokeWidthDp * density).toInt(),
                    ContextCompat.getColor(this, strokeColorRes)
                )
            }
        }

        SnakeColorPalette.presets.forEachIndexed { index, (bodyColorRes, _) ->
            val swatch = View(this)
            val params = LinearLayout.LayoutParams(swatchSizePx, swatchSizePx)
            params.setMargins(swatchMarginPx, 0, swatchMarginPx, 0)
            swatch.layoutParams = params
            swatch.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(this@MainActivity, bodyColorRes))
            }
            swatch.setOnClickListener {
                snakeColorIndex = index
                AppPreferences.setSnakeColorIndex(this, index)
                applySnakeColor()
                updateSwatchSelection()
            }
            swatchContainer.addView(swatch)
            swatchViews.add(swatch)
        }
        updateSwatchSelection()

        dialogView.findViewById<Button>(R.id.btnCloseSettings).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun togglePause() {
        if (isGameOver) return
        isPaused = !isPaused
        if (isPaused) {
            stopLoop()
            pauseButton.text = getString(R.string.resume_symbol)
            pauseButton.contentDescription = getString(R.string.resume_button_description)
        } else {
            startLoop()
            pauseButton.text = getString(R.string.pause_symbol)
            pauseButton.contentDescription = getString(R.string.pause_button_description)
        }
    }

    override fun onPause() {
        super.onPause()
        stopLoop()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundMusicPlayer.release()
        soundPool.release()
    }

    private fun startLoop() {
        if (!isLoopRunning) {
            isLoopRunning = true
            lastTickTimeNanos = System.nanoTime()
            choreographer.postFrameCallback(frameCallback)
        }
        if (!backgroundMusicPlayer.isPlaying) {
            backgroundMusicPlayer.start()
        }
    }

    private fun stopLoop() {
        isLoopRunning = false
        choreographer.removeFrameCallback(frameCallback)
        if (backgroundMusicPlayer.isPlaying) {
            backgroundMusicPlayer.pause()
        }
    }

    private fun performTick() {
        val nextHead = snake.nextHead()
        val hitWall = nextHead.col !in 0 until GameGridView.COLUMNS || nextHead.row !in 0 until GameGridView.ROWS
        val hitSelf = snake.willHitSelf(nextHead)

        if (hitWall || hitSelf) {
            onSnakeDied()
            return
        }

        // Ябълката от предходния ход се маха едва сега - точно когато змията вече я е
        // изяла визуално - за да не изчезва предварително по средата на плъзгането.
        if (pendingFoodRespawn) {
            food = spawnFood()
            gameGridView.setFoodCell(food)
            pendingFoodRespawn = false
        }

        hasRedirectedThisTick = false
        intervalBeforeLastTick = moveIntervalMs
        previousSegments = snake.segments.toList()
        val ateFood = nextHead == food.cell
        snake.move(grow = ateFood)

        if (ateFood) {
            handleFoodEaten(food.golden)
            // Веднъж изядена ябълката (и евентуално слят златен сегмент) за този ход,
            // не позволяваме на закъсняло пренасочване да го "отмени", като върне змията
            // към състоянието преди хапването.
            hasRedirectedThisTick = true
        }
    }

    private fun currentMoveProgress(): Float {
        val intervalNanos = moveIntervalMs * 1_000_000L
        return ((System.nanoTime() - lastTickTimeNanos).toFloat() / intervalNanos).coerceIn(0f, 1f)
    }

    private fun onDirectionButtonPressed(newDirection: Direction) {
        if (currentMoveProgress() <= turnForgivenessThreshold && !hasRedirectedThisTick) {
            redirectCurrentMove(newDirection)
        } else {
            snake.changeDirection(newDirection)
        }
    }

    /**
     * Ако змията едва е тръгнала към новата клетка (до 1/5 от хода), приемаме, че завоят е
     * закъснял съвсем малко, и го прилагаме все едно се е случил в предната клетка -
     * вместо играчът да чака цял ход повече, докато завоят влезе в сила. Позволяваме само
     * едно пренасочване на ход - следващо натискане в същия прозорец само сменя посоката за
     * следващия ход, иначе би презаписало резултата от първото пренасочване (напр. златен
     * сегмент от току-що изядена ябълка).
     */
    private fun redirectCurrentMove(newDirection: Direction) {
        hasRedirectedThisTick = true
        moveIntervalMs = intervalBeforeLastTick
        pendingFoodRespawn = false

        snake.setBody(previousSegments)
        snake.changeDirection(newDirection)

        val nextHead = snake.nextHead()
        val hitWall = nextHead.col !in 0 until GameGridView.COLUMNS || nextHead.row !in 0 until GameGridView.ROWS
        val hitSelf = snake.willHitSelf(nextHead)
        if (hitWall || hitSelf) {
            onSnakeDied()
            return
        }

        val ateFood = nextHead == food.cell
        snake.move(grow = ateFood)
        if (ateFood) {
            handleFoodEaten(food.golden)
        }
    }

    /** Златната ябълка маркира сегмента до главата като златен и маха [goldenTailRemoveCount] от опашката. */
    private fun handleFoodEaten(golden: Boolean) {
        if (golden) {
            snake.applyGoldenBonus(goldenTailRemoveCount)
            addScore(pointsPerGoldenApple)
        } else {
            addScore(pointsPerApple)
        }
        moveIntervalMs = maxOf(moveIntervalMs - speedUpStepMs, minIntervalMs)
        pendingFoodRespawn = true
    }

    private fun addScore(points: Int) {
        score += points
        updateScoreText()
        soundPool.play(eatSoundId, 1f, 1f, 0, 0, 1f)
    }

    private fun updateScoreText() {
        scoreText.text = getString(R.string.score_format, score)
    }

    private fun updateLivesText() {
        livesText.text = getString(R.string.heart).repeat(lives.coerceAtLeast(0))
    }

    private fun buildInitialBody(): List<Cell> {
        val middleColumn = GameGridView.COLUMNS / 2
        return listOf(
            Cell(middleColumn, 4), // главата - ред 5
            Cell(middleColumn, 3),
            Cell(middleColumn, 2),
            Cell(middleColumn, 1),
            Cell(middleColumn, 0)  // опашката - ред 1
        )
    }

    private fun spawnFood(): Food {
        val emptyCells = mutableListOf<Cell>()
        for (col in 0 until GameGridView.COLUMNS) {
            for (row in 0 until GameGridView.ROWS) {
                val cell = Cell(col, row)
                if (cell !in snake.body) emptyCells.add(cell)
            }
        }
        val golden = snake.body.size >= minSnakeLengthForGoldenApple &&
            Random.nextInt(goldenAppleChanceDenominator) == 0
        return Food(emptyCells.random(), golden)
    }

    /** Пресъздава змията и ябълката на начална позиция, без да пипа точките и животите. */
    private fun resetSnakeAndFood() {
        moveIntervalMs = startingIntervalMs
        intervalBeforeLastTick = startingIntervalMs
        pendingFoodRespawn = false
        hasRedirectedThisTick = false
        snake = Snake(buildInitialBody(), Direction.DOWN)
        previousSegments = snake.segments.toList()
        gameGridView.setSnakeCells(snake.segments, snake.direction)
        food = spawnFood()
        gameGridView.setFoodCell(food)
    }

    private fun startNewGame() {
        isGameOver = false
        isPaused = false
        pauseButton.text = getString(R.string.pause_symbol)
        pauseButton.contentDescription = getString(R.string.pause_button_description)
        score = 0
        lives = startingLives
        updateScoreText()
        updateLivesText()
        deathFlashOverlay.visibility = View.INVISIBLE
        gameOverText.visibility = View.GONE
        resetSnakeAndFood()
        startLoop()
    }

    private fun restartGame() {
        startNewGame()
    }

    private fun onSnakeDied() {
        stopLoop()
        soundPool.play(deathSoundId, 1f, 1f, 0, 0, 1f)
        lives--
        updateLivesText()
        val outOfLives = lives <= 0
        if (outOfLives) {
            isGameOver = true
        }

        val flashRunnable = object : Runnable {
            var isVisible = false
            var completedFlashes = 0

            override fun run() {
                isVisible = !isVisible
                deathFlashOverlay.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
                if (!isVisible) completedFlashes++

                if (completedFlashes < flashCount) {
                    handler.postDelayed(this, flashStepMs)
                } else if (outOfLives) {
                    gameOverText.visibility = View.VISIBLE
                } else {
                    resetSnakeAndFood()
                    startLoop()
                }
            }
        }
        handler.post(flashRunnable)
    }
}
