package pl.marchuck.rxcommons.rx

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log

import hugo.weaving.DebugLog
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Cancellable

/**
 * Project "Evalu-Android"
 *
 *
 * Created by Lukasz Marczak
 * on 12.04.2017.
 */

class RxPlayer {


    @DebugLog fun play(context: Context, path: String): Observable<Boolean> {
        return Observable.create(MediaPlayerObservableOnSubscribe(context, path))
    }

    class MediaPlayerObservableOnSubscribe @DebugLog
    constructor(internal val context: Context, internal val fileName: String) : ObservableOnSubscribe<Boolean>, Disposable, Cancellable, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
        internal lateinit var emitter: ObservableEmitter<Boolean>
        internal var mediaPlayer: MediaPlayer? = null
        @Volatile internal var isDisposed: Boolean = false

        @DebugLog @Throws(Exception::class)
        internal fun preparePlayer(context: Context, fileNameWithPath: String) {

        }

        @Throws(Exception::class)
        override fun subscribe(emitter: ObservableEmitter<Boolean>) {
            this.emitter = emitter
            subscribeImpl(this.emitter)
        }

        @DebugLog @Throws(Exception::class)
        internal fun subscribeImpl(emitter: ObservableEmitter<Boolean>) {
            if (emitter.isDisposed) {
                return
            }

            emitter.setDisposable(this)
            emitter.setCancellable(this)

            mediaPlayer = MediaPlayer()
            val afd = context.assets.openFd(fileName)
            mediaPlayer!!.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()

            if (mediaPlayer == null) {
                emitter.onError(IllegalStateException("Media Player not exists!"))
                return
            }

            mediaPlayer!!.prepare()
            mediaPlayer!!.setOnPreparedListener(this)
            mediaPlayer!!.setOnCompletionListener(this)
            mediaPlayer!!.setOnErrorListener(this)
            mediaPlayer!!.start()

        }

        override fun dispose() {
            if (isDisposed) return
            isDisposed = true
            if (mediaPlayer != null) {
                stopPlay()
            }
        }

        override fun isDisposed(): Boolean {
            return isDisposed
        }

        @DebugLog @Throws(Exception::class)
        override fun cancel() {
            dispose()
        }

        @DebugLog override fun onPrepared(mp: MediaPlayer) {

        }

        @DebugLog override fun onCompletion(mp: MediaPlayer) {
            stopPlay()
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                emitter.onNext(true)
                emitter.onComplete()
            }, 50)
        }

        @DebugLog override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            this.emitter.onError(IllegalStateException("Media player error $what, $extra"))
            return true
        }

        @DebugLog
        @Synchronized fun stopPlay(): Boolean {
            if (mediaPlayer == null) {
                return false
            }

            mediaPlayer!!.setOnCompletionListener(null)
            mediaPlayer!!.setOnErrorListener(null)
            try {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.stop()
                }
                mediaPlayer!!.reset()
                mediaPlayer!!.release()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "stopPlay fail, IllegalStateException: " + e.message)
            }

            mediaPlayer = null
            return true
        }

        companion object {

            val TAG = MediaPlayerObservableOnSubscribe::class.java.simpleName
        }
    }
}
