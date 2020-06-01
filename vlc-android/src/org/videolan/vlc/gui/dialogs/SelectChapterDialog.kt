/**
 * **************************************************************************
 * PickTimeFragment.java
 * ****************************************************************************
 * Copyright © 2015 VLC authors and VideoLAN
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
 * ***************************************************************************
 */
package org.videolan.vlc.gui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.videolan.medialibrary.Tools
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.databinding.ChapterListItemBinding
import java.util.*

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class SelectChapterDialog : VLCBottomSheetDialogFragment(), Observer<PlaybackService>, IOnChapterSelectedListener {

    companion object {

        fun newInstance(): SelectChapterDialog {
            return SelectChapterDialog()
        }
    }


    private lateinit var chapterList: RecyclerView

    private var service: PlaybackService? = null

    override fun initialFocusedView(): View = chapterList

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_select_chapter, container)
        chapterList = view.findViewById(R.id.chapter_list)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PlaybackService.service.observe(this, this)
    }

    private fun initChapterList() {
        val chapters = service!!.getChapters(-1)
        val chaptersCount = chapters?.size ?: 0
        if (chaptersCount <= 1) return

        val chapterData = ArrayList<Chapter>()

        for (i in 0 until chaptersCount) {
            val name: String = if (chapters!![i].name == null || chapters[i].name == "")
                resources.getString(R.string.chapter) + " " + i
            else
                chapters[i].name
            chapterData.add(Chapter(name, Tools.millisToString(chapters[i].timeOffset)))
        }


        val adapter = ChapterAdapter(chapterData, service?.chapterIdx, this)

        chapterList.layoutManager = LinearLayoutManager(requireActivity())
        chapterList.adapter = adapter


    }

    override fun onChapterSelected(position: Int) {
        service?.chapterIdx = position
        dismiss()

    }

    override fun onChanged(service: PlaybackService?) {
        if (service != null) {
            this.service = service
            initChapterList()
        } else
            this.service = null
    }

    override fun getDefaultState(): Int {
        return STATE_EXPANDED
    }

    override fun needToManageOrientation(): Boolean {
        return true
    }


    data class Chapter(val name: String, val time: String)

    inner class ChapterAdapter(private val chapters: List<Chapter>, private val selectedIndex: Int?, private val listener: IOnChapterSelectedListener) : RecyclerView.Adapter<ChapterViewHolder>() {
        private lateinit var binding: ChapterListItemBinding

        override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
            binding.chapter = chapters[position]
            binding.selected = selectedIndex == position
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
            binding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.chapter_list_item, parent, false) as ChapterListItemBinding
            return ChapterViewHolder(binding, listener)

        }

        override fun getItemCount(): Int {
            return chapters.size
        }
    }

    inner class ChapterViewHolder(var binding: ChapterListItemBinding, private val listener: IOnChapterSelectedListener) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.holder = this
        }

        fun onClick(v: View) {
            listener.onChapterSelected(layoutPosition)

        }


    }


}

interface IOnChapterSelectedListener {
    fun onChapterSelected(position: Int)
}




