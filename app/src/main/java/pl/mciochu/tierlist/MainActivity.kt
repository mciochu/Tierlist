package pl.mciochu.tierlist

import android.content.Context
import android.os.Bundle
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.unit.sp
import pl.mciochu.tierlist.data.AnimeAsset
import pl.mciochu.tierlist.data.LocalBoardStorage
import pl.mciochu.tierlist.data.SavedEditor
import pl.mciochu.tierlist.domain.Tier
import pl.mciochu.tierlist.domain.TierEntry
import pl.mciochu.tierlist.domain.TierList
import pl.mciochu.tierlist.domain.TierListEditor
import pl.mciochu.tierlist.domain.TierListHistory
import pl.mciochu.tierlist.domain.buildPositionChanges
import java.text.DateFormat
import java.util.Date
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
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { TierBoardScreen(this) { updateProjection(it) } } }
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
        val loaded = storage.load()
        val preferences = context.getSharedPreferences("tier_board", Context.MODE_PRIVATE)
        val hasSeededDemoData = preferences.getBoolean("sample_data_initialized", false)
        val base = loaded ?: SavedEditor(
            TierList.createDefault("local-board", "Moja tierlista"),
            emptyMap(),
            1
        )
        val shouldSeedDemo = !hasSeededDemoData &&
            (loaded == null || (loaded.board.entries.isEmpty() && loaded.anime.isEmpty()))
        preferences.edit().putBoolean("sample_data_initialized", true).apply()
        if (shouldSeedDemo) createSampleEditor(base) else base
    }
    var saved by remember { mutableStateOf(initial) }
    var history by remember { mutableStateOf(TierListHistory(initial.board)) }
    var newTitle by remember { mutableStateOf("") }
    var editTier by remember { mutableStateOf<Tier?>(null) }
    var editTierLabel by remember { mutableStateOf("") }
    var pendingPosterId by remember { mutableStateOf<String?>(null) }
    var selectedAnimeId by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showAddAnimeDialog by remember { mutableStateOf(false) }
    var showTierManager by remember { mutableStateOf(false) }

    LaunchedEffect(saved) {
        storage.save(saved)
        onProjectionUpdate(saved)
    }

    fun applyBoard(next: TierList) {
        history = history.apply(next)
        saved = recordPositionChanges(saved, next, "MOVE")
    }

    fun restoreBoard(next: TierList, operation: String) {
        saved = recordPositionChanges(saved, next, operation)
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

    Surface(Modifier.fillMaxSize(), color = Color(0xFF101216)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(saved.board.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        saved.board.entries.size.toString() + " anime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    OutlinedButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text("MENU")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Dodaj anime") },
                            onClick = {
                                menuExpanded = false
                                showAddAnimeDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ustawienia poziomów") },
                            onClick = {
                                menuExpanded = false
                                showTierManager = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cofnij") },
                            enabled = history.canUndo,
                            onClick = {
                                menuExpanded = false
                                history = history.undo()
                                restoreBoard(history.current, "UNDO")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ponów") },
                            enabled = history.canRedo,
                            onClick = {
                                menuExpanded = false
                                history = history.redo()
                                restoreBoard(history.current, "REDO")
                            }
                        )
                    }
                }
            }

            val rankedTiers = saved.board.tiers.sortedBy { it.position }
            val unrankedEntries = saved.board.entries
                .filter { it.tierId == null }
                .sortedBy { it.position }

            Column(
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 5.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                rankedTiers.forEach { tier ->
                    TierBoardRow(
                        modifier = Modifier.weight(1f),
                        tier = tier,
                        entries = saved.board.entries
                            .filter { it.tierId == tier.id }
                            .sortedBy { it.position },
                        assets = saved.anime,
                        board = saved.board,
                        onMoveAnime = { id, target, position ->
                            applyBoard(TierListEditor.moveAnime(saved.board, id, target, position))
                        },
                        onRemoveAnime = { id ->
                            applyBoard(TierListEditor.removeAnime(saved.board, id))
                        },
                        onChoosePoster = { id ->
                            pendingPosterId = id
                            posterPicker.launch(arrayOf("image/*"))
                        },
                        onShowStats = { selectedAnimeId = it }
                    )
                }
                if (unrankedEntries.isNotEmpty()) {
                    TierBoardRow(
                        modifier = Modifier.weight(1f),
                        tier = Tier("unranked", "…", 0xFF555B66L, Int.MAX_VALUE),
                        entries = unrankedEntries,
                        assets = saved.anime,
                        board = saved.board,
                        onMoveAnime = { id, target, position ->
                            applyBoard(TierListEditor.moveAnime(saved.board, id, target, position))
                        },
                        onRemoveAnime = { id ->
                            applyBoard(TierListEditor.removeAnime(saved.board, id))
                        },
                        onChoosePoster = { id ->
                            pendingPosterId = id
                            posterPicker.launch(arrayOf("image/*"))
                        },
                        onShowStats = { selectedAnimeId = it }
                    )
                }
            }
        }
    }

    if (showAddAnimeDialog) {
        AlertDialog(
            onDismissRequest = { showAddAnimeDialog = false },
            title = { Text("Dodaj anime") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    label = { Text("Tytuł anime") }
                )
            },
            confirmButton = {
                TextButton(
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
                        showAddAnimeDialog = false
                    }
                ) { Text("Dodaj") }
            },
            dismissButton = {
                TextButton(onClick = { showAddAnimeDialog = false }) { Text("Anuluj") }
            }
        )
    }

    if (showTierManager) {
        AlertDialog(
            onDismissRequest = { showTierManager = false },
            title = { Text("Ustawienia poziomów") },
            text = {
                LazyColumn(
                    Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(saved.board.tiers.sortedBy { it.position }, key = { it.id }) { tier ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                Modifier.width(7.dp).height(30.dp)
                                    .background(Color(tier.colorArgb.toInt()))
                            )
                            Text(
                                tier.label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium
                            )
                            TextButton(onClick = {
                                showTierManager = false
                                editTier = tier
                                editTierLabel = tier.label
                            }) { Text("Edytuj") }
                            TextButton(
                                enabled = tier.position > 0,
                                onClick = {
                                    applyBoard(
                                        TierListEditor.moveTier(saved.board, tier.id, tier.position - 1)
                                    )
                                }
                            ) { Text("↑") }
                            TextButton(
                                enabled = tier.position < saved.board.tiers.lastIndex,
                                onClick = {
                                    applyBoard(
                                        TierListEditor.moveTier(saved.board, tier.id, tier.position + 1)
                                    )
                                }
                            ) { Text("↓") }
                            TextButton(
                                enabled = saved.board.tiers.size > 1,
                                onClick = {
                                    applyBoard(TierListEditor.removeTier(saved.board, tier.id))
                                }
                            ) { Text("×") }
                        }
                    }
                }
            },
            confirmButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        val base = "Nowy"
                        var label = base
                        var suffix = 2
                        while (saved.board.tiers.any { it.label.equals(label, ignoreCase = true) }) {
                            label = base + " " + suffix
                            suffix += 1
                        }
                        val id = "tier-" + System.currentTimeMillis()
                        applyBoard(
                            TierListEditor.addTier(saved.board, id, label, 0xFF777777L)
                        )
                    }) { Text("+ Dodaj poziom") }
                    TextButton(onClick = { showTierManager = false }) { Text("Gotowe") }
                }
            }
        )
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
    val selectedAnime = selectedAnimeId
    if (selectedAnime != null) {
        val asset = saved.anime[selectedAnime] ?: AnimeAsset(selectedAnime)
        val currentEntry = saved.board.entries.singleOrNull { it.animeId == selectedAnime }
        val changes = saved.positionHistory.filter { it.animeId == selectedAnime }
            .sortedByDescending { it.timestamp }
        val tierChanges = changes.count { it.fromTierId != it.toTierId }
        val orderChanges = changes.count {
            it.fromTierId == it.toTierId &&
                it.fromPosition != null && it.toPosition != null &&
                it.fromPosition != it.toPosition
        }
        val visited = (changes.flatMap { listOfNotNull(it.fromTierLabel, it.toTierLabel) } +
            listOfNotNull(currentEntry?.tierId?.let { id -> saved.board.tiers.firstOrNull { it.id == id }?.label }))
            .distinct()
        val tierOrder = saved.board.tiers.sortedBy { it.position }
        val tierRanks = visited.mapNotNull { label ->
            tierOrder.indexOfFirst { it.label == label }.takeIf { it >= 0 }?.let { label to it }
        }
        val highest = tierRanks.minByOrNull { it.second }?.first
        val lowest = tierRanks.maxByOrNull { it.second }?.first
        val currentLabel = currentEntry?.tierId?.let { id ->
            saved.board.tiers.firstOrNull { it.id == id }?.label
        } ?: "Nieumieszczone"
        AlertDialog(
            onDismissRequest = { selectedAnimeId = null },
            title = { Text(asset.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aktualnie: $currentLabel", style = MaterialTheme.typography.titleSmall)
                    Text("Zmiany położenia: ${changes.size} · odwiedzone poziomy: ${visited.size}")
                    Text("Zmiany tieru: $tierChanges · kolejności: $orderChanges")
                    Text("Najwyższy: ${highest ?: "—"} · najniższy: ${lowest ?: "—"}")
                    Text("Historia pozycji", style = MaterialTheme.typography.titleSmall)
                    if (changes.isEmpty()) {
                        Text("Brak zarejestrowanych zmian.")
                    } else {
                        LazyColumn(Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(changes) { change ->
                                val from = if (change.fromPosition == null) "Biblioteka" else
                                    (change.fromTierLabel ?: "Nieumieszczone") + " · poz. ${change.fromPosition}"
                                val to = if (change.toPosition == null) "Biblioteka" else
                                    (change.toTierLabel ?: "Nieumieszczone") + " · poz. ${change.toPosition}"
                                Column {
                                    Text("$from → $to", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(change.timestamp))} · ${change.operation}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAnimeId = null }) { Text("Zamknij") }
            }
        )
    }
}

private fun createSampleEditor(base: SavedEditor): SavedEditor {
    val samples = listOf(
        Triple("demo-spice-and-wolf", AnimeAsset("Spice and Wolf"), "s"),
        Triple("demo-frieren", AnimeAsset("Frieren: Beyond Journey's End"), "a"),
        Triple("demo-bocchi", AnimeAsset("Bocchi the Rock!"), "b"),
        Triple("demo-steins-gate", AnimeAsset("Steins;Gate"), "c"),
        Triple("demo-fullmetal", AnimeAsset("Fullmetal Alchemist: Brotherhood"), "d")
    )
    var board = base.board
    val anime = base.anime.toMutableMap()
    samples.forEach { (id, asset, tierId) ->
        anime[id] = asset
        if (board.entries.none { it.animeId == id }) {
            board = TierListEditor.addAnime(board, id)
            board = TierListEditor.moveAnime(board, id, tierId)
        }
    }
    return base.copy(
        board = board,
        anime = anime
    )
}

private fun recordPositionChanges(
    saved: SavedEditor,
    nextBoard: TierList,
    operation: String
): SavedEditor {
    val events = buildPositionChanges(
        before = saved.board,
        after = nextBoard,
        operation = operation,
        timestamp = System.currentTimeMillis()
    )
    return saved.copy(
        board = nextBoard,
        positionHistory = saved.positionHistory + events
    )
}

private val TIER_COLORS = listOf(
    0xFFE45B5BL, 0xFFFF9848L, 0xFFE7CE56L, 0xFF75B86BL,
    0xFF5DA5CFL, 0xFF9B80C6L, 0xFF777777L, 0xFF333333L
)

@Composable
private fun TierBoardRow(
    modifier: Modifier = Modifier,
    tier: Tier,
    entries: List<TierEntry>,
    assets: Map<String, AnimeAsset>,
    board: TierList,
    onMoveAnime: (String, String?, Int?) -> Unit,
    onRemoveAnime: (String) -> Unit,
    onChoosePoster: (String) -> Unit,
    onShowStats: (String) -> Unit
) {
    val tierColor = Color(tier.colorArgb.toInt())
    Row(
        modifier.fillMaxWidth().fillMaxHeight()
            .background(Color(0xFF1B2028)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.width(48.dp).fillMaxHeight()
                .background(tierColor.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                tier.label,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF101216),
                maxLines = 1
            )
        }

        BoxWithConstraints(
            Modifier.weight(1f).fillMaxHeight()
        ) {
            if (entries.isNotEmpty()) {
                val posterHeight = (maxHeight - 12.dp).coerceAtMost(104.dp).coerceAtLeast(28.dp)
                LazyRow(
                    Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(entries, key = { it.animeId }) { entry ->
                        AnimeTile(
                            entry = entry,
                            asset = assets[entry.animeId] ?: AnimeAsset(entry.animeId),
                            board = board,
                            availableHeight = maxHeight,
                            posterHeight = posterHeight,
                            onMoveAnime = onMoveAnime,
                            onRemoveAnime = onRemoveAnime,
                            onChoosePoster = onChoosePoster,
                            onShowStats = onShowStats
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
    availableHeight: androidx.compose.ui.unit.Dp,
    posterHeight: androidx.compose.ui.unit.Dp,
    onMoveAnime: (String, String?, Int?) -> Unit,
    onRemoveAnime: (String) -> Unit,
    onChoosePoster: (String) -> Unit,
    onShowStats: (String) -> Unit
) {
    var expanded by remember(entry.animeId) { mutableStateOf(false) }
    val inTier = board.entries.filter { it.tierId == entry.tierId }.sortedBy { it.position }
    val index = inTier.indexOfFirst { it.animeId == entry.animeId }
    val tileWidth = (posterHeight * 0.68f).coerceAtLeast(24.dp)

    Box(
        Modifier.width(tileWidth + 8.dp).fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight()
                .clickable { expanded = true }
                .padding(vertical = 3.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PosterPreview(
                asset.posterPath,
                Modifier.width(tileWidth).height(posterHeight)
            )
            if (availableHeight >= 58.dp) {
                Text(
                    asset.title,
                    modifier = Modifier.width(tileWidth + 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFFE8EAF0)
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            board.tiers.filter { it.id != entry.tierId }.sortedBy { it.position }.forEach { tier ->
                DropdownMenuItem(text = { Text("Do " + tier.label) }, onClick = {
                    onMoveAnime(entry.animeId, tier.id, null)
                    expanded = false
                })
            }
            if (entry.tierId != null) {
                DropdownMenuItem(text = { Text("Nieumieszczone") }, onClick = {
                    onMoveAnime(entry.animeId, null, null)
                    expanded = false
                })
            }
            if (index > 0) {
                DropdownMenuItem(text = { Text("Przesuń w lewo") }, onClick = {
                    onMoveAnime(entry.animeId, entry.tierId, index - 1)
                    expanded = false
                })
            }
            if (index >= 0 && index < inTier.lastIndex) {
                DropdownMenuItem(text = { Text("Przesuń w prawo") }, onClick = {
                    onMoveAnime(entry.animeId, entry.tierId, index + 1)
                    expanded = false
                })
            }
            DropdownMenuItem(text = { Text("Statystyki i historia") }, onClick = {
                onShowStats(entry.animeId)
                expanded = false
            })
            DropdownMenuItem(text = { Text("Dodaj/zmień plakat") }, onClick = {
                onChoosePoster(entry.animeId)
                expanded = false
            })
            DropdownMenuItem(text = { Text("Usuń z listy") }, onClick = {
                onRemoveAnime(entry.animeId)
                expanded = false
            })
        }
    }
}

@Composable
private fun PosterPreview(path: String?, modifier: Modifier = Modifier) {
    val bitmap = remember(path) {
        path?.let { runCatching { PosterImages.decode(it) }.getOrNull() }
    }
    if (bitmap != null) {
        Image(bitmap.asImageBitmap(), "Plakat anime", modifier)
    } else {
        Box(
            modifier.background(Color(0xFF303640)),
            contentAlignment = Alignment.Center
        ) {
            Text("—", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9AA2B1))
        }
    }
}
