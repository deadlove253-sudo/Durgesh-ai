package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AssistantTab
import com.example.ui.AssistantViewModel
import com.example.ui.components.PermissionRationaleDialog
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.CommandsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CosmicNavy
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepVioletContainer
import com.example.ui.theme.DurgeshAITheme
import com.example.ui.theme.MidnightVoid
import com.example.ui.theme.MistBlue
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StarlightWhite

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
    )
    setContent {
      DurgeshAITheme {
        DurgeshAiRoot(activity = this@MainActivity)
      }
    }
  }
}

@Composable
fun DurgeshAiRoot(
  activity: ComponentActivity?,
  viewModel: AssistantViewModel = viewModel(),
) {
  var currentTab by rememberSaveable { mutableStateOf(AssistantTab.ASSISTANT) }

  val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
  val voiceHistory by viewModel.voiceHistory.collectAsStateWithLifecycle()
  val preferences by viewModel.preferences.collectAsStateWithLifecycle()
  val listeningState by viewModel.listeningState.collectAsStateWithLifecycle()
  val capabilities by viewModel.capabilities.collectAsStateWithLifecycle()
  val deviceStatus by viewModel.deviceStatus.collectAsStateWithLifecycle()
  val micGranted by viewModel.micGranted.collectAsStateWithLifecycle()
  val cameraGranted by viewModel.cameraGranted.collectAsStateWithLifecycle()
  val permissionDialog by viewModel.permissionDialog.collectAsStateWithLifecycle()
  val commandFilter by viewModel.commandFilter.collectAsStateWithLifecycle()

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refreshSystemStatus()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val permissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
      val activePermission = permissionDialog?.permission
      if (activePermission != null) {
        val shouldShowRationale =
          activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, activePermission)
          } ?: true
        viewModel.onPermissionResult(activePermission, granted, shouldShowRationale)
      } else {
        viewModel.refreshSystemStatus()
      }
    }

  BackHandler(enabled = currentTab != AssistantTab.ASSISTANT) {
    currentTab = AssistantTab.ASSISTANT
  }

  permissionDialog?.let { dialogState ->
    PermissionRationaleDialog(
      state = dialogState,
      onAllowClick = { permissionLauncher.launch(dialogState.permission) },
      onOpenSettingsClick = { viewModel.openAppSettings() },
      onDismiss = { viewModel.dismissPermissionDialog() },
    )
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    if (useNavRail) {
      Scaffold(
        containerColor = MidnightVoid,
        contentWindowInsets = WindowInsets.safeDrawing,
      ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          NavigationRail(
            containerColor = CosmicNavy,
            contentColor = StarlightWhite,
          ) {
            AssistantTab.entries.forEach { tab ->
              val selected = currentTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = { currentTab = tab },
                icon = {
                  Icon(
                    imageVector = tabIcon(tab, selected),
                    contentDescription = tabLabel(tab),
                  )
                },
                label = { Text(tabLabel(tab)) },
                colors =
                  NavigationRailItemDefaults.colors(
                    selectedIconColor = CyberCyan,
                    selectedTextColor = StarlightWhite,
                    indicatorColor = DeepVioletContainer,
                    unselectedIconColor = MistBlue,
                    unselectedTextColor = MistBlue,
                  ),
                modifier = Modifier.testTag("nav_tab_${tab.name}"),
              )
            }
          }

          TabContent(
            currentTab = currentTab,
            viewModel = viewModel,
            chatMessages = chatMessages,
            voiceHistory = voiceHistory,
            preferences = preferences,
            listeningState = listeningState,
            capabilities = capabilities,
            deviceStatus = deviceStatus,
            micGranted = micGranted,
            cameraGranted = cameraGranted,
            commandFilter = commandFilter,
            onNavigateToAssistant = { currentTab = AssistantTab.ASSISTANT },
            modifier = Modifier.weight(1f).fillMaxSize(),
          )
        }
      }
    } else {
      Scaffold(
        containerColor = MidnightVoid,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
          NavigationBar(
            containerColor = CosmicNavy,
            contentColor = StarlightWhite,
            windowInsets = WindowInsets.navigationBars,
          ) {
            AssistantTab.entries.forEach { tab ->
              val selected = currentTab == tab
              NavigationBarItem(
                selected = selected,
                onClick = { currentTab = tab },
                icon = {
                  Icon(
                    imageVector = tabIcon(tab, selected),
                    contentDescription = tabLabel(tab),
                  )
                },
                label = { Text(tabLabel(tab)) },
                colors =
                  NavigationBarItemDefaults.colors(
                    selectedIconColor = CyberCyan,
                    selectedTextColor = StarlightWhite,
                    indicatorColor = DeepVioletContainer,
                    unselectedIconColor = MistBlue,
                    unselectedTextColor = MistBlue,
                  ),
                modifier = Modifier.testTag("nav_tab_${tab.name}"),
              )
            }
          }
        },
      ) { innerPadding ->
        TabContent(
          currentTab = currentTab,
          viewModel = viewModel,
          chatMessages = chatMessages,
          voiceHistory = voiceHistory,
          preferences = preferences,
          listeningState = listeningState,
          capabilities = capabilities,
          deviceStatus = deviceStatus,
          micGranted = micGranted,
          cameraGranted = cameraGranted,
          commandFilter = commandFilter,
          onNavigateToAssistant = { currentTab = AssistantTab.ASSISTANT },
          modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
      }
    }
  }
}

