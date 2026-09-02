package com.rewifi.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.rewifi.app.data.AppLockType
import com.rewifi.app.data.DriveAuth
import com.rewifi.app.data.DriveBackupWorker
import com.rewifi.app.data.SettingsStore
import com.rewifi.app.data.WifiConnector
import com.rewifi.app.data.WifiCred
import com.rewifi.app.ui.components.DuplicateNetworkDialog
import com.rewifi.app.ui.screens.BackupScreen
import com.rewifi.app.ui.screens.DetailScreen
import com.rewifi.app.ui.screens.EditScreen
import com.rewifi.app.ui.screens.IntroScreen
import com.rewifi.app.ui.screens.LockScreen
import com.rewifi.app.ui.screens.NearbyScreen
import com.rewifi.app.ui.screens.PinLockScreen
import com.rewifi.app.ui.screens.QrCustomizeScreen
import com.rewifi.app.ui.screens.NfcWriteScreen
import com.rewifi.app.ui.screens.ScannerScreen
import com.rewifi.app.ui.screens.SettingsScreen
import com.rewifi.app.ui.screens.SetupScreen
import com.rewifi.app.ui.screens.SplashScreen
import com.rewifi.app.ui.screens.VaultScreen
import com.rewifi.app.ui.theme.Paper
import com.rewifi.app.ui.theme.RewifiTheme
import com.rewifi.app.vault.BiometricLock
import com.rewifi.app.vault.SyncState
import com.rewifi.app.vault.VaultViewModel
import java.io.File

private sealed interface Screen {
    data object Vault : Screen
    data object Backup : Screen
    data object Settings : Screen
    data object Setup : Screen
    data object Scan : Screen
    data object Nearby : Screen

    data class Edit(
        val cred: WifiCred?,
        val prefillSsid: String? = null,
        val prefillPass: String? = null
    ) : Screen

    data class Detail(val cred: WifiCred) : Screen
    data class NfcWrite(val cred: WifiCred) : Screen
    data class QrCustomize(val cred: WifiCred) : Screen
}

/**
 * Write the encrypted backup to a private cache file and open the system Share
 * sheet (Drive / Files / Gmail …).
 */
private fun shareBackup(context: Context, bytes: ByteArray) {
    runCatching {
        val dir = File(context.cacheDir, "backups").apply { mkdirs() }
        val file = File(dir, "rewifi-backup.rewifi")

        file.writeBytes(bytes)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(
                send,
                "Save / share REWIFI backup"
            )
        )
    }.onFailure { error ->
        Log.e("REWIFI_BACKUP", "Backup share failed", error)

        Toast.makeText(
            context,
            "Share failed: ${error.message ?: error.javaClass.simpleName}",
            Toast.LENGTH_LONG
        ).show()
    }
}

class MainActivity : FragmentActivity() {

    companion object {
        const val EXTRA_OPEN_SCANNER = "open_scanner"

        private const val TAG_AUTH = "REWIFI_AUTH"
    }

    private val openScanner = mutableStateOf(false)

    private val unlocked = mutableStateOf(false)

    private var backgroundedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        openScanner.value =
            intent?.getBooleanExtra(EXTRA_OPEN_SCANNER, false) == true

        val app = application as RewifiApp
        val autoBackupFile =
            File(filesDir, "auto/rewifi-auto-backup.dat")

        setContent {
            val appTheme by app.settings.appTheme.collectAsState()
            val isDark = appTheme == SettingsStore.THEME_DARK

            RewifiTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = RewifiTheme.colors.background
                ) {
                    val vm: VaultViewModel = viewModel(
                        factory = VaultViewModel.Factory(
                            app.repository,
                            app.settings,
                            applicationContext,
                            autoBackupFile
                        )
                    )

                    AppRoot(vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)

        if (intent.getBooleanExtra(EXTRA_OPEN_SCANNER, false)) {
            openScanner.value = true
        }
    }

    override fun onStop() {
        super.onStop()

        backgroundedAt =
            android.os.SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()

        val settings =
            (application as RewifiApp).settings

        if (
            backgroundedAt != 0L &&
            settings.appLockType.value != AppLockType.OFF
        ) {
            val elapsed =
                android.os.SystemClock.elapsedRealtime() - backgroundedAt

            if (
                elapsed >=
                settings.autoLockMinutes.value * 60_000L
            ) {
                unlocked.value = false
            }
        }
    }

