package fr.free.nrw.commons.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Leaderboard(leaderboard: List<LeaderboardUser>) {
    LazyColumn {
        items(leaderboard) { user ->
            LeaderboardItem(user = user)
        }
    }
}

@Composable
fun LeaderboardItem(user: LeaderboardUser) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "${user.rank}. ${user.userName}")
        Text(text = user.score.toString())
    }
}