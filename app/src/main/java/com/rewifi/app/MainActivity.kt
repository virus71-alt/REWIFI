package com.rewifi.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rewifi.app.data.WifiCred
import com.rewifi.app.ui.screens.BackupScreen
import com.rewifi.app.ui.screens.DetailScreen
import com.rewifi.app.ui.screens.EditScreen
import com.rewifi.app.ui.screens.IntroScreen
import com.rewifi.app.ui.screens.LockScreen
import com.rewifi.app.ui.screens.NfcWriteScreen
import com.rewifi.app.ui.screens.ScannerScreen
import com.rewifi.app.ui.screens.SettingsScreen
import com.rewifi.app.ui.screens.SetupScreen
import com.rewifi.app.ui.screens.SplashScreen
import com.rewifi.app.ui.screens.VaultScreen
import com.rewifi.app.ui.theme.Paper
import com.rewifi.app.ui.theme.RewifiTheme
import com.rewifi.app.vault.BiometricLock
import com.rewifi.app.vault.VaultViewModel
import androidx.core.content.FileProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.rewifi.app.data.DriveAuth
import com.rewifi.app.data.DriveBackupWorker
import com.rewifi.app.data.WifiConnector
import java.io.File

private sealed interface Screen {
    data object Vault : Screen
    data object Backup : Screen
    data object Settings : Screen
    data object Setup : Screen
    data object Scan : Screen
    data class Edit(
        val cred: WifiCred?,
        val prefillSsid: String? = null,
        val prefillPass: String? = null
    ) : Screen
    data class Detail(val cred: WifiCred) : Screen
    data class NfcWrite(val cred: WifiCred) : Screen
}

/**
 * Write the encrypted backup to a private cache file and open the system Share
 * sheet (Drive / Files / Gmail …). Far more reliable than the CreateDocument
 * picker, which is missing on some OEM / emulator builds.
 */
private fun shareBackup(context: Context, bytes: ByteArray) {
    runCatching {
        val dir = File(context.cacheDir, "backups").apply { mkdirs() }
        val file = File(dir, "rewifi-backup.rewifi")
        file.writeBytes(bytes)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Save / share REWIFI backup"))
    }.onFailure {
        Toast.makeText(context, "Share failed: ${it.message}", Toast.LENGTH_LONG).show()
    }
}

class MainActivity : FragmentActivity() {

    companion object {
        /** Set by the home-screen widget to jump straight into the QR scanner. */
        const val EXTRA_OPEN_SCANNER = "open_scanner"
    }

    // Observed by Compose so a widget tap navigates to the scanner even if the
    // activity was already running (delivered via onNewIntent).
    private val openScanner = mutableStateOf(false)

    // Vault unlock state, hoisted so the auto-lock timeout (onStop/onStart) can
    // re-lock it when the app is backgrounded long enough.
    private val unlocked = mutableStateOf(false)
    private var backgroundedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openScanner.value = intent?.getBooleanExtra(EXTRA_OPEN_SCANNER, false) == true
        val app = application as RewifiApp
        val autoBackupFile = File(filesDir, "auto/rewifi-auto-backup.dat")

