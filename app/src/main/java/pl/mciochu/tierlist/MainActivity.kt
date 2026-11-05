package pl.mciochu.tierlist

import android.content.Context
import android.os.Bundle
import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.mciochu.tierlist.data.AnimeAsset
import pl.mciochu.tierlist.data.LocalBoardStorage
import pl.mciochu.tierlist.data.SavedEditor
import pl.mciochu.tierlist.domain.Tier
import pl.mciochu.tierlist.domain.TierEntry
import pl.mciochu.tierlist.domain.TierList
import pl.mciochu.tierlist.domain.TierListEditor
import pl.mciochu.tierlist.domain.TierListHistory
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var displayManager: DisplayManager
    private var projection: BoardProjection? = null
    private var projectionDisplayId: Int? = null
    private var lastProjectionState: SavedEditor? = null

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            lastProjectionState?.let { updateProjection(it) }
        }
        override fun onDisplayChanged(displayId: Int) {
            lastProjectionState?.let { updateProjection(it) }
        }
        override fun onDisplayRemoved(displayId: Int) {
            if (projectionDisplayId == displayId) {
                projection?.dismiss()
                projection = null
                projectionDisplayId = null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
        setContent { MaterialTheme { TierBoardScreen(this) { updateProjection(it) } } }
    }

    private fun updateProjection(state: SavedEditor) {
        lastProjectionState = state
        val display = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            .firstOrNull()
        if (display == null) {
            projection?.dismiss()
            projection = null
            projectionDisplayId = null
            return
        }
        if (projectionDisplayId != display.displayId || projection == null) {
            projection?.dismiss()
            projection = BoardProjection(this, display, state).also {
                projectionDisplayId = display.displayId
                it.show()
            }
        } else {
            projection?.update(state)
        }
    }

    override fun onDestroy() {
        displayManager.unregisterDisplayListener(displayListener)
        projection?.dismiss()
        projection = null
        super.onDestroy()
    }
}

@Composable
private fun TierBoardScreen(context: Context, onProjectionUpdate: (SavedEditor) -> Unit) {
    val storage = remember { LocalBoardStorage(context) }
    val initial = remember {
        storage.load() ?: SavedEditor(
            TierList.createDefault("local-board", "Moja tierlista"),
            emptyMap(),
            1
        )
    }
    var saved by remember { mutableStateOf(initial) }
    var history by remember { mutableStateOf(TierListHistory(initial.board)) }
    var newTitle by remember { mutableStateOf("") }
    var editTier by remember { mutableStateOf<Tier?>(null) }
    var editTierLabel by remember { mutableStateOf("") }
    var pendingPosterId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(saved) {
        storage.save(saved)
        onProjectionUpdate(saved)
    }

    fun applyBoard(next: TierList) {
        history = history.apply(next)
        saved = saved.copy(board = next)
    }

    val posterPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val animeId = pendingPosterId
        if (uri != null && animeId != null) {
            val directory = File(context.filesDir, "posters").apply { mkdirs() }
            val file = File(directory, animeId + "-" + System.currentTimeMillis() + ".img")
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Nie udało się otworzyć pliku.")
                val asset = saved.anime[animeId] ?: AnimeAsset(animeId)
                saved = saved.copy(anime = saved.anime + (animeId to asset.copy(posterPath = file.absolutePath)))
            }
        }
        pendingPosterId = null
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(saved.board.name, style = MaterialTheme.typography.headlineMedium)
                    Text("Anime Tier Board · zapis lokalny", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            history = history.undo()
                            saved = saved.copy(board = history.current)
                        },
                        enabled = history.canUndo
                    ) { Text("Cofnij") }
                    OutlinedButton(
                        onClick = {
                            history = history.redo()
                            saved = saved.copy(board = history.current)
                        },
                        enabled = history.canRedo
                    ) { Text("Ponów") }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Dodaj anime do biblioteki") }
                )
                Button(
                    enabled = newTitle.isNotBlank(),
                    onClick = {
                        val title = newTitle.trim()
                        val id = "anime-" + saved.nextAnimeId
                        applyBoard(TierListEditor.addAnime(saved.board, id))
                        saved = saved.copy(
                            anime = saved.anime + (id to AnimeAsset(title)),
                            nextAnimeId = saved.nextAnimeId + 1
                        )
                        newTitle = ""
                    }
                ) { Text("Dodaj") }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(saved.board.entries.size.toString() + " anime · " +
                    saved.board.tiers.size + " poziomów", style = MaterialTheme.typography.labelLarge)
                TextButton(
                    onClick = {
                        val id = "tier-" + (saved.board.tiers.size + 1)
                        applyBoard(TierListEditor.addTier(saved.board, id, "Nowy", 0xFF777777L))
                    }
                ) { Text("+ Poziom") }
            }

            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(saved.board.tiers.sortedBy { it.position }, key = { it.id }) { tier ->
                    TierBoardRow(
                        tier, saved.board.entries.filter { it.tierId == tier.id }.sortedBy { it.position },
                        saved.anime, saved.board,
                        onEditTier = { editTier = tier; editTierLabel = tier.label },
                        onMoveAnime = { id, target, position ->
                            applyBoard(TierListEditor.moveAnime(saved.board, id, target, position))
                        },
                        onRemoveAnime = { id -> applyBoard(TierListEditor.removeAnime(saved.board, id)) },
                        onChoosePoster = { id ->
                            pendingPosterId = id
                            posterPicker.launch(arrayOf("image/*"))
                        }
                    )
                }
                item(key = "unranked") {
                    TierBoardRow(
                        Tier("unranked", "Do umieszczenia", 0xFF555555L, Int.MAX_VALUE),
                        saved.board.entries.filter { it.tierId == null }.sortedBy { it.position },
                        saved.anime, saved.board,
                        onEditTier = {},
                        onMoveAnime = { id, target, position ->
                            applyBoard(TierListEditor.moveAnime(saved.board, id, target, position))
                        },
                        onRemoveAnime = { id -> applyBoard(TierListEditor.removeAnime(saved.board, id)) },
                        onChoosePoster = { id ->
                            pendingPosterId = id
                            posterPicker.launch(arrayOf("image/*"))
                        }
                    )
                }
            }
        }
    }

    val selectedTier = editTier
    if (selectedTier != null) {
        AlertDialog(
            onDismissRequest = { editTier = null },
            title = { Text("Edytuj poziom " + selectedTier.label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTierLabel,
                        onValueChange = { editTierLabel = it },
                        singleLine = true,
                        label = { Text("Nazwa poziomu") }
                    )
                    Text("Kolor")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(TIER_COLORS) { color ->
                            TextButton(onClick = {
                                applyBoard(TierListEditor.updateTier(saved.board, selectedTier.id, colorArgb = color))
                            }) { Text("●", color = Color(color.toInt())) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = selectedTier.position > 0,
                            onClick = {
                                applyBoard(TierListEditor.moveTier(saved.board, selectedTier.id, selectedTier.position - 1))
                            }
                        ) { Text("↑") }
                        OutlinedButton(
                            enabled = selectedTier.position < saved.board.tiers.lastIndex,
                            onClick = {
                                applyBoard(TierListEditor.moveTier(saved.board, selectedTier.id, selectedTier.position + 1))
                            }
                        ) { Text("↓") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        applyBoard(TierListEditor.updateTier(saved.board, selectedTier.id, label = editTierLabel))
                    }
                    editTier = null
                }) { Text("Zapisz") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        if (saved.board.tiers.size > 1) {
                            applyBoard(TierListEditor.removeTier(saved.board, selectedTier.id))
                        }
                        editTier = null
                    }) { Text("Usuń poziom") }
                    TextButton(onClick = { editTier = null }) { Text("Anuluj") }
                }
            }
        )
    }
}

