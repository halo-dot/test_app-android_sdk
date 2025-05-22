package za.co.synthesis.halo.multiactivitytemplate

import android.app.Activity
import android.app.Application
import za.co.synthesis.halo.sdk.HaloSDK
import za.co.synthesis.halo.sdk.model.HaloInitializationParameters

class MyApplication: Application() {

    var haloCallbacks: HaloCallbacks? = null
    var isInitialized = false

    fun initializeHaloSdk(activity: Activity) {
        HaloSDK.onCreate(this, activity)
        // step 1
        if (isInitialized) {
            return
        }

        Thread {
            val timer = Timer()
            haloCallbacks = HaloCallbacks(this, activity, timer) {}
            timer.start()
            HaloSDK.initialize(
                HaloInitializationParameters(
                    haloCallbacks,
                    60000,
                    applicationInfo.packageName,
                    BuildConfig.VERSION_NAME,
                )
            )
        }.start()
    }
}