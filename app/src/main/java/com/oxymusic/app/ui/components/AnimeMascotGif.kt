package com.oxymusic.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.oxymusic.app.model.AnimeTheme

@Composable
fun AnimeMascotGif(
    animeTheme: AnimeTheme,
    isPlaying: Boolean,
    size: Dp = 180.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val assetName = when {
        animeTheme == AnimeTheme.GHIBLI && isPlaying -> "anime/mascot_ghibli_dance.gif"
        animeTheme == AnimeTheme.GHIBLI -> "anime/mascot_ghibli.gif"
        isPlaying -> "anime/mascot_sakura_dance.gif"
        else -> "anime/mascot_sakura.gif"
    }
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data("file:///android_asset/$assetName")
            .decoderFactory(
                if (android.os.Build.VERSION.SDK_INT >= 28) ImageDecoderDecoder.Factory()
                else GifDecoder.Factory()
            )
            .build()
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Image(painter = painter, contentDescription = "Anime mascot", modifier = Modifier.fillMaxSize())
    }
}
