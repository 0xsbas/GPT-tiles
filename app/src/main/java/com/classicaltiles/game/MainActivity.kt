package com.classicaltiles.game

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager

class MainActivity : Activity() {
    private lateinit var gameView: PianoTilesView

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(11, 16, 32)
        window.navigationBarColor = Color.rgb(11, 16, 32)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        gameView = PianoTilesView(this)
        setContentView(gameView)
    }

    @Deprecated("Deprecated in Android, retained for compatibility with older devices")
    override fun onBackPressed() {
        if (!::gameView.isInitialized || !gameView.handleBack()) {
            super.onBackPressed()
        }
    }
}
