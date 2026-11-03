package com.spidervision.tv

import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.spidervision.tv.ui.HomeScreen
import com.spidervision.tv.ui.SplashOverlay
import com.spidervision.tv.ui.SplashState
import com.spidervision.tv.ui.SoundFx
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SoundFx.init(applicationContext)
        if (savedInstanceState == null) SplashState.decide(applicationContext)
        setContent {
            // The intro draws from the very first frame, before saved layouts finish loading.
            Box(Modifier.fillMaxSize()) {
                HomeScreen(homePresses, ::isHomeApp)
                if (SplashState.visible) SplashOverlay { SplashState.visible = false }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SoundFx.hush() // focus is restored automatically on return; don't tick for it
    }

    override fun onDestroy() {
        if (isFinishing) SoundFx.release()
        super.onDestroy()
    }

    /** The Home key while SpiderVision is already showing arrives here (singleTask). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePresses.value++
    }

    /** True while SpiderVision holds the TV's HOME role; then Back on the board must not exit. */
    private fun isHomeApp(): Boolean =
        getSystemService(RoleManager::class.java)?.let {
            it.isRoleAvailable(RoleManager.ROLE_HOME) && it.isRoleHeld(RoleManager.ROLE_HOME)
        } ?: false

    companion object {
        /** Counts Home-key presses delivered to the running instance. */
        val homePresses = MutableStateFlow(0)
    }
}