private val TIER_COLORS = listOf(
    0xFFE45B5BL, 0xFFFF9848L, 0xFFE7CE56L, 0xFF75B86BL,
    0xFF5DA5CFL, 0xFF9B80C6L, 0xFF777777L, 0xFF333333L
)

@Composable
private fun TierBoardRow(
    tier: Tier,
    entries: List<TierEntry>,
    assets: Map<String, AnimeAsset>,
    board: TierList,
    onEditTier: () -> Unit,
    onMoveAnime: (String, String?, Int?) -> Unit,
    onRemoveAnime: (String) -> Unit,
    onChoosePoster: (String) -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(tier.colorArgb.toInt()).copy(alpha = 0.20f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(tier.label, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entries.size.toString(), style = MaterialTheme.typography.labelLarge)
                    if (tier.id != "unranked") TextButton(onClick = onEditTier) { Text("Edytuj") }
                }
            }
            if (entries.isEmpty()) {
                Text("Brak anime", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(entries, key = { it.animeId }) { entry ->
                        AnimeTile(
                            entry, assets[entry.animeId] ?: AnimeAsset(entry.animeId), board,
                            onMoveAnime, onRemoveAnime, onChoosePoster
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeTile(
    entry: TierEntry,
    asset: AnimeAsset,
    board: TierList,
    onMoveAnime: (String, String?, Int?) -> Unit,
    onRemoveAnime: (String) -> Unit,
    onChoosePoster: (String) -> Unit
) {
    var expanded by remember(entry.animeId) { mutableStateOf(false) }
    val inTier = board.entries.filter { it.tierId == entry.tierId }.sortedBy { it.position }
    val index = inTier.indexOfFirst { it.animeId == entry.animeId }
    Card(Modifier.width(112.dp)) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PosterPreview(asset.posterPath)
            Text(asset.title, style = MaterialTheme.typography.labelMedium, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            Box {
                OutlinedButton(onClick = { expanded = true }) { Text("Opcje") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    board.tiers.filter { it.id != entry.tierId }.sortedBy { it.position }.forEach { tier ->
                        DropdownMenuItem(text = { Text("Do " + tier.label) }, onClick = {
                            onMoveAnime(entry.animeId, tier.id, null); expanded = false
                        })
                    }
                    if (entry.tierId != null) {
                        DropdownMenuItem(text = { Text("Nieumieszczone") }, onClick = {
                            onMoveAnime(entry.animeId, null, null); expanded = false
                        })
                    }
                    if (index > 0) {
                        DropdownMenuItem(text = { Text("Przesuń w lewo") }, onClick = {
                            onMoveAnime(entry.animeId, entry.tierId, index - 1); expanded = false
                        })
                    }
                    if (index >= 0 && index < inTier.lastIndex) {
                        DropdownMenuItem(text = { Text("Przesuń w prawo") }, onClick = {
                            onMoveAnime(entry.animeId, entry.tierId, index + 1); expanded = false
                        })
                    }
                    DropdownMenuItem(text = { Text("Dodaj/zmień plakat") }, onClick = {
                        onChoosePoster(entry.animeId); expanded = false
                    })
                    DropdownMenuItem(text = { Text("Usuń z listy") }, onClick = {
                        onRemoveAnime(entry.animeId); expanded = false
                    })
                }
            }
        }
    }
}

@Composable
private fun PosterPreview(path: String?) {
    val bitmap = remember(path) { path?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() } }
    if (bitmap != null) {
        Image(bitmap.asImageBitmap(), "Plakat anime", Modifier.fillMaxWidth().height(132.dp))
    } else {
        Box(
            Modifier.fillMaxWidth().height(132.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) { Text("Brak plakatu", style = MaterialTheme.typography.labelSmall) }
    }
}
