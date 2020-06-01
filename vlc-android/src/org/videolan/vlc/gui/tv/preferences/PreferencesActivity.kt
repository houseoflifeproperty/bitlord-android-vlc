/*
 * *************************************************************************
 *  PreferencesActivity.java
 * **************************************************************************
 *  Copyright © 2015 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *  ***************************************************************************
 */

package org.videolan.vlc.gui.tv.preferences

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.tools.RESULT_RESTART
import org.videolan.tools.RESULT_RESTART_APP
import org.videolan.tools.Settings

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class PreferencesActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.tv_preferences_activity)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (!fragmentManager.popBackStackImmediate()) finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun applyTheme() {
        val pref = Settings.getInstance(this)
        val enableBlackTheme = pref.getBoolean("enable_black_theme", false)
        if (enableBlackTheme) {
            setTheme(R.style.Theme_VLC_Black)
        }
    }

    fun restartMediaPlayer() {
        val le = PlaybackService.restartPlayer
        if (le.hasObservers()) le.value = true
    }

    fun setRestart() {
        setResult(RESULT_RESTART)
    }

    fun setRestartApp() {
        setResult(RESULT_RESTART_APP)
    }

    fun exitAndRescan() {
        setRestart()
        val intent = intent
        finish()
        startActivity(intent)
    }

    fun detectHeadset(detect: Boolean) {
        val le = PlaybackService.headSetDetection
        if (le.hasObservers()) le.value = detect
    }
}
