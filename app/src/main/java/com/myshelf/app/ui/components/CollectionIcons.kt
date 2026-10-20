package com.myshelf.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.myshelf.app.core.model.CollectionIcon

val CollectionIcon.vector: ImageVector get() = when (this) {
    CollectionIcon.SHELF -> Icons.Outlined.CollectionsBookmark
    CollectionIcon.MOVIE -> Icons.Outlined.Movie
    CollectionIcon.TV -> Icons.Outlined.LiveTv
    CollectionIcon.BOOK -> Icons.AutoMirrored.Outlined.MenuBook
    CollectionIcon.LIBRARY -> Icons.Outlined.LocalLibrary
    CollectionIcon.STAR -> Icons.Outlined.StarBorder
    CollectionIcon.HEART -> Icons.Outlined.FavoriteBorder
    CollectionIcon.WORLD -> Icons.Outlined.Public
    CollectionIcon.COMPASS -> Icons.Outlined.Explore
    CollectionIcon.MAGIC -> Icons.Outlined.AutoAwesome
    CollectionIcon.SCIENCE -> Icons.Outlined.Science
    CollectionIcon.ROCKET -> Icons.Outlined.RocketLaunch
    CollectionIcon.CASTLE -> Icons.Outlined.Castle
    CollectionIcon.GAMES -> Icons.Outlined.SportsEsports
    CollectionIcon.MUSIC -> Icons.Outlined.MusicNote
    CollectionIcon.COMIC -> Icons.Outlined.Forum
}
