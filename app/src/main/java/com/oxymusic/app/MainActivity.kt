package com.oxymusic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.oxymusic.app.media.PlaybackService
import com.oxymusic.app.ui.OxyMusicApp
import com.oxymusic.app.ui.theme.OxyMusicTheme
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OxyMusicTheme {
                OxyMusicApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, android.content.ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
    }

    override fun onStop() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        controllerFuture = null
        super.onStop()
    }
}
