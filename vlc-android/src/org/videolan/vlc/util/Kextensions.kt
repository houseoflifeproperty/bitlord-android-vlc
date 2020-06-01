package org.videolan.vlc.util

import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.widget.TextView
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.FileProvider
import androidx.core.text.PrecomputedTextCompat
import androidx.core.widget.TextViewCompat
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import org.videolan.libvlc.Media
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.media.MediaWrapper.TYPE_ALL
import org.videolan.medialibrary.interfaces.media.MediaWrapper.TYPE_VIDEO
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.tools.*
import org.videolan.tools.Settings.showTvUi
import org.videolan.vlc.R
import org.videolan.vlc.gui.tv.browser.REQUEST_CODE_NO_CONNECTION
import org.videolan.vlc.gui.tv.dialogs.ConfirmationTvActivity
import org.videolan.vlc.startMedialibrary
import java.io.File
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import kotlin.coroutines.resume


//object Settings : SingletonHolder<SharedPreferences, Context>({ PreferenceManager.getDefaultSharedPreferences(it) })

fun String.validateLocation(): Boolean {
    var location = this
    /* Check if the MRL contains a scheme */
    if (!location.matches("\\w+://.+".toRegex())) location = "file://$location"
    if (location.toLowerCase(Locale.ENGLISH).startsWith("file://")) {
        /* Ensure the file exists */
        val f: File
        try {
            f = File(URI(location))
        } catch (e: URISyntaxException) {
            return false
        } catch (e: IllegalArgumentException) {
            return false
        }
        if (!f.isFile) return false
    }
    return true
}

inline fun <reified T : ViewModel> Fragment.getModelWithActivity() = ViewModelProviders.of(requireActivity()).get(T::class.java)
inline fun <reified T : ViewModel> Fragment.getModel() = ViewModelProviders.of(this).get(T::class.java)
inline fun <reified T : ViewModel> FragmentActivity.getModel() = ViewModelProviders.of(this).get(T::class.java)

fun Media?.canExpand() = this != null && (type == IMedia.Type.Directory || type == IMedia.Type.Playlist)
suspend fun AppCompatActivity.share(media: MediaWrapper) {
    val intentShareFile = Intent(Intent.ACTION_SEND)
    val fileWithinMyDir = File(media.uri.path)
    val validFile = withContext(Dispatchers.IO) {
        fileWithinMyDir.exists()
    }

    if (isStarted())
        if (validFile) {
            intentShareFile.type = if (media.type == TYPE_VIDEO) "video/*" else "audio/*"
            intentShareFile.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, packageName + ".provider", fileWithinMyDir))
            intentShareFile.putExtra(Intent.EXTRA_SUBJECT, title)
            intentShareFile.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message))
            startActivity(Intent.createChooser(intentShareFile, getString(R.string.share_file, title)))
        } else Snackbar.make(findViewById(android.R.id.content), R.string.invalid_file, Snackbar.LENGTH_LONG).show()
}

fun MediaWrapper?.isMedia() = this != null && (type == MediaWrapper.TYPE_AUDIO || type == MediaWrapper.TYPE_VIDEO)
fun MediaWrapper?.isBrowserMedia() = this != null && (isMedia() || type == MediaWrapper.TYPE_DIR || type == MediaWrapper.TYPE_PLAYLIST)

fun Context.getAppSystemService(name: String) = applicationContext.getSystemService(name)!!

fun Long.random() = (Random().nextFloat() * this).toLong()

@ExperimentalCoroutinesApi
suspend inline fun <reified T> Context.getFromMl(crossinline block: Medialibrary.() -> T) = withContext(Dispatchers.IO) {
    val ml = Medialibrary.getInstance()
    if (ml.isStarted) block.invoke(ml)
    else {
        val scan = Settings.getInstance(this@getFromMl).getInt(KEY_MEDIALIBRARY_SCAN, ML_SCAN_ON) == ML_SCAN_ON
        suspendCancellableCoroutine { continuation ->
            val listener = object : Medialibrary.OnMedialibraryReadyListener {
                override fun onMedialibraryReady() {
                    val cb = this
                    if (!continuation.isCompleted) launch(start = CoroutineStart.UNDISPATCHED) {
                        continuation.resume(block.invoke(ml))
                        yield()
                        ml.removeOnMedialibraryReadyListener(cb)
                    }
                }
                override fun onMedialibraryIdle() {}
            }
            continuation.invokeOnCancellation { ml.removeOnMedialibraryReadyListener(listener) }
            ml.addOnMedialibraryReadyListener(listener)
            startMedialibrary(false, false, scan)
        }
    }
}

