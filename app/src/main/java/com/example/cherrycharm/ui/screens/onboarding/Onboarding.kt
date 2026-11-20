package com.example.cherrycharm.ui.screens.onboarding

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cherrycharm.data.local.PreferencesRepository
import com.example.cherrycharm.data.repository.AuthRepository
import com.example.cherrycharm.ui.components.PrimaryButton
import com.example.cherrycharm.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    suspend fun startRoute(): String = when {
        auth.currentUser != null -> Routes.HOME
        !prefs.isOnboardingDone() -> Routes.ONBOARDING
        else -> Routes.LOGIN
    }

    fun finishOnboarding() {
        viewModelScope.launch { prefs.setOnboardingDone() }
    }
}

// ── Splash ────────────────────────────────────────────────────────────────────

@Composable
fun SplashScreen(onRoute: (String) -> Unit, viewModel: EntryViewModel = hiltViewModel()) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(500)) }
        scale.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        delay(350)
        onRoute(viewModel.startRoute())
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFB5274E), Color(0xFF7D0D30)))
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value).alpha(alpha.value),
        ) {
            Text("🍒", fontSize = 84.sp)
            Spacer(Modifier.height(12.dp))
            Text("CherryCharm", style = MaterialTheme.typography.displaySmall, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(
                "Bags you'll love carrying",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

// ── Onboarding ────────────────────────────────────────────────────────────────

private data class Page(val emoji: String, val title: String, val body: String)

private val pages = listOf(
    Page("👜", "Curated bags, every day", "Totes, clutches, crossbodies and backpacks — handpicked for style that lasts."),
    Page("⚡", "Deals worth the wait", "Flash sales, promo codes and price drops on the bags you've saved."),
    Page("🚚", "Fast, easy, cash on delivery", "Pay with bKash, Nagad, card or cash. Track every order to your door."),
)

@Composable
fun OnboardingScreen(onDone: () -> Unit, viewModel: EntryViewModel = hiltViewModel()) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex
    val finish = {
        viewModel.finishOnboarding()
        onDone()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = finish) { Text("Skip") }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background)
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) { Text(page.emoji, fontSize = 96.sp) }
                Spacer(Modifier.height(40.dp))
                Text(page.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    page.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(pages.size) { i ->
                val selected = i == pagerState.currentPage
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(if (selected) 28.dp else 8.dp)
                        .animateContentSize()
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
        PrimaryButton(
            text = if (isLast) "Get started" else "Next",
            onClick = {
                if (isLast) finish()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
