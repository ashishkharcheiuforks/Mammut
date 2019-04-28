package io.github.koss.mammut.data.converters

import io.github.koss.mammut.data.models.Emoji

fun com.sys1yagi.mastodon4j.api.entity.Emoji.toEntity(): Emoji =
        Emoji(shortcode, staticUrl, url, visibleInPicker)

fun Emoji.toModel(): com.sys1yagi.mastodon4j.api.entity.Emoji =
        com.sys1yagi.mastodon4j.api.entity.Emoji(shortcode, staticUrl, url, visibleInPicker)