suspend fun Context.awaitMedialibraryStarted() = getFromMl { isStarted }

@WorkerThread
fun List<MediaWrapper>.updateWithMLMeta() : MutableList<MediaWrapper> {
    val ml = Medialibrary.getInstance()
    val list = mutableListOf<MediaWrapper>()
    for (media in this) {
        list.add(ml.findMedia(media).apply {
            if (type == TYPE_ALL) type = media.type
        })
    }
    return list
}

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
suspend fun String.scanAllowed() = withContext(Dispatchers.IO) {
    val file = File(Uri.parse(this@scanAllowed).path ?: return@withContext false)
    if (!file.exists() || !file.canRead()) return@withContext false
    if (AndroidDevices.watchDevices && file.list()?.any { it == ".nomedia" } == true) return@withContext false
    true
}

fun <X, Y> CoroutineScope.map(
        source: LiveData<X>,
        f : suspend (value: X?) -> Y
): LiveData<Y> {
    return MediatorLiveData<Y>().apply {
        addSource(source) {
            launch { value = f(it) }
        }
    }
}

@BindingAdapter("app:asyncText", requireAll = false)
fun asyncText(view: TextView, text: CharSequence?) {
    if (text.isNullOrEmpty()) {
        view.visibility = View.GONE
        return
    }
    view.visibility = View.VISIBLE
    val params = TextViewCompat.getTextMetricsParams(view)
    (view as AppCompatTextView).setTextFuture(PrecomputedTextCompat.getTextFuture(text, params, null))
}

@BindingAdapter("app:asyncText", requireAll = false)
fun asyncTextItem(view: TextView, item: MediaLibraryItem?) {
    if (item == null) {
        view.visibility = View.GONE
        return
    }
    val text = if (item.itemType == MediaLibraryItem.TYPE_PLAYLIST) view.context.getString(R.string.track_number, item.tracksCount) else item.description
    if (text.isNullOrEmpty()) {
        view.visibility = View.GONE
        return
    }
    view.visibility = View.VISIBLE
    val params = TextViewCompat.getTextMetricsParams(view)
    (view as AppCompatTextView).setTextFuture(PrecomputedTextCompat.getTextFuture(text, params, null))
}

fun isAppStarted() = ProcessLifecycleOwner.get().isStarted()

fun Int.toPixel(): Int {
    val metrics = Resources.getSystem().displayMetrics
    val px = toFloat() * (metrics.densityDpi / 160f)
    return Math.round(px)
}

fun Activity.getScreenWidth() : Int {
    val dm = DisplayMetrics().also { windowManager.defaultDisplay.getMetrics(it) }
    return dm.widthPixels
}

fun Activity.getScreenHeight(): Int {
    val dm = DisplayMetrics().also { windowManager.defaultDisplay.getMetrics(it) }
    return dm.heightPixels
}

@TargetApi(Build.VERSION_CODES.O)
fun Context.getPendingIntent(iPlay: Intent): PendingIntent {
    return if (AndroidUtil.isOOrLater) PendingIntent.getForegroundService(applicationContext, 0, iPlay, PendingIntent.FLAG_UPDATE_CURRENT)
    else PendingIntent.getService(applicationContext, 0, iPlay, PendingIntent.FLAG_UPDATE_CURRENT)
}

fun generateResolutionClass(width: Int, height: Int) : String? = if (width <= 0 || height <= 0) {
    null
} else when {
    width >= 7680 -> "8K"
    width >= 3840 -> "4K"
    width >= 1920 -> "1080p"
    width >= 1280 -> "720p"
    else -> "SD"
}

val View.scope : CoroutineScope
    get() = when(val ctx = context) {
        is CoroutineScope -> ctx
        is LifecycleOwner -> ctx.lifecycleScope
        else -> AppScope
    }

fun Activity.manageHttpException(e: Exception) {
    when (e) {
        is NoConnectivityException -> {
            if (showTvUi) {
                val intent = Intent(this, ConfirmationTvActivity::class.java)
                intent.putExtra(ConfirmationTvActivity.CONFIRMATION_DIALOG_TITLE, getString(R.string.no_internet_connection))
                intent.putExtra(ConfirmationTvActivity.CONFIRMATION_DIALOG_TEXT, getString(R.string.open_network_settings))
                startActivityForResult(intent, REQUEST_CODE_NO_CONNECTION)
            } else {
                Snackbar.make(findViewById<View>(android.R.id.content), R.string.no_internet_connection, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}

fun <T> Flow<T>.launchWhenStarted(scope: LifecycleCoroutineScope): Job = scope.launchWhenStarted {
    collect() // tail-call
}
