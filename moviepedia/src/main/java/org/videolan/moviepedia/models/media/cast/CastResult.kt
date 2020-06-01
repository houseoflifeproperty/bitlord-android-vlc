/*
 * ************************************************************************
 *  CastResult.kt
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

package org.videolan.moviepedia.models.media.cast

import com.squareup.moshi.Json

data class CastResult(
        @field:Json(name = "actor")
        val actor: List<Actor>?,
        @field:Json(name = "director")
        val director: List<Director>?,
        @field:Json(name = "musician")
        val musician: List<Musician>?,
        @field:Json(name = "producer")
        val producer: List<Producer>?,
        @field:Json(name = "writer")
        val writer: List<Writer>?
)

data class Actor(
        @field:Json(name = "characters")
        val characters: List<String>,
        @field:Json(name = "person")
        val person: Person,
        @field:Json(name = "source")
        val source: String
)

data class Director(
        @field:Json(name = "person")
        val person: Person,
        @field:Json(name = "source")
        val source: String
)

data class Images(
        @field:Json(name = "profiles")
        val profiles: List<Profile>
)

data class Musician(
        @field:Json(name = "person")
        val person: Person,
        @field:Json(name = "source")
        val source: String
)

data class Person(
        @field:Json(name = "imageEndpoint")
        val imageEndpoint: String,
        @field:Json(name = "images")
        val images: Images?,
        @field:Json(name = "name")
        val name: String,
        @field:Json(name = "personId")
        val personId: String
)

fun Person.image(): String? {
        if (images?.profiles?.isEmpty() != false) {
                return null
        }
        return "${imageEndpoint}img${images.profiles[0].path}"
}

data class Producer(
        @field:Json(name = "person")
        val person: Person,
        @field:Json(name = "source")
        val source: String
)

data class Profile(
        @field:Json(name = "language")
        val language: String,
        @field:Json(name = "path")
        val path: String,
        @field:Json(name = "ratio")
        val ratio: Double
)

data class Writer(
        @field:Json(name = "person")
        val person: Person,
        @field:Json(name = "source")
        val source: String
)