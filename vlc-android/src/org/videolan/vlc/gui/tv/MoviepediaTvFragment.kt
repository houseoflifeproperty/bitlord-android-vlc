/*
 * ************************************************************************
 *  NextTvFragment.kt
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

/*****************************************************************************
 * SearchFragment.kt
 *
 * Copyright © 2014-2018 VLC authors, VideoLAN and VideoLabs
 * Author: Geoffrey Métais
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
package org.videolan.vlc.gui.tv

import android.annotation.TargetApi
import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.*
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.vlc.ExternalMonitor
import org.videolan.vlc.NetworkObserver
import org.videolan.vlc.R
import org.videolan.moviepedia.MoviepediaIndexer
import org.videolan.moviepedia.models.identify.Media
import org.videolan.moviepedia.models.identify.getAllResults
import org.videolan.vlc.util.manageHttpException
import org.videolan.moviepedia.viewmodel.MoviepediaModel

private const val TAG = "SearchFragment"
private const val REQUEST_SPEECH = 1

@ExperimentalCoroutinesApi
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class MoviepediaTvFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider, CoroutineScope by MainScope(), NetworkObserver {

    private lateinit var viewModel: MoviepediaModel
    lateinit var media: MediaWrapper

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val defaultItemClickedListener: OnItemViewClickedListener
        get() = OnItemViewClickedListener { _, item, _, row ->
            if (item is Media) {
                launch {
                    withContext(Dispatchers.IO) {
                        try {
                            MoviepediaIndexer.saveMediaMetadata(requireActivity(), media, item)
                        } catch (e: Exception) {
                            requireActivity().manageHttpException(e)
                        }
                    }
                    requireActivity().finish()
                }
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSearchResultProvider(this)
        setOnItemViewClickedListener(defaultItemClickedListener)
        val intent = requireActivity().intent
        if (Intent.ACTION_SEARCH == intent.action || "com.google.android.gms.actions.SEARCH_ACTION" == intent.action)
            onQueryTextSubmit(intent.getStringExtra(SearchManager.QUERY))

        val extras = requireActivity().intent.extras ?: savedInstanceState ?: return
        media = extras.getParcelable(MoviepediaTvActivity.MEDIA) ?: return

        viewModel = ViewModelProviders.of(this).get(media.uri.path
                ?: "", MoviepediaModel::class.java)
        val cp = CardPresenter(requireActivity(), true)
        val videoAdapter = ArrayObjectAdapter(cp)
        viewModel.apiResult.observe(this, Observer {
            val medias = it.getAllResults()
            videoAdapter.clear()
            videoAdapter.addAll(0, medias)
            rowsAdapter.add(ListRow(HeaderItem(0, resources.getString(R.string.moviepedia_result)), videoAdapter))
            updateEmptyView(medias.isEmpty())
        })
        viewModel.exceptionLiveData.observe(this, Observer { e ->
            e?.let {
                requireActivity().manageHttpException(it)
                ExternalMonitor.subscribeNetworkCb(this)
            }
        })
        setSearchQuery(media.title, false)
        viewModel.search(media.uri)
    }

    override fun onDestroy() {
        ExternalMonitor.unsubscribeNetworkCb(this)
        super.onDestroy()
    }

    override fun getResultsAdapter() = rowsAdapter

    private fun queryByWords(words: String?) {
        if (words == null || words.length < 3) return
        rowsAdapter.clear()
        viewModel.search(words)
    }

    override fun onQueryTextChange(newQuery: String) = false

    override fun onQueryTextSubmit(query: String): Boolean {
        queryByWords(query)
        return true
    }

    private fun updateEmptyView(empty: Boolean) {
        (activity as? SearchActivity)?.updateEmptyView(empty)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_SPEECH && resultCode == Activity.RESULT_OK) setSearchQuery(data, true)
    }

    fun refresh() {
        viewModel.search(media.uri)
    }

    override fun onNetworkChanged() {
        refresh()
    }
}

