package com.oxymusic.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.oxymusic.app.model.Track
import com.oxymusic.app.model.TrackSource
import com.oxymusic.app.ui.viewmodel.LibraryViewModel
import com.oxymusic.app.ui.viewmodel.PlayerViewModel

@Composable
fun LibraryScreen(
    onTrackClick: () -> Unit,
    libraryVm: LibraryViewModel = hiltViewModel(),
    playerVm: PlayerViewModel = hiltViewModel(),
) {
    val selectedTab by libraryVm.selectedTab.collectAsState()
    val localTracks by libraryVm.localTracks.collectAsState()
    val onlineTracks by libraryVm.onlineTracks.collectAsState()
    val scanning by libraryVm.scanning.collectAsState()
    val searching by libraryVm.searching.collectAsState()
    val query by libraryVm.query.collectAsState()
    val permissionGranted by libraryVm.permissionGranted.collectAsState()
    val error by libraryVm.error.collectAsState()
    val colors = MaterialTheme.colorScheme

    // Permission launcher for READ_MEDIA_AUDIO
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        libraryVm.refreshPermission()
        if (results.values.any { it }) libraryVm.scanLocal()
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permLauncher.launch(arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO))
        } else if (localTracks.isEmpty()) {
            libraryVm.scanLocal()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        // Header with tabs
        TabRow(
            selectedTabIndex = if (selectedTab == "local") 0 else 1,
            containerColor = colors.surface,
            contentColor = colors.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == "local",
                onClick = { libraryVm.selectTab("local") },
                text = { Text("Meu aparelho", fontWeight = FontWeight.Medium) },
                icon = { Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(18.dp)) },
                unselectedContentColor = colors.onSurface.copy(alpha = 0.5f),
            )
            Tab(
                selected = selectedTab == "online",
                onClick = { libraryVm.selectTab("online") },
                text = { Text("Online (YouTube)", fontWeight = FontWeight.Medium) },
                icon = { Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp)) },
                unselectedContentColor = colors.onSurface.copy(alpha = 0.5f),
            )
        }

        // Online: search bar
        AnimatedVisibility(visible = selectedTab == "online", enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = libraryVm::onQueryChange,
                    placeholder = { Text("Buscar no YouTube…", color = colors.onSurface.copy(alpha = 0.4f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.primary) },
                    trailingIcon = {
                        IconButton(onClick = { libraryVm.search() }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar", tint = colors.primary)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { libraryVm.search() }),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.onSurface.copy(alpha = 0.2f),
                        cursorColor = colors.primary,
                    )
                )
                IconButton(onClick = { libraryVm.loadTrending() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Trending", tint = colors.primary)
                }
            }
        }

        // Local: refresh button
        AnimatedVisibility(visible = selectedTab == "local", enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (localTracks.isEmpty() && !scanning) "Nenhuma música ainda" else "${localTracks.size} faixas locais",
                    color = colors.onSurface.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                IconButton(onClick = {
                    if (!permissionGranted) {
                        permLauncher.launch(arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO))
                    } else {
                        libraryVm.scanLocal()
                    }
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Rescan", tint = colors.primary)
                }
            }
        }

        // Loading states
        AnimatedVisibility(visible = scanning || searching, enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = colors.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (scanning) "Escaneando aparelho…" else "Buscando…",
                        color = colors.primary,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        // Error
        AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.errorContainer.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { libraryVm.clearError() }
            ) {
                Text(
                    text = error ?: "",
                    color = colors.onErrorContainer,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Permission denied state
        if (selectedTab == "local" && !permissionGranted) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Album, contentDescription = null, tint = colors.primary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Permissão de áudio necessária", color = colors.onSurface, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Para mostrar suas músicas locais, o OxyMusic precisa de acesso ao seu áudio.",
                        color = colors.onSurface.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { permLauncher.launch(arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO)) },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Conceder acesso")
                    }
                }
            }
            return@Column
        }

        // Track list
        val tracks = if (selectedTab == "local") localTracks else onlineTracks
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            items(tracks, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    onClick = {
                        playerVm.playTrack(track)
                        onTrackClick()
                    },
                )
            }
            // Bottom spacing
            item { Spacer(Modifier.height(120.dp)) }
        }
    }
}

@Composable
private fun TrackRow(track: Track, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val isLocal = track.source == TrackSource.LOCAL
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail (or icon for local tracks without album art)
        val thumb = track.thumbnailUrl
        if (thumb.isNotEmpty() && thumb.startsWith("http")) {
            AsyncImage(
                model = thumb,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else if (thumb.isNotEmpty() && thumb.startsWith("content://")) {
            AsyncImage(
                model = thumb,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isLocal) Icons.Default.AudioFile else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Title + artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLocal) {
                    // Local: show bitrate + album
                    Text(
                        text = buildString {
                            append(track.artist)
                            if (track.bitrate > 0) {
                                val kbps = track.bitrate / 1000
                                append(" · ${kbps}kbps")
                            }
                            if (!track.album.isNullOrEmpty()) {
                                append(" · ${track.album}")
                            }
                        },
                        color = colors.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                } else {
                    Text(
                        text = track.artist,
                        color = colors.primary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }

        // Duration
        if (track.durationMs > 0) {
            Text(
                text = fmtTime(track.durationMs),
                color = colors.onSurface.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

private fun fmtTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
