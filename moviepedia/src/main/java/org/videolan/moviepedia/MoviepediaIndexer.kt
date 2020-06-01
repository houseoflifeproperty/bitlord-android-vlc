/*
 * ************************************************************************
 *  MoviepediaIndexer.kt
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

package org.videolan.moviepedia

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.moviepedia.database.models.*
import org.videolan.moviepedia.models.identify.*
import org.videolan.moviepedia.models.media.cast.image
import org.videolan.moviepedia.repository.MediaMetadataRepository
import org.videolan.moviepedia.repository.MediaPersonRepository
import org.videolan.moviepedia.repository.MoviepediaApiRepository
import org.videolan.moviepedia.repository.PersonRepository
import org.videolan.tools.getLocaleLanguages

object MoviepediaIndexer : CoroutineScope by MainScope() {

    fun indexMedialib(context: Context) = launch(Dispatchers.IO) {
        val medias = Medialibrary.getInstance().getPagedVideos(Medialibrary.SORT_DEFAULT, false, 1000, 0)

        val filesToIndex = HashMap<Long, Uri>()
        medias.forEach {
            if (it.getMetaLong(MediaWrapper.META_METADATA_RETRIEVED) != 1L)
                filesToIndex[it.id] = it.uri
        }
        val repo = MoviepediaApiRepository.getInstance()
        val results = try {
            repo.searchMediaBatch(filesToIndex)
        } catch (e: Exception) {
            return@launch
        }
        medias.forEach { media ->
            media?.setLongMeta(MediaWrapper.META_METADATA_RETRIEVED, 1L)
        }
        results.forEach { result ->
            result.lucky?.let {
                val media = medias.find { it.id == result.id.toLong() }
                try {
                    saveMediaMetadata(context, media, it, retrieveCast = false, removePersonOrphans = false)
                } catch (e: Exception) {
                    media?.setLongMeta(251, 0L)
                }
            }
        }
        removePersonOrphans(context)
    }

    private fun removePersonOrphans(context: Context) {
        //Remove orphans
        val allPersons = PersonRepository.getInstance(context).getAll()
        val allPersonJoins = MediaPersonRepository.getInstance(context).getAll()
        val personsToRemove = allPersons.filter { person -> allPersonJoins.any { personJoin -> person.moviepediaId == personJoin.personId }.not() }
        PersonRepository.getInstance(context).deleteAll(personsToRemove)
    }

    suspend fun saveMediaMetadata(context: Context, media: MediaWrapper?, item: Media, retrieveCast: Boolean = true, removePersonOrphans: Boolean = true) {
        val repo = MoviepediaApiRepository.getInstance()
        val type = when (item.mediaType) {
            MediaType.TV_EPISODE -> MediaMetadataType.TV_EPISODE
            MediaType.MOVIE -> MediaMetadataType.MOVIE
            else -> MediaMetadataType.TV_SHOW
        }

        val mediaMetadataRepository = MediaMetadataRepository.getInstance(context)

        val show: String? = when (item.mediaType) {
            MediaType.TV_EPISODE -> {
                //check if show already exists
                var moviepediaId = mediaMetadataRepository.getTvshow(item.showId)?.metadata?.moviepediaId
                if (moviepediaId == null) {
                    //show doesn't exist, let's retrieve it
                    val tvShowResult = repo.getMedia(item.showId)
                    saveMediaMetadata(context, null, tvShowResult, retrieveCast, removePersonOrphans)
                    moviepediaId = item.showId
                }
                moviepediaId
            }
            else -> null
        }

        val languages = context.getLocaleLanguages()
        val mediaMetadata = MediaMetadata(
                item.mediaId,
                media?.id,
                type,
                item.title,
                item.summary ?: "",
                item.genre?.joinToString { genre -> genre } ?: "",
                item.date,
                item.country?.joinToString { genre -> genre }
                        ?: "", item.season, item.episode, item.getImageUri(languages).toString(), item.getBackdropUri(languages).toString(), show, false)

        val oldMediaMetadata = if (media != null) mediaMetadataRepository.getMetadata(media.id) else null
        val oldImages = oldMediaMetadata?.images

        mediaMetadataRepository.addMetadataImmediate(mediaMetadata)

        val images = ArrayList<MediaImage>()
        item.getBackdrops(languages)?.forEach {
            images.add(MediaImage(item.getImageUriFromPath(it.path), mediaMetadata.moviepediaId, MediaImageType.BACKDROP, it.language))
        }
        item.getPosters(languages)?.forEach {
            images.add(MediaImage(item.getImageUriFromPath(it.path), mediaMetadata.moviepediaId, MediaImageType.POSTER, it.language))
        }
        //delete old images
        oldImages?.let {
            mediaMetadataRepository.deleteImages(it.filter { images.any { newImage -> it.url == newImage.url }.not() })
        }
        mediaMetadataRepository.addImagesImmediate(images)

        if (retrieveCast) {
            retrieveCasting(context, mediaMetadata)
        }

        if (removePersonOrphans) removePersonOrphans(context)
    }

    suspend fun retrieveCasting(context: Context, mediaMetadata: MediaMetadata) {
        val personRepo = PersonRepository.getInstance(context)
        val repo = MoviepediaApiRepository.getInstance()
        val personsToAdd = ArrayList<MediaPersonJoin>()

        val castResult = repo.getMediaCast(mediaMetadata.moviepediaId)
        castResult.actor?.forEach { actor ->
            val actorEntity = Person(actor.person.personId, actor.person.name, actor.person.image())
            personRepo.addPersonImmediate(actorEntity)
            personsToAdd.add(MediaPersonJoin(mediaMetadata.moviepediaId, actorEntity.moviepediaId, PersonType.ACTOR))

        }
        castResult.director?.forEach { actor ->
            val actorEntity = Person(actor.person.personId, actor.person.name, actor.person.image())
            personRepo.addPersonImmediate(actorEntity)
            personsToAdd.add(MediaPersonJoin(mediaMetadata.moviepediaId, actorEntity.moviepediaId, PersonType.DIRECTOR))

        }
        castResult.writer?.forEach { actor ->
            val actorEntity = Person(actor.person.personId, actor.person.name, actor.person.image())
            personRepo.addPersonImmediate(actorEntity)
            personsToAdd.add(MediaPersonJoin(mediaMetadata.moviepediaId, actorEntity.moviepediaId, PersonType.WRITER))
        }
        castResult.musician?.forEach { actor ->
            val actorEntity = Person(actor.person.personId, actor.person.name, actor.person.image())
            personRepo.addPersonImmediate(actorEntity)
            personsToAdd.add(MediaPersonJoin(mediaMetadata.moviepediaId, actorEntity.moviepediaId, PersonType.MUSICIAN))

        }
        castResult.producer?.forEach { actor ->
            val actorEntity = Person(actor.person.personId, actor.person.name, actor.person.image())
            personRepo.addPersonImmediate(actorEntity)
            personsToAdd.add(MediaPersonJoin(mediaMetadata.moviepediaId, actorEntity.moviepediaId, PersonType.PRODUCER))

        }
        MediaPersonRepository.getInstance(context).removeAllFor(mediaMetadata.moviepediaId)
        MediaPersonRepository.getInstance(context).addPersons(personsToAdd)
        mediaMetadata.hasCast = true
        MediaMetadataRepository.getInstance(context).addMetadataImmediate(mediaMetadata)
    }
}
