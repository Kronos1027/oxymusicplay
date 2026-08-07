package com.oxymusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.oxymusic.app.model.Track
import com.oxymusic.app.ui.viewmodel.PlayerViewModel
import com.oxymusic.app.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    onTrackClick: () -> Unit,
    searchVm: SearchViewModel = hiltViewModel(),
    playerVm: PlayerViewModel = hiltViewModel(),
) {
    val query by searchVm.query.collectAsState()
    val results by searchVm.results.collectAsState()
    val loading by searchVm.loading.collectAsState()
    val colors = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        OutlinedTextField(
            value = query,
            onValueChange = searchVm::onQueryChange,
            placeholder = { Text("Buscar músicas, artistas…", color = colors.onSurface.copy(alpha = 0.4f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = colors.primary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { searchVm.search() }),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.onSurface.copy(alpha = 0.2f),
                cursorColor = colors.primary,
            )
        )
        if (loading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
            items(results) { track ->
                Row(modifier = Modifier.fillMaxWidth().clickable {
                    playerVm.playQueue(results, results.indexOf(track))
                    onTrackClick()
                }.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = track.artworkUrl, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track.title, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = colors.primary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
