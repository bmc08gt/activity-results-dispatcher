package dev.bmcreations.dispatcher

import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch

@InternalCoroutinesApi
abstract class ActivityResultContainer<D: ActivityResultDispatcher<*>> : CoroutineScope by CoroutineScope(Dispatchers.Main) {
    fun start() {
        start(null)
    }

    abstract fun start(cb: D? = null)

    fun sendIntent(
        dispatcher: ReactiveActivityResult,
        intent: Intent,
        callback: D?
    ) {
        launch {
            dispatcher.start(intent).collect(object : FlowCollector<ActivityResult> {
                override suspend fun emit(value: ActivityResult) {
                    handleResult(value, callback)
                }
            })
        }
    }
    internal abstract fun handleResult(result: ActivityResult, callback: D?)
}
