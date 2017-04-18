package pl.marchuck.rxcommons.rx

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log

import java.util.concurrent.atomic.AtomicBoolean

import hugo.weaving.DebugLog
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Cancellable

/**
 * Project "RxCommons"
 *
 *
 * Created by Lukasz Marczak
 * on 18.04.2017.
 */

class RxGeolocation {

    class GeolocationObservableOnSubscribe(

            internal var locationManager: LocationManager) : ObservableOnSubscribe<Location>,
            LocationListener, Disposable, Cancellable, Runnable {
        @Volatile internal var disposed = false
        internal var subscribed = AtomicBoolean(false)
        lateinit internal var e: ObservableEmitter<Location>
        internal var handler: Handler

        init {
            this.handler = Handler(Looper.getMainLooper())
        }

        @Throws(Exception::class)
        override fun subscribe(_e: ObservableEmitter<Location>) {
            this.e = _e
            this.subscribed.set(true)
            this.disposed = false
            this.e.setDisposable(this)
            this.e.setCancellable(this)
            this.handler.post(this)
        }

        @DebugLog
        override fun onLocationChanged(location: Location?) {

            if (!e.isDisposed && location != null) {
                e.onNext(location)
            } else {
                handler.removeCallbacks(this)
                try {
                    locationManager.removeUpdates(this@GeolocationObservableOnSubscribe)
                } catch (x: SecurityException) {
                    Log.e(TAG, "dispose: error", x)
                }

            }
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {

        }

        override fun onProviderEnabled(provider: String) {

        }

        override fun onProviderDisabled(provider: String) {

        }

        @DebugLog
        override fun dispose() {
            if (isDisposed) {
                return
            }
            disposed = true
        }

        @DebugLog
        override fun isDisposed(): Boolean {
            return disposed
        }

        @DebugLog
        @Throws(Exception::class)
        override fun cancel() {
            dispose()
        }


        @DebugLog override fun run() {
            if (!this.e.isDisposed) {
                try {
                    locationManager.requestLocationUpdates("fused", 0, 0f, this@GeolocationObservableOnSubscribe)
                } catch (calledWhenPermissionsNotGranted: SecurityException) {
                    this@GeolocationObservableOnSubscribe.e.onError(calledWhenPermissionsNotGranted)
                }
            }
        }

        companion object {

            val TAG = GeolocationObservableOnSubscribe::class.java.simpleName!!
        }
    }
}
