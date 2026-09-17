package dev.supersudoku.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import dev.supersudoku.app.state.BoardPlayground

/** m:ss or h:mm:ss. */
fun formatTime(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0L)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec)
    else "%d:%02d".format(m, sec)
}

/**
 * Screen clock: ticks the VM once per second while the game screen is
 * composed and flushes the save (with elapsed seconds) when leaving.
 */
@Composable
fun rememberGameClock(vm: BoardPlayground) {
    LaunchedEffect(vm) {
        while (true) {
            delay(1000L)
            vm.tickSecond()
        }
    }
    DisposableEffect(vm) {
        onDispose { vm.flush() }
    }
}
