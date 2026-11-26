package com.classicaltiles.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private enum class Screen { MENU, GAME, PAUSED, RESULT }
private enum class TileState { PENDING, HIT, MISSED }

private data class TileEvent(
    val midi: Int,
    val lane: Int,
    val targetSeconds: Float,
    var state: TileState = TileState.PENDING
)

private class GameSession(
    val song: Song,
    val events: List<TileEvent>,
    val speedPxPerSecond: Float,
    val fallSeconds: Float,
    startedAt: Long
) {
    private var startedAtMs = startedAt
    private var frozenAtMs: Long? = null
    private var frozenElapsed = 0f

    var score = 0
    var hits = 0
    var misses = 0
    var combo = 0
    var bestCombo = 0
    var perfects = 0
    var goods = 0

    fun elapsed(now: Long): Float {
        return if (frozenAtMs != null) frozenElapsed else (now - startedAtMs) / 1000f
    }

    fun pause(now: Long) {
        if (frozenAtMs == null) {
            frozenElapsed = (now - startedAtMs) / 1000f
            frozenAtMs = now
        }
    }

    fun resume(now: Long) {
        if (frozenAtMs != null) {
            startedAtMs = now - (frozenElapsed * 1000L).toLong()
            frozenAtMs = null
        }
    }

    fun remaining(): Int = events.count { it.state == TileState.PENDING }
}

private class PianoAudio(context: Context) {
    private val soundPool: SoundPool
    private val sounds = HashMap<Int, Int>()

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attributes)
            .build()
        // Each short sample is a locally generated piano-like tone; no recording is used.
        for (midi in 45..84) {
            val resourceId = context.resources.getIdentifier("p$midi", "raw", context.packageName)
            if (resourceId != 0) sounds[midi] = soundPool.load(context, resourceId, 1)
        }
    }

    fun play(midi: Int) {
        val sound = sounds[midi] ?: return
        soundPool.play(sound, 0.92f, 0.92f, 1, 0, 1f)
    }

    fun release() = soundPool.release()
}