        setContent {
            RewifiTheme {
                Surface(Modifier.fillMaxSize(), color = Paper) {
                    val vm: VaultViewModel = viewModel(
                        factory = VaultViewModel.Factory(
                            app.repository, app.settings, applicationContext, autoBackupFile
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
        if (intent.getBooleanExtra(EXTRA_OPEN_SCANNER, false)) openScanner.value = true
    }

    override fun onStop() {
        super.onStop()
        backgroundedAt = android.os.SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // Re-lock if the app was in the background longer than the configured window.
        val s = (application as RewifiApp).settings
        if (backgroundedAt != 0L && s.appLockEnabled.value && BiometricLock.isAvailable(this)) {
            val elapsed = android.os.SystemClock.elapsedRealtime() - backgroundedAt
            if (elapsed >= s.autoLockMinutes.value * 60_000L) unlocked.value = false
        }
    }

    @Composable
    private fun AppRoot(vm: VaultViewModel) {
        val creds by vm.creds.collectAsState()
        val settings = (application as RewifiApp).settings

        // Google Drive connect/disconnect/restore wiring (shared by setup + settings).
        val signInLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { res ->
            runCatching {
                GoogleSignIn.getSignedInAccountFromIntent(res.data).getResult(ApiException::class.java)
            }.onSuccess { acct ->
                settings.setDriveEmail(acct.email)
                DriveBackupWorker.schedule(this)
                vm.onDriveConnected { msg -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
            }.onFailure {
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_LONG).show()
            }
        }
        val connectDrive: () -> Unit = { signInLauncher.launch(DriveAuth.client(this).signInIntent) }
        val disconnectDrive: () -> Unit = {
            DriveAuth.client(this).signOut()
            settings.setDriveEmail(null)
            DriveBackupWorker.cancel(this)
        }
        val syncNow: () -> Unit = {
            vm.syncNow { msg -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
        }
        val restoreFromDrive: () -> Unit = {
            vm.restoreFromDrive { msg -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
        }

        // 1) Splash gate — the splash drives its own animated timeline and calls
        //    back when it finishes (or the user taps SKIP).
        var showSplash by remember { mutableStateOf(true) }
        if (showSplash) {
            SplashScreen(onFinish = { showSplash = false })
            return
        }

        // 2) First-run intro — five swipeable cards explaining the app, shown once
        //    after the splash and before backup setup.
        val introDone by settings.introDone.collectAsState()
        if (!introDone) {
            IntroScreen(onFinish = { settings.setIntroDone(true) })
            return
        }

        // 3) First-run setup gate — skippable backup/sync onboarding after the intro.
        val onboardingDone by settings.onboardingDone.collectAsState()
        if (!onboardingDone) {
            val driveEmail by settings.driveEmail.collectAsState()
            val lastBackup by settings.lastBackupAt.collectAsState()
            SetupScreen(
                firstRun = true,
                driveEmail = driveEmail,
                lastBackupAt = lastBackup,
                onConnectDrive = connectDrive,
                onDisconnectDrive = disconnectDrive,
                onRestoreFromDrive = restoreFromDrive,
                onSyncNow = syncNow,
                onFinish = { settings.setOnboardingDone(true) },
                onSkip = { settings.setOnboardingDone(true) }
            )
            return
        }

        // 4) Lock gate — fully optional, off by default, controlled from Settings.
        //    `unlocked` is hoisted to the Activity so the auto-lock timeout can reset it.
        val lockEnabled by settings.appLockEnabled.collectAsState()
        if (lockEnabled && BiometricLock.isAvailable(this) && !unlocked.value) {
            LockScreen(onUnlock = {
                BiometricLock.prompt(this, onSuccess = { unlocked.value = true }, onFail = { })
            })
            return
        }

        // 5) Real back stack so the system Back button pops screens instead of
        //    exiting the app to the home screen.
        val backStack = remember { mutableStateListOf<Screen>(Screen.Vault) }
        val current = backStack.last()
        fun navTo(s: Screen) = backStack.add(s)
        fun pop() { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }

        BackHandler(enabled = backStack.size > 1) { pop() }

        // Widget tap (or cold launch from the widget): jump to the scanner once we're
        // past the splash/onboarding/lock gates. Consumed so it only fires once.
        LaunchedEffect(openScanner.value) {
            if (openScanner.value) {
                if (backStack.last() !is Screen.Scan) navTo(Screen.Scan)
                openScanner.value = false
            }
        }

        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (slideInHorizontally(tween(160)) { it / 6 } + fadeIn(tween(160))) togetherWith
                    (fadeOut(tween(120)))
            },
            label = "nav"
        ) { s ->
            when (s) {
                is Screen.Vault -> {
                    val syncState by vm.syncState.collectAsState()
                    val flash by vm.flash.collectAsState()
                    VaultScreen(
                        creds = creds,
                        syncState = syncState,
                        flash = flash,
                        onAdd = { navTo(Screen.Edit(null)) },
                        onOpen = { navTo(Screen.Detail(it)) },
                        onBackup = { navTo(Screen.Backup) },
                        onScan = { navTo(Screen.Scan) },
                        onSettings = { navTo(Screen.Settings) },
                        onSync = {
                            if (settings.driveEmail.value == null) {
                                Toast.makeText(this@MainActivity, "Connect Google Drive first", Toast.LENGTH_LONG).show()
                            } else {
                                vm.triggerSync()
                            }
                        }
                    )
                }

                is Screen.Scan -> ScannerScreen(
                    onBack = { pop() },
                    onResult = { ssid, pass, security ->
                        // Killer flow: connect the phone, save to the vault, and let it sync.
                        pop()
                        vm.saveScanned(ssid, pass)
                        val result = WifiConnector.connect(this@MainActivity, ssid, pass, security)
                        when (result) {
                            is WifiConnector.Result.Connected ->
                                vm.showFlash("Connected to $ssid · saved", ok = true)
                            is WifiConnector.Result.PromptShown ->
                                vm.showFlash("Saved $ssid · tap Save to connect", ok = true)
                            is WifiConnector.Result.Failed ->
                                vm.showFlash("Saved $ssid · couldn't auto-connect", ok = false)
                        }
                    }
                )

                is Screen.Settings -> {
                    val lock by settings.appLockEnabled.collectAsState()
                    val autoLock by settings.autoLockMinutes.collectAsState()
                    val driveEmail by settings.driveEmail.collectAsState()
                    SettingsScreen(
                        appLock = lock,
                        autoLockMinutes = autoLock,
                        backupConfigured = driveEmail != null,
                        biometricAvailable = BiometricLock.isAvailable(this@MainActivity),
                        onBack = { pop() },
                        onToggleAppLock = { settings.setAppLock(it) },
                        onCycleAutoLock = {
                            // Cycle: leaving the app → 1 min → 5 min → back to leaving.
                            val next = when (settings.autoLockMinutes.value) {
                                0 -> 1
                                1 -> 5
                                else -> 0
                            }
                            settings.setAutoLockMinutes(next)
                        },
                        onOpenBackupSetup = { navTo(Screen.Setup) }
                    )
                }

                is Screen.Setup -> {
                    val driveEmail by settings.driveEmail.collectAsState()
                    val lastBackup by settings.lastBackupAt.collectAsState()
                    SetupScreen(
                        firstRun = false,
                        driveEmail = driveEmail,
                        lastBackupAt = lastBackup,
                        onConnectDrive = connectDrive,
                        onDisconnectDrive = disconnectDrive,
                        onRestoreFromDrive = restoreFromDrive,
                        onSyncNow = syncNow,
                        onFinish = { pop() }
                    )
                }

                is Screen.Backup -> {
                    val ctx = LocalContext.current
                    BackupScreen(
                        onBack = { pop() },
                        onExport = { pass ->
                            vm.exportBytes(pass) { bytes, err ->
                                if (bytes != null) shareBackup(ctx, bytes)
                                else Toast.makeText(ctx, err, Toast.LENGTH_LONG).show()
                            }
                        },
                        onImport = { uri, pass ->
                            vm.importFrom(ctx.contentResolver, uri, pass) {
                                Toast.makeText(ctx, it, Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }

                is Screen.Edit -> EditScreen(
                    existing = s.cred,
                    onBack = { pop() },
                    onSave = { id, ssid, pass, note -> vm.save(id, ssid, pass, note) },
                    prefillSsid = s.prefillSsid,
                    prefillPass = s.prefillPass
                )

                is Screen.Detail -> {
                    // Re-resolve from the live list so edits/deletes reflect instantly.
                    val live = creds.firstOrNull { it.id == s.cred.id }
                    if (live == null) {
                        LaunchedEffect(s.cred.id) { pop() }
                    } else {
                        DetailScreen(
                            cred = live,
                            onBack = { pop() },
                            onEdit = { navTo(Screen.Edit(live)) },
                            onDelete = { vm.delete(live.id) },
                            onWriteNfc = { navTo(Screen.NfcWrite(live)) }
                        )
                    }
                }

                is Screen.NfcWrite -> NfcWriteScreen(
                    ssid = s.cred.ssid,
                    password = s.cred.password,
                    security = "WPA",
                    onBack = { pop() }
                )
            }
        }
    }
}
