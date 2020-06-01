/*
 * ************************************************************************
 *  MoviepediaBrowserTvFragment.kt
 * *************************************************************************
 * Copyright © 2019 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
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
 * **************************************************************************
 *
 *
 */

package org.videolan.vlc.gui.tv.browser

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.song_browser.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.vlc.R
import org.videolan.moviepedia.database.models.MediaMetadataType
import org.videolan.moviepedia.database.models.MediaMetadataWithImages
import org.videolan.resources.CATEGORY
import org.videolan.resources.CATEGORY_VIDEOS
import org.videolan.resources.HEADER_MOVIES
import org.videolan.resources.HEADER_TV_SHOW
import org.videolan.vlc.gui.tv.*
import org.videolan.vlc.gui.view.EmptyLoadingState
import org.videolan.vlc.interfaces.IEventsHandler
import org.videolan.vlc.providers.MoviepediaProvider
import org.videolan.vlc.util.*
import org.videolan.vlc.viewmodels.tv.MoviepediaBrowserViewModel
import org.videolan.vlc.viewmodels.tv.getMoviepediaBrowserModel
import java.util.*

@UseExperimental(ObsoleteCoroutinesApi::class)
@ExperimentalCoroutinesApi
class MoviepediaBrowserTvFragment : BaseBrowserTvFragment<MediaMetadataWithImages>() {
    override fun provideAdapter(eventsHandler: IEventsHandler<MediaMetadataWithImages>, itemSize: Int): TvItemAdapter {
        return MoviepediaTvItemAdapter((viewModel as MoviepediaBrowserViewModel).category, this, itemSize)
    }

    override lateinit var adapter: TvItemAdapter

    override fun getTitle() = when ((viewModel as MoviepediaBrowserViewModel).category) {
        HEADER_TV_SHOW -> getString(R.string.header_tvshows)
        HEADER_MOVIES -> getString(R.string.header_movies)
        else -> getString(R.string.video)
    }

    override fun getCategory(): Long = (viewModel as MoviepediaBrowserViewModel).category

    override fun getColumnNumber() = when ((viewModel as MoviepediaBrowserViewModel).category) {
        CATEGORY_VIDEOS -> resources.getInteger(R.integer.tv_videos_col_count)
        else -> resources.getInteger(R.integer.tv_songs_col_count)
    }

    companion object {
        fun newInstance(type: Long) =
                MoviepediaBrowserTvFragment().apply {
                    arguments = Bundle().apply {
                        this.putLong(CATEGORY, type)
                    }
                }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = getMoviepediaBrowserModel(arguments?.getLong(CATEGORY, HEADER_MOVIES)
                ?: HEADER_MOVIES)

        (viewModel.provider as MoviepediaProvider).pagedList.observe(this, Observer { items ->
            binding.emptyLoading.post {
                submitList(items)

                binding.emptyLoading.state = if (items.isEmpty()) EmptyLoadingState.EMPTY else EmptyLoadingState.NONE

                //headers
                val nbColumns = if ((viewModel as MoviepediaBrowserViewModel).sort == Medialibrary.SORT_ALPHA || (viewModel as MoviepediaBrowserViewModel).sort == Medialibrary.SORT_DEFAULT) 9 else 1

                headerList.layoutManager = GridLayoutManager(requireActivity(), nbColumns)
                headerAdapter.sortType = (viewModel as MoviepediaBrowserViewModel).sort
                val headerItems = ArrayList<String>()
                viewModel.provider.headers.run {
                    for (i in 0 until size()) {
                        headerItems.add(valueAt(i))
                    }
                }
                headerAdapter.items = headerItems
                headerAdapter.notifyDataSetChanged()
            }
        })
        (viewModel.provider as MoviepediaProvider).loading.observe(this, Observer {
            if (it) binding.emptyLoading.state = EmptyLoadingState.LOADING
        })
        (viewModel.provider as MoviepediaProvider).liveHeaders.observe(this, Observer {
            headerAdapter.notifyDataSetChanged()
        })
    }

    override fun onClick(v: View, position: Int, item: MediaMetadataWithImages) {
        when (item.metadata.type) {
            MediaMetadataType.TV_SHOW -> {
                val intent = Intent(activity, MoviepediaTvshowDetailsActivity::class.java)
                intent.putExtra(TV_SHOW_ID, item.metadata.moviepediaId)
                requireActivity().startActivity(intent)
            }
            else -> {
                item.metadata.mlId?.let {
                    lifecycleScope.launchWhenStarted {
                        val media = requireActivity().getFromMl { getMedia(it) }
                        TvUtil.showMediaDetail(requireActivity(), media)
                    }
                }
            }
        }
    }

    override fun onLongClick(v: View, position: Int, item: MediaMetadataWithImages): Boolean {
        when (item.metadata.type) {
            MediaMetadataType.TV_SHOW -> {
                val intent = Intent(activity, MoviepediaTvshowDetailsActivity::class.java)
                intent.putExtra(TV_SHOW_ID, item.metadata.moviepediaId)
                requireActivity().startActivity(intent)
            }
            else -> {
                item.metadata.mlId?.let {
                    lifecycleScope.launchWhenStarted {
                        val media = requireActivity().getFromMl { getMedia(it) }
                        TvUtil.showMediaDetail(requireActivity(), media)
                    }
                }
            }
        }

        return true
    }
}
