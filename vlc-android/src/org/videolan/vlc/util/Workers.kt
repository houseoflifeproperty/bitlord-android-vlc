package org.videolan.vlc.util

import android.os.Looper
import kotlinx.coroutines.*

fun runBackground(runnable: Runnable) {
    if (Looper.myLooper() != Looper.getMainLooper()) runnable.run()
    else AppScope.launch(Dispatchers.Default) { runnable.run() }
}

fun runOnMainThread(runnable: Runnable) {
    AppScope.launch { runnable.run() }
}

fun runIO(runnable: Runnable) {
    AppScope.launch(Dispatchers.IO) { runnable.run() }
}

object AppScope : CoroutineScope {
    @ExperimentalCoroutinesApi
    override val coroutineContext = Dispatchers.Main.immediate + SupervisorJob()
}