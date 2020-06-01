/*****************************************************************************
 * HeaderMediaSwitcher.java
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

package org.videolan.vlc.gui.view

import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi

import org.videolan.vlc.R

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class HeaderMediaSwitcher(context: Context, attrs: AttributeSet) : AudioMediaSwitcher(context, attrs) {

    override fun addMediaView(inflater: LayoutInflater, title: String?, artist: String?, cover: Bitmap?) {
        val v = inflater.inflate(R.layout.audio_media_switcher_item, this, false)

        val coverView = v.findViewById<View>(R.id.cover) as ImageView
        val titleView = v.findViewById<View>(R.id.title) as TextView
        val artistView = v.findViewById<View>(R.id.artist) as TextView

        if (cover != null) {
            coverView.visibility = View.VISIBLE
            coverView.setImageBitmap(cover)
        }

        titleView.text = title
        titleView.isSelected = true
        val hasArtist = !TextUtils.isEmpty(artist)
        artistView.text = artist
        artistView.isSelected = hasArtist
        artistView.visibility = if (hasArtist) View.VISIBLE else View.GONE

        addView(v)
    }
}