class PianoTilesView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private fun dp(value: Float) = value * density

    private val bg = Color.rgb(11, 16, 32)
    private val panel = Color.rgb(20, 29, 51)
    private val panel2 = Color.rgb(26, 38, 64)
    private val mint = Color.rgb(112, 226, 206)
    private val gold = Color.rgb(255, 204, 112)
    private val white = Color.rgb(244, 247, 252)
    private val muted = Color.rgb(153, 168, 192)
    private val red = Color.rgb(255, 112, 130)
    private val laneColors = intArrayOf(
        Color.rgb(91, 205, 195),
        Color.rgb(111, 160, 245),
        Color.rgb(190, 145, 242),
        Color.rgb(245, 184, 99)
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val audio = PianoAudio(context)
    private val prefs = context.getSharedPreferences("classical_tiles", Context.MODE_PRIVATE)
    private val songs = ClassicalTracks.all

    private var screen = Screen.MENU
    private var selectedIndex = 0
    private var session: GameSession? = null
    private var menuScroll = 0f
    private var downX = 0f
    private var downY = 0f
    private var consumeNextUp = false
    private var feedback = ""
    private var feedbackUntil = 0L
    private var lastResultWasCleared = false

    private val menuListTop get() = dp(224f)
    private val menuRowHeight get() = dp(76f)
    private val menuBottom get() = height.toFloat() - dp(48f)

    init {
        isFocusable = true
        isClickable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(bg)
        when (screen) {
            Screen.MENU -> drawMenu(canvas)
            Screen.GAME -> {
                updateGame()
                if (screen == Screen.RESULT) drawResult(canvas) else {
                    drawGame(canvas)
                    postInvalidateOnAnimation()
                }
            }
            Screen.PAUSED -> {
                drawGame(canvas)
                drawPauseOverlay(canvas)
            }
            Screen.RESULT -> drawResult(canvas)
        }
    }

    private fun drawMenu(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        drawText(canvas, "A CLASSICAL RHYTHM GAME", dp(22f), dp(34f), dp(10f), mint, true)
        drawText(canvas, "CLASSICAL", dp(22f), dp(78f), dp(35f), white, true)
        drawText(canvas, "TILES", dp(22f), dp(116f), dp(35f), gold, true)
        drawText(canvas, "Play the greats, one tile at a time.", dp(23f), dp(143f), dp(13f), muted)

        val badge = RectF(dp(22f), dp(164f), min(w - dp(22f), dp(264f)), dp(201f))
        roundRect(canvas, badge, panel2, dp(18f))
        drawText(canvas, "15 PUBLIC-DOMAIN CLASSICS", badge.left + dp(13f), badge.top + dp(23f), dp(10f), mint, true)
        drawText(canvas, "SONG LIBRARY", dp(22f), dp(217f), dp(10f), muted, true)

        val visibleHeight = max(0f, menuBottom - menuListTop)
        val maxScroll = max(0f, songs.size * menuRowHeight - visibleHeight)
        menuScroll = menuScroll.coerceIn(0f, maxScroll)
        canvas.save()
        canvas.clipRect(0f, menuListTop, w, menuBottom)

        songs.forEachIndexed { index, song ->
            val top = menuListTop + index * menuRowHeight - menuScroll
            val rect = RectF(dp(16f), top + dp(4f), w - dp(16f), top + menuRowHeight - dp(4f))
            if (rect.bottom >= menuListTop && rect.top <= menuBottom) {
                roundRect(canvas, rect, panel, dp(14f))
                val numberX = rect.left + dp(27f)
                paint.color = laneColors[index % laneColors.size]
                canvas.drawCircle(numberX, rect.centerY(), dp(17f), paint)
                drawTextCentered(canvas, "%02d".format(index + 1), numberX, rect.centerY() + dp(4f), dp(10f), bg, true)

                val titleMax = rect.width() - dp(112f)
                drawTextFit(canvas, song.title, rect.left + dp(56f), rect.top + dp(31f), titleMax, dp(14f), white, true)
                drawTextFit(canvas, "${song.composer}  •  ${song.bpm} BPM", rect.left + dp(56f), rect.top + dp(52f), titleMax, dp(10f), muted)
                drawTextCentered(canvas, "›", rect.right - dp(21f), rect.centerY() + dp(7f), dp(26f), muted, false)
            }
        }
        canvas.restore()

        drawTextCentered(canvas, "OFFLINE  •  SYNTHESIZED PIANO  •  NO RECORDINGS", w / 2f, h - dp(18f), dp(8f), muted, true)
    }

    private fun drawGame(canvas: Canvas) {
        val current = session ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        val now = SystemClock.elapsedRealtime()
        val elapsed = current.elapsed(now)
        val hitY = h - dp(112f)
        val playTop = dp(150f)
        val laneLeft = dp(13f)
        val laneRight = w - dp(13f)
        val laneWidth = (laneRight - laneLeft) / 4f
        val tileHeight = dp(62f)

        drawText(canvas, current.song.title, dp(18f), dp(29f), dp(15f), white, true, maxWidth = w - dp(92f))
        drawText(canvas, current.song.composer, dp(18f), dp(49f), dp(10f), muted)

        val pauseRect = RectF(w - dp(54f), dp(13f), w - dp(15f), dp(52f))
        roundRect(canvas, pauseRect, panel2, dp(12f))
        drawTextCentered(canvas, "Ⅱ", pauseRect.centerX(), pauseRect.centerY() + dp(5f), dp(19f), mint, true)

        drawText(canvas, "SCORE", dp(18f), dp(82f), dp(9f), muted, true)
        drawText(canvas, current.score.toString(), dp(18f), dp(111f), dp(23f), white, true)
        drawText(canvas, "STREAK", w - dp(105f), dp(82f), dp(9f), muted, true)
        drawText(canvas, current.combo.toString(), w - dp(105f), dp(111f), dp(23f), gold, true)

        val totalDuration = current.events.lastOrNull()?.targetSeconds ?: 1f
        val progress = (elapsed / totalDuration).coerceIn(0f, 1f)
        val progressRect = RectF(dp(18f), dp(126f), w - dp(18f), dp(132f))
        roundRect(canvas, progressRect, panel2, dp(3f))
        roundRect(canvas, RectF(progressRect.left, progressRect.top, progressRect.left + progressRect.width() * progress, progressRect.bottom), mint, dp(3f))

        // Four lanes form the play field.
        for (lane in 0..3) {
            val left = laneLeft + lane * laneWidth
            val rect = RectF(left + dp(2f), playTop, left + laneWidth - dp(2f), h - dp(31f))
            roundRect(canvas, rect, if (lane % 2 == 0) Color.rgb(16, 24, 43) else Color.rgb(14, 21, 39), dp(9f))
            if (lane > 0) {
                paint.color = Color.argb(50, 126, 146, 176)
                paint.strokeWidth = dp(1f)
                canvas.drawLine(left, playTop + dp(8f), left, h - dp(36f), paint)
            }
        }

        current.events.forEach { event ->
            if (event.state != TileState.PENDING) return@forEach
            val centerY = hitY - (event.targetSeconds - elapsed) * current.speedPxPerSecond
            if (centerY < playTop - tileHeight || centerY > hitY + tileHeight) return@forEach
            val left = laneLeft + event.lane * laneWidth + dp(5f)
            val rect = RectF(left, centerY - tileHeight / 2f, left + laneWidth - dp(10f), centerY + tileHeight / 2f)
            roundRect(canvas, rect, laneColors[event.lane], dp(9f))
            // A subtle inset creates a raised tile appearance.
            roundRect(canvas, RectF(rect.left + dp(3f), rect.top + dp(3f), rect.right - dp(3f), rect.bottom - dp(3f)), Color.argb(35, 255, 255, 255), dp(7f))
            drawTextCentered(canvas, noteName(event.midi), rect.centerX(), rect.centerY() + dp(4f), dp(12f), bg, true)
        }

        // Hit line and tap guidance.
        paint.color = Color.argb(190, 112, 226, 206)
        paint.strokeWidth = dp(2f)
        canvas.drawLine(laneLeft + dp(4f), hitY, laneRight - dp(4f), hitY, paint)
        drawTextCentered(canvas, "TAP THE TILE AS IT CROSSES", w / 2f, h - dp(16f), dp(8f), muted, true)

        if (feedback.isNotEmpty() && now < feedbackUntil) {
            drawTextCentered(canvas, feedback, w / 2f, dp(174f), dp(15f), if (feedback == "MISS") red else mint, true)
        }
    }

    private fun updateGame() {
        val current = session ?: return
        val now = SystemClock.elapsedRealtime()
        val elapsed = current.elapsed(now)
        var newMisses = 0
        current.events.forEach { event ->
            if (event.state == TileState.PENDING && elapsed > event.targetSeconds + 0.30f) {
                event.state = TileState.MISSED
                newMisses++
            }
        }
        if (newMisses > 0) {
            current.misses += newMisses
            current.combo = 0
            feedback = "MISS"
            feedbackUntil = now + 650L
        }
        if (current.misses >= 3 || current.remaining() == 0) {
            finishGame()
        }
    }

    private fun drawPauseOverlay(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.color = Color.argb(205, 5, 8, 18)
        canvas.drawRect(0f, 0f, w, h, paint)
        val panelRect = RectF(dp(28f), h * 0.31f, w - dp(28f), h * 0.70f)
        roundRect(canvas, panelRect, panel, dp(22f))
        drawTextCentered(canvas, "PAUSED", w / 2f, panelRect.top + dp(52f), dp(27f), white, true)
        drawTextCentered(canvas, "Take a breath. The music can wait.", w / 2f, panelRect.top + dp(80f), dp(11f), muted)
        val resume = RectF(panelRect.left + dp(20f), panelRect.top + dp(112f), panelRect.right - dp(20f), panelRect.top + dp(160f))
        roundRect(canvas, resume, mint, dp(14f))
        drawTextCentered(canvas, "RESUME", resume.centerX(), resume.centerY() + dp(5f), dp(12f), bg, true)
        val menu = RectF(resume.left, resume.bottom + dp(10f), resume.right, resume.bottom + dp(54f))
        roundRect(canvas, menu, panel2, dp(14f))
        drawTextCentered(canvas, "SONG LIBRARY", menu.centerX(), menu.centerY() + dp(4f), dp(11f), white, true)
    }

    private fun drawResult(canvas: Canvas) {
        val current = session
        val w = width.toFloat()
        val h = height.toFloat()
        drawTextCentered(canvas, if (lastResultWasCleared) "WELL PLAYED" else "NICE TRY", w / 2f, h * 0.20f, dp(13f), mint, true)
        drawTextCentered(canvas, current?.song?.title ?: "Classical Tiles", w / 2f, h * 0.25f, dp(14f), white, true)
        drawTextCentered(canvas, current?.score?.toString() ?: "0", w / 2f, h * 0.39f, dp(48f), gold, true)
        drawTextCentered(canvas, "POINTS", w / 2f, h * 0.43f, dp(9f), muted, true)

        val card = RectF(dp(24f), h * 0.49f, w - dp(24f), h * 0.61f)
        roundRect(canvas, card, panel, dp(17f))
        val accuracy = current?.let { if (it.events.isEmpty()) 0 else (it.hits * 100 / it.events.size) } ?: 0
        drawTextCentered(canvas, "ACCURACY", w * 0.25f, card.top + dp(24f), dp(8f), muted, true)
        drawTextCentered(canvas, "$accuracy%", w * 0.25f, card.top + dp(49f), dp(17f), white, true)
        val bestScore = current?.let { prefs.getInt("best_${it.song.id}", it.score) } ?: 0
        drawTextCentered(canvas, "BEST SCORE", w * 0.73f, card.top + dp(24f), dp(8f), muted, true)
        drawTextCentered(canvas, bestScore.toString(), w * 0.73f, card.top + dp(49f), dp(17f), white, true)

        val retry = RectF(dp(28f), h * 0.67f, w - dp(28f), h * 0.735f)
        roundRect(canvas, retry, mint, dp(14f))
        drawTextCentered(canvas, "PLAY AGAIN", retry.centerX(), retry.centerY() + dp(4f), dp(12f), bg, true)
        val next = RectF(dp(28f), h * 0.755f, w - dp(28f), h * 0.82f)
        roundRect(canvas, next, panel2, dp(14f))
        drawTextCentered(canvas, "NEXT CLASSIC", next.centerX(), next.centerY() + dp(4f), dp(11f), white, true)
        val menu = RectF(dp(28f), h * 0.84f, w - dp(28f), h * 0.905f)
        roundRect(canvas, menu, panel2, dp(14f))
        drawTextCentered(canvas, "SONG LIBRARY", menu.centerX(), menu.centerY() + dp(4f), dp(11f), white, true)
    }

    private fun beginGame(index: Int) {
        selectedIndex = index.coerceIn(0, songs.lastIndex)
        val song = songs[selectedIndex]
        val h = height.toFloat()
        val playTop = dp(150f)
        val hitY = h - dp(112f)
        val speed = dp(390f)
        val fallSeconds = ((hitY - playTop) / speed).coerceIn(0.85f, 1.45f)
        val spacing = max(0.30f, (60f / song.bpm) * 0.72f)
        val firstTarget = fallSeconds + 0.08f
        val events = song.notes.mapIndexed { i, midi ->
            TileEvent(midi, midi % 4, firstTarget + i * spacing)
        }
        session = GameSession(song, events, speed, fallSeconds, SystemClock.elapsedRealtime())
        feedback = ""
        lastResultWasCleared = false
        screen = Screen.GAME
        invalidate()
    }

    private fun hitLane(x: Float) {
        val current = session ?: return
        val w = width.toFloat()
        val laneWidth = (w - dp(26f)) / 4f
        val lane = ((x - dp(13f)) / laneWidth).toInt().coerceIn(0, 3)
        val now = SystemClock.elapsedRealtime()
        updateGame()
        if (screen != Screen.GAME) return
        val elapsed = current.elapsed(now)
        val candidate = current.events
            .asSequence()
            .filter { it.state == TileState.PENDING && it.lane == lane }
            .minByOrNull { abs(it.targetSeconds - elapsed) }
            ?: return
        val difference = abs(candidate.targetSeconds - elapsed)
        if (difference <= 0.22f) {
            candidate.state = TileState.HIT
            current.hits++
            current.combo++
            current.bestCombo = max(current.bestCombo, current.combo)
            val perfect = difference <= 0.105f
            val points = if (perfect) 100 else 60
            current.score += points + min(40, current.combo * 2)
            if (perfect) current.perfects++ else current.goods++
            audio.play(candidate.midi)
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            feedback = if (perfect) "PERFECT  +$points" else "GOOD  +$points"
            feedbackUntil = now + 450L
            if (current.remaining() == 0) finishGame()
            invalidate()
        } else if (difference < 0.52f) {
            feedback = if (candidate.targetSeconds > elapsed) "A LITTLE EARLY" else "A LITTLE LATE"
            feedbackUntil = now + 350L
            invalidate()
        }
    }

    private fun finishGame() {
        val current = session ?: return
        if (screen == Screen.RESULT) return
        lastResultWasCleared = current.misses < 3 && current.remaining() == 0
        val key = "best_${current.song.id}"
        val previous = prefs.getInt(key, 0)
        if (current.score > previous) prefs.edit().putInt(key, current.score).apply()
        screen = Screen.RESULT
        invalidate()
    }

    fun handleBack(): Boolean {
        when (screen) {
            Screen.MENU -> return false
            Screen.GAME -> {
                session?.pause(SystemClock.elapsedRealtime())
                screen = Screen.PAUSED
            }
            Screen.PAUSED -> {
                screen = Screen.MENU
                session = null
            }
            Screen.RESULT -> {
                screen = Screen.MENU
                session = null
            }
        }
        invalidate()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                consumeNextUp = false
                if (screen == Screen.GAME) {
                    val pauseRect = RectF(width - dp(58f), dp(10f), width.toFloat() - dp(10f), dp(56f))
                    if (pauseRect.contains(event.x, event.y)) {
                        session?.pause(SystemClock.elapsedRealtime())
                        screen = Screen.PAUSED
                        invalidate()
                    } else if (event.y >= dp(145f) && event.y <= height - dp(25f)) {
                        hitLane(event.x)
                    }
                } else if (screen == Screen.PAUSED) {
                    consumeNextUp = true
                    val w = width.toFloat()
                    val h = height.toFloat()
                    val panelTop = h * 0.31f
                    val resume = RectF(dp(48f), panelTop + dp(112f), w - dp(48f), panelTop + dp(160f))
                    val menu = RectF(resume.left, resume.bottom + dp(10f), resume.right, resume.bottom + dp(54f))
                    if (resume.contains(event.x, event.y)) {
                        session?.resume(SystemClock.elapsedRealtime())
                        screen = Screen.GAME
                    } else if (menu.contains(event.x, event.y)) {
                        screen = Screen.MENU
                        session = null
                    }
                    invalidate()
                } else if (screen == Screen.RESULT) {
                    consumeNextUp = true
                    handleResultTap(event.x, event.y)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (consumeNextUp) {
                    consumeNextUp = false
                    return true
                }
                if (screen == Screen.MENU) {
                    val dy = event.y - downY
                    if (abs(dy) > dp(12f)) {
                        val visibleHeight = max(0f, menuBottom - menuListTop)
                        val maxScroll = max(0f, songs.size * menuRowHeight - visibleHeight)
                        menuScroll = (menuScroll - dy).coerceIn(0f, maxScroll)
                        invalidate()
                    } else if (event.y >= menuListTop && event.y <= menuBottom) {
                        val row = ((event.y - menuListTop + menuScroll) / menuRowHeight).toInt()
                        if (row in songs.indices) beginGame(row)
                    }
                }
                return true
            }
        }
        return true
    }

    private fun handleResultTap(x: Float, y: Float) {
        val h = height.toFloat()
        val w = width.toFloat()
        val retry = RectF(dp(28f), h * 0.67f, w - dp(28f), h * 0.735f)
        val next = RectF(dp(28f), h * 0.755f, w - dp(28f), h * 0.82f)
        val menu = RectF(dp(28f), h * 0.84f, w - dp(28f), h * 0.905f)
        when {
            retry.contains(x, y) -> beginGame(selectedIndex)
            next.contains(x, y) -> beginGame((selectedIndex + 1) % songs.size)
            menu.contains(x, y) -> {
                session = null
                screen = Screen.MENU
            }
        }
        invalidate()
    }

    private fun noteName(midi: Int): String {
        val names = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
        val octave = midi / 12 - 1
        return "${names[midi % 12]}$octave"
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean = false,
        maxWidth: Float = Float.MAX_VALUE
    ) {
        paint.color = color
        paint.textSize = size
        paint.typeface = if (bold) android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        else android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT
        var shown = text
        if (paint.measureText(shown) > maxWidth) {
            while (shown.length > 2 && paint.measureText("$shown…") > maxWidth) shown = shown.dropLast(1)
            shown += "…"
        }
        canvas.drawText(shown, x, y, paint)
    }

    private fun drawTextFit(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) {
        paint.color = color
        paint.textSize = size
        paint.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        var shown = text
        while (shown.length > 3 && paint.measureText(shown) > maxWidth) shown = shown.dropLast(1)
        if (shown != text) shown = shown.dropLast(1) + "…"
        canvas.drawText(shown, x, y, paint)
    }

    private fun drawTextCentered(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) {
        paint.color = color
        paint.textSize = size
        paint.typeface = if (bold) android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        else android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, x, y, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun roundRect(canvas: Canvas, rect: RectF, color: Int, radius: Float) {
        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    override fun onDetachedFromWindow() {
        audio.release()
        super.onDetachedFromWindow()
    }
}
