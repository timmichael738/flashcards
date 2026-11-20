package com.kotlinapps.flashcards.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlinapps.flashcards.core.designsystem.component.LoadingState
import com.kotlinapps.flashcards.core.designsystem.theme.FlashcardsTheme
import com.kotlinapps.flashcards.core.designsystem.theme.supportsDynamicColor
import com.kotlinapps.flashcards.core.model.ThemeMode
import com.kotlinapps.flashcards.core.model.UserPreferences
import com.kotlinapps.flashcards.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SettingsRoute(
    onNavigateUp: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        showDynamicColorOption = supportsDynamicColor,
        onThemeModeChange = viewModel::onThemeModeChange,
        onDynamicColorChange = viewModel::onDynamicColorChange,
        onQuestionsPerQuizChange = viewModel::onQuestionsPerQuizChange,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    showDynamicColorOption: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onQuestionsPerQuizChange: (Int) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), stringResource(Res.string.navigate_up))
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState) {
                SettingsUiState.Loading -> LoadingState()
                is SettingsUiState.Success -> SettingsContent(
                    preferences = uiState.preferences,
                    showDynamicColorOption = showDynamicColorOption,
                    onThemeModeChange = onThemeModeChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onQuestionsPerQuizChange = onQuestionsPerQuizChange,
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    preferences: UserPreferences,
    showDynamicColorOption: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onQuestionsPerQuizChange: (Int) -> Unit,
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(stringResource(Res.string.questions_per_quiz))
        Text(
            stringResource(Res.string.questions_per_quiz_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Column(Modifier.selectableGroup()) {
            UserPreferences.QuestionsPerQuizOptions.forEach { count ->
                RadioItem(
                    label = stringResource(Res.string.questions_count, count),
                    selected = count == preferences.questionsPerQuiz,
                    onClick = { onQuestionsPerQuizChange(count) },
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionHeader(stringResource(Res.string.theme_label))
        Column(Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                RadioItem(
                    label = stringResource(
                        when (mode) {
                            ThemeMode.SYSTEM -> Res.string.theme_system
                            ThemeMode.LIGHT -> Res.string.theme_light
                            ThemeMode.DARK -> Res.string.theme_dark
                        },
                    ),
                    selected = mode == preferences.themeMode,
                    onClick = { onThemeModeChange(mode) },
                )
            }
        }
        if (showDynamicColorOption) {
            ListItem(
                headlineContent = { Text(stringResource(Res.string.dynamic_color)) },
                supportingContent = { Text(stringResource(Res.string.dynamic_color_summary)) },
                trailingContent = { Switch(checked = preferences.useDynamicColor, onCheckedChange = null) },
                modifier = Modifier.toggleable(
                    value = preferences.useDynamicColor,
                    role = Role.Switch,
                    onValueChange = onDynamicColorChange,
                ),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun RadioItem(label: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    FlashcardsTheme(dynamicColor = false) {
        SettingsScreen(
            uiState = SettingsUiState.Success(UserPreferences(themeMode = ThemeMode.DARK)),
            showDynamicColorOption = true,
            onThemeModeChange = {},
            onDynamicColorChange = {},
            onQuestionsPerQuizChange = {},
            onNavigateUp = {},
        )
    }
}
