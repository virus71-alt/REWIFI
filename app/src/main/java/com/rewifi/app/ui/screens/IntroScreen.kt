package com.rewifi.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewifi.app.ui.components.BrutalButton
import com.rewifi.app.ui.theme.Blue
import com.rewifi.app.ui.theme.Green
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Muted
import com.rewifi.app.ui.theme.Paper
import com.rewifi.app.ui.theme.Snow
import com.rewifi.app.ui.theme.Yellow
import kotlinx.coroutines.launch

private data class IntroPage(
    val icon: ImageVector,
    val accent: Color,
    val iconTint: Color,
    val title: String,
    val body: String,
)

private val PAGES = listOf(
    IntroPage(
        Icons.Default.Lock, Yellow, Ink,
        "ENCRYPTED\nVAULT",
        "Your passwords are sealed with AES-256 on your device. Only ciphertext ever touches the database."
    ),
    IntroPage(
        Icons.Default.QrCode2, Blue, Snow,
        "SCAN TO\nCONNECT",
        "Every saved network gets a QR code. Point any phone's camera at it to join instantly — no typing."
    ),
    IntroPage(
        Icons.Default.Nfc, Green, Ink,
        "TAP TO\nSHARE",
        "Write a network to an NFC tag and hand off WiFi with a single tap. Stick one on the fridge."
    ),
    IntroPage(
        Icons.Default.CloudDone, Yellow, Ink,
        "SURVIVES\nRESETS",
        "Encrypted backups to your Google Drive mean a factory reset, crash, or new phone never costs you a password."
    ),
    IntroPage(
        Icons.Default.RocketLaunch, Ink, Yellow,
        "READY\nTO GO",
        "Save your first network and never walk into a cafe and lose your WiFi again."
    ),
)

/**
 * First-launch walkthrough: five swipeable brutalist cards that explain the app,
 * shown once after the splash and before the backup setup. [onFinish] fires from
 * the final "GET STARTED" button or the SKIP shortcut.
 */
@Composable
fun IntroScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == PAGES.lastIndex

    BackHandler { onFinish() }

    Box(Modifier.fillMaxSize().background(Paper).systemBarsPadding()) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            // Top row: brand chip + skip.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(Yellow)
                        .border(3.dp, Ink, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("REWIFI", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Ink) }
                Spacer(Modifier.weight(1f))
                if (!last) {
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).clickable { onFinish() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) { Text("SKIP", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Muted) }
                }
            }

            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                PageContent(PAGES[page])
            }

            // Page indicator.
            Row(
                Modifier.fillMaxWidth().padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(PAGES.size) { i ->
                    val active = i == pager.currentPage
                    val w by animateDpAsState(if (active) 26.dp else 10.dp, label = "dotW")
                    Box(
                        Modifier.padding(horizontal = 3.dp).width(w).height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (active) Ink else Snow)
                            .border(2.dp, Ink, RoundedCornerShape(5.dp))
                    )
                }
            }

            BrutalButton(
                if (last) "GET STARTED" else "NEXT",
                modifier = Modifier.fillMaxWidth(), bg = Ink, fg = Yellow
            ) {
                if (last) onFinish()
                else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            }
        }
    }
}

@Composable
private fun PageContent(page: IntroPage) {
    Column(
        Modifier.fillMaxSize().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon badge with the brutalist hard offset shadow.
        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(132.dp).rotate(-6f).padding(start = 9.dp, top = 9.dp)
                    .clip(RoundedCornerShape(30.dp)).background(Ink)
            )
            Box(
                Modifier.size(132.dp).rotate(-6f)
                    .clip(RoundedCornerShape(30.dp)).background(page.accent)
                    .border(4.dp, Ink, RoundedCornerShape(30.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(page.icon, null, tint = page.iconTint, modifier = Modifier.size(68.dp))
            }
        }

        Spacer(Modifier.height(36.dp))
        Text(
            page.title, fontWeight = FontWeight.Black, fontSize = 38.sp, color = Ink,
            lineHeight = 40.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            page.body, color = Muted, fontWeight = FontWeight.Medium, fontSize = 15.sp,
            lineHeight = 22.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