@Composable
private fun TabContent(
  currentTab: AssistantTab,
  viewModel: AssistantViewModel,
  chatMessages: List<com.example.data.local.ChatMessageEntity>,
  voiceHistory: List<com.example.data.local.VoiceCommandHistoryEntity>,
  preferences: com.example.data.local.AssistantPreferences,
  listeningState: com.example.domain.ListeningState,
  capabilities: com.example.system.VoiceEngineCapabilities,
  deviceStatus: com.example.system.DeviceStatusSnapshot,
  micGranted: Boolean,
  cameraGranted: Boolean,
  commandFilter: String,
  onNavigateToAssistant: () -> Unit,
  modifier: Modifier = Modifier,
) {
  when (currentTab) {
    AssistantTab.ASSISTANT ->
      AssistantScreen(
        messages = chatMessages,
        listeningState = listeningState,
        deviceStatus = deviceStatus,
        capabilities = capabilities,
        micGranted = micGranted,
        onMicClick = { viewModel.onMicButtonTapped() },
        onSendTypedCommand = { viewModel.submitTypedCommand(it) },
        onQuickCommand = { viewModel.runQuickCommand(it) },
        onReplayAudio = { viewModel.speakMessage(it) },
        onRequestMicPermission = {
          viewModel.requestPermissionPrompt(android.Manifest.permission.RECORD_AUDIO)
        },
        onClearChat = { viewModel.clearChatHistory() },
        modifier = modifier,
      )

    AssistantTab.COMMANDS ->
      CommandsScreen(
        voiceHistory = voiceHistory,
        selectedFilter = commandFilter,
        onSelectFilter = { viewModel.setCommandFilter(it) },
        onLaunchApp = {
          viewModel.launchAppDirect(it)
          onNavigateToAssistant()
        },
        onOpenSettings = {
          viewModel.openSettingsDirect(it)
          onNavigateToAssistant()
        },
        onRunCommand = {
          viewModel.runQuickCommand(it)
          onNavigateToAssistant()
        },
        onDeleteHistoryItem = { viewModel.deleteVoiceHistoryItem(it) },
        onClearHistory = { viewModel.clearVoiceHistory() },
        modifier = modifier,
      )

    AssistantTab.SETTINGS ->
      SettingsScreen(
        preferences = preferences,
        capabilities = capabilities,
        deviceStatus = deviceStatus,
        micGranted = micGranted,
        cameraGranted = cameraGranted,
        onToggleAutoSpeak = { viewModel.setAutoSpeakHindi(it) },
        onTogglePreferOffline = { viewModel.setPreferOfflineSpeech(it) },
        onSpeechRateChange = { viewModel.setSpeechRate(it) },
        onRequestPermission = { perm -> viewModel.requestPermissionPrompt(perm) },
        onOpenAppSettings = { viewModel.openAppSettings() },
        onOpenSystemSettings = { viewModel.openSettingsDirect(it) },
        onClearChatHistory = { viewModel.clearChatHistory() },
        onClearVoiceHistory = { viewModel.clearVoiceHistory() },
        modifier = modifier,
      )
  }
}

@Composable
private fun tabLabel(tab: AssistantTab): String {
  return when (tab) {
    AssistantTab.ASSISTANT -> stringResource(R.string.tab_assistant)
    AssistantTab.COMMANDS -> stringResource(R.string.tab_commands)
    AssistantTab.SETTINGS -> stringResource(R.string.tab_settings)
  }
}

private fun tabIcon(tab: AssistantTab, selected: Boolean) =
  when (tab) {
    AssistantTab.ASSISTANT -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
    AssistantTab.COMMANDS -> if (selected) Icons.Filled.History else Icons.Outlined.History
    AssistantTab.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
  }
