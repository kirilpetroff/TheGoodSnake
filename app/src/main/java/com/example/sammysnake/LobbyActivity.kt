package com.example.sammysnake

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class LobbyActivity : AppCompatActivity() {

    private lateinit var musicPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applyLanguage(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lobby)

        musicPlayer = MediaPlayer.create(this, R.raw.three_lives_remaining).apply {
            isLooping = true
            val volume = AppPreferences.getMusicVolume(this@LobbyActivity)
            setVolume(volume, volume)
        }

        findViewById<View>(R.id.lobbyRoot).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (!musicPlayer.isPlaying) {
            musicPlayer.start()
        }
    }

    override fun onPause() {
        super.onPause()
        if (musicPlayer.isPlaying) {
            musicPlayer.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicPlayer.release()
    }
}
