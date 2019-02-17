package io.github.koss.mammut.toot.repo

import arrow.core.Either
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.entity.Emoji
import com.sys1yagi.mastodon4j.api.method.Public
import com.sys1yagi.mastodon4j.api.method.Statuses
import io.github.koss.mammut.base.extensions.run
import io.github.koss.mammut.toot.model.SubmissionState
import io.github.koss.mammut.toot.model.TootModel

/**
 * Class for handling status related network interactions
 */
class StatusRepository(private val client: MastodonClient) {

    /**
     * Function for posting a toot based off a given model. Returns
     * a [SubmissionState] used for keeping track of loading, errors etc.
     *
     * @param model The model to post
     * @return The state of the submission
     */
    suspend fun post(model: TootModel): SubmissionState {
        Statuses(client).postStatus(
                status = model.status,
                inReplyToId = model.inReplyToId,
                mediaIds = model.mediaIds,
                sensitive = model.sensitive,
                spoilerText = model.spoilerText,
                visibility = model.visibility
        ).run().let { result ->
            return when (result) {
                is Either.Left -> {
                    SubmissionState(
                            isSubmitting = false,
                            error = result.a.error
                    )
                }
                is Either.Right -> {
                    SubmissionState(
                            isSubmitting = false,
                            hasSubmitted = true
                    )
                }
            }
        }
    }

    /**
     * Function for loading emojis for the current instance and filtering
     * out those which aren't supposed to be visible.
     *
     * @return List of visible emojis
     */
    suspend fun loadVisibleEmojis(): List<Emoji> {
        Public(client).getEmojis().run(retryCount = 2).let { result ->
            return when (result) {
                is Either.Left -> {
                    emptyList()
                }
                is Either.Right -> {
                    result.b.filter {
                        it.visibleInPicker
                    }
                }
            }
        }
    }
}