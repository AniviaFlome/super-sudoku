package dev.supersudoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** One card per variant: what the extra rules are and how to use them. */
@Composable
fun RulesScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(BoardColors.bg)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = BoardColors.given)
            }
            Text("How to play", style = MaterialTheme.typography.titleMedium, color = BoardColors.given)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            Text(
                "This super sudoku puzzle is made up of eight overlapping sudokus. The shared boxes must work for both of the sudokus! You can't solve them individually, you have to do them all together! — carykh",
                style = MaterialTheme.typography.bodyMedium,
                color = BoardColors.dim,
            )
            Spacer(Modifier.height(12.dp))
            for ((i, r) in VARIANT_RULES.withIndex()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BoardColors.cellLine.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "${i + 1}. ${r.name}",
                            style = MaterialTheme.typography.titleMedium,
                            color = BoardColors.entry,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(r.rule, style = MaterialTheme.typography.bodyMedium, color = BoardColors.given)
                        Spacer(Modifier.height(6.dp))
                        Text("Tip: ${r.tip}", style = MaterialTheme.typography.bodySmall, color = BoardColors.dim)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
