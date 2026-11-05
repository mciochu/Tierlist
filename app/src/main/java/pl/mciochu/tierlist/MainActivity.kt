package pl.mciochu.tierlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pl.mciochu.tierlist.domain.TierEntry
import pl.mciochu.tierlist.domain.TierList
import pl.mciochu.tierlist.domain.TierListEditor
import pl.mciochu.tierlist.domain.TierListHistory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TierBoardScreen()
            }
        }
    }
}

@Composable
private fun TierBoardScreen() {
    var history by remember {
        mutableStateOf(TierListHistory(TierList.createDefault("local-board", "Moja tierlista")))
    }
    val names = remember { mutableStateMapOf<String, String>() }
    var newAnimeTitle by remember { mutableStateOf("") }
    var nextAnimeId by remember { mutableIntStateOf(1) }
    val board = history.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(board.name, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Anime Tier Board",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { history = history.undo() },
                        enabled = history.canUndo
                    ) { Text("Cofnij") }
                    OutlinedButton(
                        onClick = { history = history.redo() },
                        enabled = history.canRedo
                    ) { Text("Ponów") }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newAnimeTitle,
                    onValueChange = { newAnimeTitle = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Nazwa anime") }
                )
                Button(
                    onClick = {
                        val title = newAnimeTitle.trim()
                        if (title.isNotEmpty()) {
                            val id = "anime-" + nextAnimeId
                            history = history.apply(TierListEditor.addAnime(board, id))
                            names[id] = title
                            nextAnimeId += 1
                            newAnimeTitle = ""
                        }
                    },
                    enabled = newAnimeTitle.isNotBlank()
                ) { Text("Dodaj") }
            }

            Text(
                board.entries.size.toString() + " pozycji · " + board.tiers.size + " poziomów",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(board.tiers.sortedBy { it.position }, key = { it.id }) { tier ->
                    TierBoardRow(
                        label = tier.label,
                        color = Color(tier.colorArgb.toInt()),
                        tierId = tier.id,
                        entries = board.entries.filter { it.tierId == tier.id }
                            .sortedBy { it.position },
                        board = board,
                        names = names,
                        onMove = { animeId, targetTierId, position ->
                            history = history.apply(
                                TierListEditor.moveAnime(board, animeId, targetTierId, position)
                            )
                        },
                        onRemove = { animeId ->
                            history = history.apply(TierListEditor.removeAnime(board, animeId))
                        }
                    )
                }

                item(key = "unranked") {
                    TierBoardRow(
                        label = "Do umieszczenia",
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tierId = null,
                        entries = board.entries.filter { it.tierId == null }
                            .sortedBy { it.position },
                        board = board,
                        names = names,
                        onMove = { animeId, targetTierId, position ->
                            history = history.apply(
                                TierListEditor.moveAnime(board, animeId, targetTierId, position)
                            )
                        },
                        onRemove = { animeId ->
                            history = history.apply(TierListEditor.removeAnime(board, animeId))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TierBoardRow(
    label: String,
    color: Color,
    tierId: String?,
    entries: List<TierEntry>,
    board: TierList,
    names: Map<String, String>,
    onMove: (String, String?, Int?) -> Unit,
    onRemove: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(
                    entries.size.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entries.isEmpty()) {
                Text(
                    "Dodaj anime, aby wypełnić ten poziom",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(entries, key = { it.animeId }) { entry ->
                        TierEntryChip(
                            entry = entry,
                            title = names[entry.animeId] ?: entry.animeId,
                            tierId = tierId,
                            board = board,
                            onMove = onMove,
                            onRemove = onRemove
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TierEntryChip(
    entry: TierEntry,
    title: String,
    tierId: String?,
    board: TierList,
    onMove: (String, String?, Int?) -> Unit,
    onRemove: (String) -> Unit
) {
    var expanded by remember(entry.animeId) { mutableStateOf(false) }
    val currentEntries = board.entries
        .filter { it.tierId == tierId }
        .sortedBy { it.position }
    val currentPosition = currentEntries.indexOfFirst { it.animeId == entry.animeId }

    Column {
        OutlinedButton(onClick = { expanded = true }) {
            Text(title, maxLines = 2)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            board.tiers.sortedBy { it.position }.filter { it.id != tierId }.forEach { tier ->
                DropdownMenuItem(
                    text = { Text("Przenieś do " + tier.label) },
                    onClick = {
                        onMove(entry.animeId, tier.id, null)
                        expanded = false
                    }
                )
            }
            if (tierId != null) {
                DropdownMenuItem(
                    text = { Text("Do nieumieszczonych") },
                    onClick = {
                        onMove(entry.animeId, null, null)
                        expanded = false
                    }
                )
            }
            if (currentPosition > 0) {
                DropdownMenuItem(
                    text = { Text("Przesuń w lewo") },
                    onClick = {
                        onMove(entry.animeId, tierId, currentPosition - 1)
                        expanded = false
                    }
                )
            }
            if (currentPosition >= 0 && currentPosition < currentEntries.lastIndex) {
                DropdownMenuItem(
                    text = { Text("Przesuń w prawo") },
                    onClick = {
                        onMove(entry.animeId, tierId, currentPosition + 1)
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Usuń z tierlisty") },
                onClick = {
                    onRemove(entry.animeId)
                    expanded = false
                }
            )
        }
    }
}
