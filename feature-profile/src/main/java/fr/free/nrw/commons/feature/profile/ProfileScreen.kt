package fr.free.nrw.commons.feature.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(1) } // Default to Leaderboard
    val tabs = listOf("Achievements", "Leaderboard", "Contributions")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(uiState.userName) })
        }
    ) {
        paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
//                Image(
//                    // Using app's vector drawable instead of system drawable
//                    painter = painterResource(id = fr.free.nrw.commons.feature.profile.R.drawable.ic_user_avatar),
//                    contentDescription = "User Avatar",
//                    modifier = Modifier
//                        .size(100.dp)
//                        .clip(CircleShape)
//                )
                Column {
                    Text(text = "Rank: ${uiState.userRank}", fontSize = 18.sp)
                    Text(text = "Contributions: ${uiState.userContributions}", fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTabIndex == index, onClick = { selectedTabIndex = index }) {
                        Text(text = title, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTabIndex) {
                0 -> Text("Achievements will be shown here")
                1 -> Leaderboard(leaderboard = uiState.leaderboard)
                2 -> Text("Contributions will be shown here")
            }
        }
    }
}