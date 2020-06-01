/*******************************************************************************
 *  TestUtil.kt
 * ****************************************************************************
 * Copyright © 2018 VLC authors and VideoLAN
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
 ******************************************************************************/

package org.videolan.vlc.util

import android.net.Uri
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.resources.TYPE_LOCAL_FAV
import org.videolan.resources.TYPE_NETWORK_FAV
import org.videolan.vlc.api.OpenSubtitle
import org.videolan.vlc.api.QueryParameters
import org.videolan.vlc.database.models.BrowserFav
import org.videolan.vlc.database.models.CustomDirectory
import org.videolan.vlc.database.models.ExternalSub
import org.videolan.vlc.database.models.Slave
import org.videolan.vlc.gui.dialogs.State
import org.videolan.vlc.gui.dialogs.SubtitleItem

object TestUtil {
    private const val fakeUri: String = "https://www.videolan.org/fake_"
    private const val fakeSubUri: String = "/storage/emulated/0/Android/data/org.videolan.vlc.debug/files/subs/"
    private const val fakeMediaUri: String = "/storage/emulated/0/Android/data/org.videolan.vlc.debug/files/media/"

    fun createLocalFav(uri: Uri, title: String, iconUrl: String?): BrowserFav {
        return BrowserFav(uri, TYPE_LOCAL_FAV, title, iconUrl)
    }

    fun createLocalUris(count: Int): List<String> {
        return (0 until count).map {
            "${fakeMediaUri}local_$it.mp4"
        }
    }

    fun createLocalFavs(count: Int): List<BrowserFav> {
        return (0 until count).map {
            createLocalFav(Uri.parse("${fakeMediaUri}_$it.mp4"), "local$it", null)
        }
    }

    fun createNetworkFav(uri: Uri, title: String, iconUrl: String?): BrowserFav {
        return BrowserFav(uri, TYPE_NETWORK_FAV, title, iconUrl)
    }

    fun createNetworkUris(count: Int): List<String> {
        return (0 until count).map { "${fakeUri}_network$it.mp4" }
    }

    fun createNetworkFavs(count: Int): List<BrowserFav> {
        return (0 until count).map {
            createNetworkFav(
                    Uri.parse(fakeUri + "network" + it),
                    "network" + 1,
                    null)
        }
    }


    fun createExternalSub(
            idSubtitle: String,
            subtitlePath: String,
            mediaPath: String,
            subLanguageID: String,
            movieReleaseName: String): ExternalSub {
        return ExternalSub(idSubtitle, subtitlePath, mediaPath, subLanguageID, movieReleaseName)
    }

    fun createExternalSubsForMedia(mediaPath: String, mediaName: String, count: Int): List<ExternalSub> {
        return (0 until count).map {
            ExternalSub(it.toString(), "${fakeSubUri}$mediaName$it", mediaPath, "en", mediaName)
        }
    }

    fun createSubtitleSlave(mediaPath: String, uri: String): Slave {
        return Slave(mediaPath, IMedia.Slave.Type.Subtitle, 2, uri)
    }

    fun createSubtitleSlavesForMedia(mediaName: String, count: Int): List<Slave> {
        return (0 until count).map {
            createSubtitleSlave("$fakeMediaUri$mediaName", "$fakeSubUri$mediaName$it.srt")
        }
    }

    fun createCustomDirectory(path: String): CustomDirectory {
        return CustomDirectory(path)
    }

    fun createCustomDirectories(count: Int): List<CustomDirectory> {
        val directory = "/sdcard/foo"
        return (0 until count).map {
            createCustomDirectory("$directory$it")
        }
    }

    fun createDownloadingSubtitleItem(
            idSubtitle: String,
            mediaUri: Uri,
            subLanguageID: String,
            movieReleaseName: String,
            zipDownloadLink: String): SubtitleItem = SubtitleItem(idSubtitle, mediaUri, subLanguageID, movieReleaseName, State.Downloading, zipDownloadLink)

    fun createDownloadingSubtitleItem(
            idSubtitle: String,
            mediaPath: String,
            subLanguageID: String,
            movieReleaseName: String,
            zipDownloadLink: String): SubtitleItem = TestUtil.createDownloadingSubtitleItem(idSubtitle, Uri.parse(mediaPath), subLanguageID, movieReleaseName, zipDownloadLink)

    fun createOpenSubtitle(
            idSubtitle: String,
            subLanguageID: String,
            movieReleaseName: String,
            zipDownloadLink: String) = OpenSubtitle(
                idSubtitle = idSubtitle, subLanguageID = subLanguageID, movieReleaseName = movieReleaseName, zipDownloadLink = zipDownloadLink,
                idMovie = "", idMovieImdb = "", idSubMovieFile = "", idSubtitleFile = "", infoFormat = "", infoOther = "", infoReleaseGroup = "",
                userID = "", iSO639 = "", movieFPS = "", languageName = "", subActualCD = "", subSumVotes = "", subAuthorComment = "", subComments = "",
                score = 0.0, seriesEpisode = "", seriesIMDBParent = "", seriesSeason = "", subAddDate = "", subAutoTranslation = "", subBad = "", subDownloadLink = "",
                subDownloadsCnt = "", subEncoding = "", subFeatured = "", subFileName = "", subForeignPartsOnly = "", subFormat = "", subFromTrusted = "", subHash = "",
                subHD = "", subHearingImpaired = "", subLastTS = "", subRating = "", subSize = "", subSumCD = "", subtitlesLink = "", subTranslator = "", subTSGroup = "",
                subTSGroupHash = "", movieByteSize = "", movieHash = "", movieTimeMS = "", queryParameters = QueryParameters("", "", ""), queryNumber = "",
                userNickName = "", userRank = "", matchedBy = "", movieImdbRating = "", movieKind = "", movieName = "", movieNameEng = "", movieYear = "")
}
