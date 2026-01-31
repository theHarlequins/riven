package ua.riven.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScreenTransition
import ua.riven.app.ui.dashboard.DashboardScreen
import ua.riven.app.ui.theme.RivenTheme

@Composable
fun App() {
    RivenTheme {
        Navigator(DashboardScreen()) { navigator ->
            // Phase 4 Fix: SlideTransition for "deck of cards" navigation feel
            // Rationale: Navigation should feel like a "deck of cards," not a strobe light
            SlideTransition(navigator) { screen ->
                screen.Content()
            }
        }
    }
}

/**
 * Custom Slide Transition for Voyager Navigator
 * Implements Material Design motion guidelines for screen transitions
 */
@Composable
fun SlideTransition(
    navigator: Navigator,
    content: @Composable (Screen) -> Unit
) {
    AnimatedContent(
        targetState = navigator.lastItem,
        transitionSpec = {
            // Determine direction based on screen stack depth
            val isForward = navigator.items.indexOf(targetState) > navigator.items.indexOf(initialState)
            
            if (isForward) {
                // Forward navigation: slide in from right, fade out to left
                (slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { -it / 3 },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300)))
            } else {
                // Back navigation: slide in from left, fade out to right
                (slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300)))
            }
        },
        label = "ScreenTransition"
    ) { screen ->
        content(screen)
    }
}
