package dev.bmcreations.dispatcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

@Parcelize
data class ActivityResult(val resultCode: Int, val data: Intent?) : Parcelable
fun ActivityResult.isOk() = resultCode == Activity.RESULT_OK
fun ActivityResult.isCanceled() = resultCode == Activity.RESULT_CANCELED
fun ActivityResult.isFirstUser() = resultCode == Activity.RESULT_FIRST_USER

class ReactiveActivityResult(context: Context) {

    companion object {
        const val TAG = "dev.bmcreations.dispatcher.fragment"
    }

    private var fragment: ReactiveActivityResultFragment

    init {
        fragment = with(context) {
            val fm = requireNotNull(when (context) {
                is FragmentActivity -> context.supportFragmentManager
                is Fragment -> context.childFragmentManager
                else -> null
            }, { "context isn't a Fragment or FragmentActivity" })

            var attachedFragment = fm.findFragmentByTag(TAG) as? ReactiveActivityResultFragment

            if (attachedFragment == null) {
                attachedFragment = ReactiveActivityResultFragment()
                fm.beginTransaction().add(attachedFragment, TAG).commitAllowingStateLoss()
                fm.executePendingTransactions()
            }

            attachedFragment
        }
    }

    fun start(intent: Intent): Flow<ActivityResult> = fragment.start(intent)
}

class ReactiveActivityResultFragment : Fragment(), CoroutineScope by CoroutineScope(Dispatchers.Main) {

    private val channel = BroadcastChannel<Pair<Int, ActivityResult>>(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        launch {
            channel.send(Pair(requestCode, ActivityResult(resultCode, data)))
        }
    }

    fun start(intent: Intent): Flow<ActivityResult> {
        val requestCode = RequestCodeGenerator.generate()
        startActivityForResult(intent, requestCode)

        return flow {
            coroutineScope {
                channel.consumeEach {
                    if (it.first == requestCode) {
                        emit(it.second)
                    }
                }
            }
        }
    }
}

private object RequestCodeGenerator {
    private val seed: AtomicInteger = AtomicInteger(500)
    fun generate(): Int {
        return seed.incrementAndGet()
    }
}
