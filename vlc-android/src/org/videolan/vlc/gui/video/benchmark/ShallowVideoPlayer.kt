/*****************************************************************************
 * ShallowVideoPlayer.java
 *
 * Copyright © 2011-2014 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 */

package org.videolan.vlc.gui.video.benchmark

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi

import org.videolan.vlc.gui.video.VideoPlayerActivity


/**
 * Class to store the overriden methods in BenchActivity
 * for code readability
 */
@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
open class ShallowVideoPlayer : VideoPlayerActivity() {
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        return true
    }

    override fun onTrackballEvent(event: MotionEvent): Boolean {
        return true
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return true
    }


    override fun onAudioSubClick(anchor: View?) {}

    override fun onClick(v: View) {}
}