    @Composable
    private fun AppRoot(vm: VaultViewModel) {

        val creds by vm.creds.collectAsState()

        val settings =
            (application as RewifiApp).settings

        /*
         * Google Drive sign-in.
         *
         * IMPORTANT:
         * We intentionally expose the Google Sign-In status code here while
         * debugging the Play Store build.
         */
        val signInLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->

                Log.d(
                    TAG_AUTH,
                    "Sign-in activity returned resultCode=${result.resultCode}"
                )

                runCatching {

                    GoogleSignIn
                        .getSignedInAccountFromIntent(result.data)
                        .getResult(ApiException::class.java)

                }.onSuccess { account ->

                    Log.d(
                        TAG_AUTH,
                        "Google sign-in successful. email=${account.email}"
                    )

                    settings.setDriveEmail(account.email)

                    DriveBackupWorker.schedule(this)

                    vm.onDriveConnected { message ->
                        Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }

                }.onFailure { error ->

                    val apiException =
                        error as? ApiException

                    val statusCode =
                        apiException?.statusCode

                    Log.e(
                        TAG_AUTH,
                        buildString {
                            append("Google sign-in failed")
                            append(" | statusCode=")
                            append(statusCode ?: "N/A")
                            append(" | exception=")
                            append(error.javaClass.simpleName)
                            append(" | message=")
                            append(error.message ?: "null")
                        },
                        error
                    )

                    val userMessage =
                        if (statusCode != null) {

                            "Google sign-in failed — code: $statusCode"

                        } else {

                            "Google sign-in failed — " +
                                    "${error.javaClass.simpleName}: " +
                                    "${error.message ?: "Unknown error"}"
                        }

                    Toast.makeText(
                        this,
                        userMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        val connectDrive: () -> Unit = {

            Log.d(
                TAG_AUTH,
                "Launching Google Sign-In"
            )

            signInLauncher.launch(
                DriveAuth.client(this).signInIntent
            )
        }

        val disconnectDrive: () -> Unit = {

            DriveAuth.client(this).signOut()

            settings.setDriveEmail(null)

            DriveBackupWorker.cancel(this)

            Log.d(
                TAG_AUTH,
                "Google Drive disconnected"
            )
        }

        val syncNow: () -> Unit = {

            vm.syncNow { message ->
                Toast.makeText(
                    this,
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val restoreFromDrive: () -> Unit = {

            vm.restoreFromDrive { message ->
                Toast.makeText(
                    this,
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        /*
         * 1. Splash
         */
        var showSplash by remember {
            mutableStateOf(true)
        }

        if (showSplash) {

            SplashScreen(
                onFinish = {
                    showSplash = false
                }
            )

            return
        }

        /*
         * 2. Intro
         */
        val introDone by
        settings.introDone.collectAsState()

        if (!introDone) {

            IntroScreen(
                onFinish = {
                    settings.setIntroDone(true)
                }
            )

            return
        }

        /*
         * 3. Initial backup setup
         */
        val onboardingDone by
        settings.onboardingDone.collectAsState()

        if (!onboardingDone) {

            val driveEmail by
            settings.driveEmail.collectAsState()

            val lastBackup by
            settings.lastBackupAt.collectAsState()

            SetupScreen(
                firstRun = true,
                driveEmail = driveEmail,
                lastBackupAt = lastBackup,
                onConnectDrive = connectDrive,
                onDisconnectDrive = disconnectDrive,
                onRestoreFromDrive = restoreFromDrive,
                onSyncNow = syncNow,
                onFinish = {
                    settings.setOnboardingDone(true)
                },
                onSkip = {
                    settings.setOnboardingDone(true)
                }
            )

            return
        }

        /*
         * 4. App Lock (PIN / Biometric / PIN + Biometric)
         */
        val appLockType by settings.appLockType.collectAsState()
        val hasPin by settings.hasPin.collectAsState()
        val pinLength by settings.pinLength.collectAsState()
        val clipboardClearSeconds by settings.clipboardClearSeconds.collectAsState()
        val showRecentNetworks by settings.showRecentNetworks.collectAsState()
        val coroutineScope = rememberCoroutineScope()

        if (appLockType != AppLockType.OFF && !unlocked.value) {
            when (appLockType) {
                AppLockType.BIOMETRIC -> {
                    if (BiometricLock.isAvailable(this)) {
                        LockScreen(
                            onUnlock = {
                                BiometricLock.prompt(
                                    this,
                                    onSuccess = {
                                        unlocked.value = true
                                    },
                                    onFail = {
                                        // User cancelled or authentication failed.
                                    }
                                )
                            }
                        )
                        return
                    }
                }
                AppLockType.PIN, AppLockType.PIN_AND_BIOMETRIC -> {
                    PinLockScreen(
                        pinLength = pinLength,
                        allowBiometric = appLockType == AppLockType.PIN_AND_BIOMETRIC && BiometricLock.isAvailable(this),
                        onVerifyPin = { enteredPin ->
                            settings.verifyPin(enteredPin)
                        },
                        onRecordFailedAttempt = {
                            settings.recordFailedAttempt()
                        },
                        getRemainingLockoutSeconds = {
                            settings.getRemainingLockoutSeconds()
                        },
                        onBiometricClick = {
                            if (BiometricLock.isAvailable(this)) {
                                BiometricLock.prompt(
                                    this,
                                    onSuccess = {
                                        unlocked.value = true
                                    },
                                    onFail = {}
                                )
                            }
                        },
                        onSuccess = {
                            unlocked.value = true
                        }
                    )
                    return
                }
                AppLockType.OFF -> {}
            }
        }

        /*
         * 5. Main navigation
         */
        val backStack =
            remember {
                mutableStateListOf<Screen>(
                    Screen.Vault
                )
            }

        val current =
            backStack.last()

        fun navTo(screen: Screen) {
            backStack.add(screen)
        }

        fun pop() {
            if (backStack.size > 1) {
                backStack.removeAt(
                    backStack.lastIndex
                )
            }
        }

        BackHandler(
            enabled = backStack.size > 1
        ) {
            pop()
        }

        /*
         * Widget scanner shortcut.
         */
        LaunchedEffect(
            openScanner.value
        ) {

            if (openScanner.value) {

                if (
                    backStack.last() !is Screen.Scan
                ) {
                    navTo(
                        Screen.Scan
                    )
                }

                openScanner.value = false
            }
        }

        AnimatedContent(
            targetState = current,
            transitionSpec = {

                (
                        slideInHorizontally(
                            tween(160)
                        ) {
                            it / 6
                        } +
                                fadeIn(
                                    tween(160)
                                )
                        ) togetherWith
                        fadeOut(
                            tween(120)
                        )
            },
            label = "nav"
        ) { screen ->

            when (screen) {

                is Screen.Vault -> {
                    val syncState by vm.syncState.collectAsState()
                    val flash by vm.flash.collectAsState()
                    val filteredCreds by vm.filteredCreds.collectAsState()
                    val searchQuery by vm.searchQuery.collectAsState()
                    val filter by vm.vaultFilter.collectAsState()
                    val sort by vm.vaultSort.collectAsState()
                    val categoryFilter by vm.categoryFilter.collectAsState()
                    val customCategories by vm.customCategories.collectAsState()
                    val selectedIds by vm.selectedIds.collectAsState()

                    val backupReminder = remember(
                        creds.size,
                        settings.lastBackupAt.collectAsState().value,
                        settings.lastBackupAttemptAt.collectAsState().value,
                        settings.lastBackupSuccess.collectAsState().value,
                        settings.lastBackupFailureReason.collectAsState().value,
                        settings.vaultChangeCounter.collectAsState().value,
                        settings.lastBackedUpChangeCounter.collectAsState().value,
                        settings.backupRemindersEnabled.collectAsState().value,
                        settings.backupReminderSnoozedUntil.collectAsState().value,
                        settings.backupReminderSnoozedPriority.collectAsState().value,
                        settings.driveEmail.collectAsState().value,
                        settings.autoBackupEnabled.collectAsState().value
                    ) {
                        settings.computeBackupReminder(creds.size)
                    }

                    VaultScreen(
                        creds = filteredCreds,
                        totalCount = creds.size,
                        searchQuery = searchQuery,
                        filter = filter,
                        sort = sort,
                        categoryFilter = categoryFilter,
                        allCategories = vm.getAllCategories(),
                        showRecentNetworks = showRecentNetworks,
                        syncState = syncState,
                        flash = flash,
                        selectedIds = selectedIds,
                        onToggleSelect = { vm.toggleSelect(it) },
                        onSelectAll = { vm.selectAll(filteredCreds.map { it.id }) },
                        onClearSelection = { vm.clearSelection() },
                        onBulkFavorite = { vm.bulkSetFavorite(it) },
                        onBulkCategory = { vm.bulkSetCategory(it) },
                        onBulkDelete = { vm.bulkDelete() },
                        onBulkExport = { passphrase ->
                            coroutineScope.launch {
                                runCatching {
                                    val blob = vm.exportSelected(passphrase)
                                    shareBackup(this@MainActivity, blob)
                                    vm.clearSelection()
                                }.onFailure { err ->
                                    Toast.makeText(this@MainActivity, "Export failed: ${err.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onSearchQueryChange = { vm.setSearchQuery(it) },
                        onFilterChange = { vm.setFilter(it) },
                        onSortChange = { vm.setSort(it) },
                        onCategoryFilterChange = { vm.setCategoryFilter(it) },
                        onClearFilters = { vm.clearFilters() },
                        onToggleFavorite = { vm.toggleFavorite(it.id, !it.isFavorite) },
                        onAdd = { navTo(Screen.Edit(null)) },
                        onOpen = { navTo(Screen.Detail(it)) },
                        onBackup = { navTo(Screen.Backup) },
                        onScan = { navTo(Screen.Scan) },
                        onNearby = { navTo(Screen.Nearby) },
                        onSettings = { navTo(Screen.Settings) },
                        onSync = {
                            if (settings.driveEmail.value == null) {
                                Toast.makeText(this@MainActivity, "Connect Google Drive first", Toast.LENGTH_SHORT).show()
                            } else {
                                vm.triggerSync()
                            }
                        },
                        backupReminder = backupReminder,
                        isBackingUp = (syncState == SyncState.SYNCING),
                        onBackupNow = {
                            if (settings.driveEmail.value == null) {
                                navTo(Screen.Backup)
                            } else {
                                vm.triggerSync()
                            }
                        },
                        onSnoozeReminder = { durationMs, priority ->
                            settings.snoozeReminder(durationMs, priority)
                            Toast.makeText(this@MainActivity, "Reminder snoozed", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                is Screen.Scan -> {
                    var scanDuplicates by remember { mutableStateOf<List<WifiCred>?>(null) }
                    var scannedPending by remember { mutableStateOf<Triple<String, String, String>?>(null) }

                    fun connectAndFlash(sSsid: String, sPass: String, sSec: String) {
                        val result = WifiConnector.connect(this@MainActivity, sSsid, sPass, sSec)
                        when (result) {
                            is WifiConnector.Result.Connected -> {
                                vm.recordConnectionForSsid(sSsid)
                                vm.showFlash("Connected to $sSsid · saved", ok = true)
                            }
                            is WifiConnector.Result.PromptShown -> {
                                vm.showFlash("Saved $sSsid · tap Save to connect", ok = true)
                            }
                            is WifiConnector.Result.Failed -> {
                                vm.showFlash("Saved $sSsid · couldn't auto-connect", ok = false)
                            }
                        }
                    }

                    ScannerScreen(
                        onBack = {
                            pop()
                        },
                        onResult = { ssid, pass, security ->
                            val cleanSsid = ssid.trim()
                            val duplicates = vm.findDuplicates(cleanSsid)
                            if (duplicates.isNotEmpty()) {
                                scannedPending = Triple(cleanSsid, pass, security)
                                scanDuplicates = duplicates
                            } else {
                                pop()
                                vm.saveScanned(cleanSsid, pass)
                                connectAndFlash(cleanSsid, pass, security)
                            }
                        }
                    )

                    if (scanDuplicates != null && scannedPending != null) {
                        val (sSsid, sPass, sSec) = scannedPending!!
                        DuplicateNetworkDialog(
                            newSsid = sSsid,
                            newPassword = sPass,
                            newSecurity = sSec,
                            newCategory = "Other",
                            newNote = null,
                            existingMatches = scanDuplicates!!,
                            onUpdateExisting = { targetCred ->
                                val targetId = targetCred.id
                                scanDuplicates = null
                                scannedPending = null
                                pop()
                                vm.updateExisting(targetId, sPass, null, null) {
                                    connectAndFlash(sSsid, sPass, sSec)
                                }
                            },
                            onSaveAsNew = {
                                scanDuplicates = null
                                scannedPending = null
                                pop()
                                vm.save(0L, sSsid, sPass, null, "Other")
                                connectAndFlash(sSsid, sPass, sSec)
                            },
                            onCancel = {
                                scanDuplicates = null
                                scannedPending = null
                            }
                        )
                    }
                }

                is Screen.Settings -> {

                    val appTheme by
                    settings.appTheme.collectAsState()

                    val autoLock by
                    settings.autoLockMinutes.collectAsState()

                    val driveEmail by
                    settings.driveEmail.collectAsState()

                    val customCategories by
                    vm.customCategories.collectAsState()

                    SettingsScreen(
                        appTheme = appTheme,
                        appLockType = appLockType,
                        hasPin = hasPin,
                        pinLength = pinLength,
                        autoLockMinutes = autoLock,
                        backupConfigured = driveEmail != null,
                        biometricAvailable = BiometricLock.isAvailable(this@MainActivity),
                        customCategories = customCategories,
                        clipboardClearSeconds = clipboardClearSeconds,
                        showRecentNetworks = showRecentNetworks,
                        onBack = { pop() },
                        onSelectTheme = { settings.setAppTheme(it) },
                        onSetAppLockType = { settings.setAppLockType(it) },
                        onSetPin = { pin, len -> settings.setPin(pin, len) },
                        onVerifyPin = { settings.verifyPin(it) },
                        onClearPin = { settings.clearPin() },
                        onSelectClipboardClearSeconds = { settings.setClipboardClearSeconds(it) },
                        onToggleShowRecentNetworks = { settings.setShowRecentNetworks(it) },

                        onCycleAutoLock = {

                            val next =
                                when (
                                    settings
                                        .autoLockMinutes
                                        .value
                                ) {
                                    0 -> 1
                                    1 -> 5
                                    else -> 0
                                }

                            settings
                                .setAutoLockMinutes(
                                    next
                                )
                        },

                        onOpenBackupSetup = {
                            navTo(
                                Screen.Setup
                            )
                        },

                        onCreateCategory = { cat: String ->
                            vm.addCategory(cat)
                        },

                        onRenameCategory = { old: String, new: String ->
                            vm.renameCategory(old, new)
                        },

                        onDeleteCategory = { cat: String ->
                            vm.deleteCategory(cat)
                        },
                        backupRemindersEnabled = settings.backupRemindersEnabled.collectAsState().value,
                        backupNotificationsEnabled = settings.backupNotificationsEnabled.collectAsState().value,
                        backupReminderSnoozedUntil = settings.backupReminderSnoozedUntil.collectAsState().value,
                        onToggleBackupReminders = { settings.setBackupRemindersEnabled(it) },
                        onToggleBackupNotifications = { settings.setBackupNotificationsEnabled(it) },
                        onResetReminderSnooze = { settings.resetReminderSnooze() }
                    )
                }

                is Screen.Setup -> {

                    val driveEmail by
                    settings.driveEmail.collectAsState()

                    val lastBackup by
                    settings.lastBackupAt.collectAsState()

                    SetupScreen(

                        firstRun = false,

                        driveEmail =
                            driveEmail,

                        lastBackupAt =
                            lastBackup,

                        onConnectDrive =
                            connectDrive,

                        onDisconnectDrive =
                            disconnectDrive,

                        onRestoreFromDrive =
                            restoreFromDrive,

                        onSyncNow =
                            syncNow,

                        onFinish = {
                            pop()
                        }
                    )
                }

                is Screen.Backup -> {
                    val context = LocalContext.current

                    BackupScreen(
                        settings = settings,
                        onBack = { pop() },
                        onBackupNow = { onDone ->
                            vm.syncNow(onDone)
                        },
                        onRestoreDrive = { onDone ->
                            vm.restoreFromDrive(onDone)
                        },
                        onVerifyBackup = {
                            vm.verifyCloudBackup()
                        },
                        onFetchCloudMeta = {
                            vm.getCloudBackupMeta()
                        },
                        onExport = { passphrase ->
                            vm.exportBytes(passphrase) { bytes, error ->
                                if (bytes != null) {
                                    shareBackup(context, bytes)
                                } else {
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onImport = { uri, passphrase ->
                            vm.importFrom(context.contentResolver, uri, passphrase) {
                                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }

                is Screen.Edit -> {

                    val customCategories by
                    vm.customCategories.collectAsState()

                    EditScreen(

                        existing =
                            screen.cred,

                        categories =
                            vm.getAllCategories(),

                        onBack = {
                            pop()
                        },

                        onSave = {
                                id,
                                ssid,
                                pass,
                                note,
                                category ->

                            vm.save(
                                id,
                                ssid,
                                pass,
                                note,
                                category
                            )
                        },

                        onCreateCategory = {
                            vm.addCategory(it)
                        },

                        onCheckDuplicates = { ssid, excludeId ->
                            vm.findDuplicates(ssid, excludeId)
                        },

                        onUpdateExisting = { targetId, pass, note, category ->
                            vm.updateExisting(targetId, pass, note, category)
                        },

                        prefillSsid =
                            screen.prefillSsid,

                        prefillPass =
                            screen.prefillPass
                    )
                }

                is Screen.Detail -> {

                    val live =
                        creds.firstOrNull {
                            it.id ==
                                    screen.cred.id
                        }

                    if (live == null) {

                        LaunchedEffect(
                            screen.cred.id
                        ) {
                            pop()
                        }

                    } else {

                        DetailScreen(
                            cred = live,
                            clipboardClearSeconds = clipboardClearSeconds,
                            onBack = {
                                pop()
                            },

                            onEdit = {
                                navTo(
                                    Screen.Edit(
                                        live
                                    )
                                )
                            },

                            onDelete = {
                                vm.delete(
                                    live.id
                                )
                            },

                            onWriteNfc = {
                                navTo(
                                    Screen.NfcWrite(
                                        live
                                    )
                                )
                            },

                            onToggleFavorite = {
                                vm.toggleFavorite(live.id, it)
                            },

                            onConnect = {
                                val result = WifiConnector.connect(this@MainActivity, live.ssid, live.password, "")
                                when (result) {
                                    is WifiConnector.Result.Connected -> {
                                        vm.recordConnection(live.id)
                                        vm.showFlash("Connected to ${live.ssid}", ok = true)
                                    }
                                    is WifiConnector.Result.PromptShown -> {
                                        vm.showFlash("Tap Save to connect", ok = true)
                                    }
                                    is WifiConnector.Result.Failed -> {
                                        vm.showFlash("Couldn't auto-connect: ${result.reason}", ok = false)
                                    }
                                }
                            },
                            onCustomizeQr = {
                                navTo(Screen.QrCustomize(live))
                            }
                        )
                    }
                }

                is Screen.QrCustomize -> {
                    QrCustomizeScreen(
                        cred = screen.cred,
                        settings = settings,
                        onBack = { pop() }
                    )
                }

                is Screen.NfcWrite -> {

                    NfcWriteScreen(

                        ssid =
                            screen.cred.ssid,

                        password =
                            screen.cred.password,

                        security =
                            "WPA",

                        onBack = {
                            pop()
                        }
                    )
                }

                is Screen.Nearby -> {
                    NearbyScreen(
                        vaultCreds = creds,
                        onBack = { pop() },
                        onConnectToNetwork = { ssid, cred ->
                            val result = WifiConnector.connect(this@MainActivity, cred.ssid, cred.password, "")
                            when (result) {
                                is WifiConnector.Result.Connected -> {
                                    vm.recordConnection(cred.id)
                                    vm.showFlash("Connected to ${cred.ssid}", ok = true)
                                }
                                is WifiConnector.Result.PromptShown -> {
                                    vm.showFlash("Tap Save to connect", ok = true)
                                }
                                is WifiConnector.Result.Failed -> {
                                    vm.showFlash("Couldn't auto-connect: ${result.reason}", ok = false)
                                }
                            }
                        },
                        onAddNetwork = { ssid ->
                            navTo(Screen.Edit(cred = null, prefillSsid = ssid))
                        },
                        onOpenCredDetail = { cred ->
                            navTo(Screen.Detail(cred))
                        }
                    )
                }
            }
        }
    }
}