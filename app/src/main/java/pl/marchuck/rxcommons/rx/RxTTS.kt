package pl.marchuck.rxcommons.rx

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.annotation.RequiresApi
import android.support.v4.app.FragmentActivity

import java.util.HashMap
import java.util.Locale

import hugo.weaving.DebugLog
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Cancellable
import pl.marchuck.rxcommons.Utils

import android.speech.tts.TextToSpeech.LANG_COUNTRY_AVAILABLE

/**
 * Created by Lukasz Marczak on 14.03.17.
 */

class RxTTS(internal var context: Context) {

    fun <T : FragmentActivity> requestTTS(activity: T, requestCode: Int) {
        val checkTTSIntent = Intent()
        checkTTSIntent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
        activity.startActivityForResult(checkTTSIntent, requestCode)
    }

    fun speak(textToRead: String): Observable<Boolean> {
        return Observable.create(RxTTSObservableOnSubscribe(context, textToRead, null))
    }

    class RxTTSObservableOnSubscribe @DebugLog
    constructor(internal var context: Context, text: String, locale: Locale?)
        : UtteranceProgressListener(), ObservableOnSubscribe<Boolean>,
            Disposable, Cancellable, TextToSpeech.OnInitListener {

        @Volatile internal var disposed: Boolean = false
        lateinit internal var emitter: ObservableEmitter<Boolean>
        internal var textToSpeech: TextToSpeech? = null
        internal var text = ""
        private var selectedLocale = Locale.getDefault()
        @Volatile internal var manually: Boolean = false

        fun setManuallyDisposable(manually: Boolean): RxTTSObservableOnSubscribe {
            this.manually = manually
            return this
        }

        init {
            if (locale != null) {
                this.selectedLocale = locale
            }
            this.text = text
        }

        @DebugLog @Throws(Exception::class)
        override fun subscribe(e: ObservableEmitter<Boolean>) {
            this.emitter = e
            this.textToSpeech = TextToSpeech(context, this)
        }

        @DebugLog override fun dispose() {
            if (disposed) {
                return
            }
            disposed = true
            if (manually) {
                return
            }

            disposeImpl()
        }

        @DebugLog override fun isDisposed(): Boolean {
            return disposed
        }

        @DebugLog @Throws(Exception::class)
        override fun cancel() {
            dispose()
        }

        @DebugLog override fun onInit(status: Int) {

            val languageCode = textToSpeech!!.setLanguage(selectedLocale)

            if (languageCode == LANG_COUNTRY_AVAILABLE) {

                textToSpeech!!.setPitch(1f)
                textToSpeech!!.setSpeechRate(1f)
                textToSpeech!!.setOnUtteranceProgressListener(this)
                performSpeak()
            } else {
                emitter.onError(Throwable("language " + selectedLocale.country + " is not supported"))
            }
        }


        @DebugLog override fun onStart(utteranceId: String) {}

        @DebugLog override fun onDone(utteranceId: String) {
            this.emitter.onNext(true)
        }

        @DebugLog override fun onError(utteranceId: String) {
            this.emitter.onError(Throwable("error TTS " + utteranceId))
        }

        private fun performSpeak() {

            if (Utils.isAtLeastLollipop()) {
                speakWithNewApi()
            } else {
                speakOldApi()
            }
        }

        internal fun disposeImpl() {
            if (textToSpeech != null) {
                textToSpeech!!.setOnUtteranceProgressListener(null)
                textToSpeech!!.shutdown()
            }
        }

        @RequiresApi(api = 21)
        internal fun speakWithNewApi() {
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "")
            textToSpeech!!.speak(text, TextToSpeech.QUEUE_ADD, params, uniqueId())
        }

        internal fun speakOldApi() {
            val map = HashMap<String, String>()
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, uniqueId())
            textToSpeech!!.speak(text, TextToSpeech.QUEUE_ADD, map)
        }

        internal fun uniqueId(): String {
            return System.currentTimeMillis().toString()
        }
    }

    companion object {

        val TAG = RxTTS::class.java.simpleName
    }
}
