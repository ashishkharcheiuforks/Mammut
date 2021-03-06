package io.github.koss.mammut.feed.domain.paging.network

import arrow.core.Either
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.Range
import com.sys1yagi.mastodon4j.api.entity.Status
import io.github.koss.mammut.data.extensions.run
import io.github.koss.mammut.feed.domain.FeedType
import io.github.koss.paging.network.*
import kotlinx.coroutines.coroutineScope
import java.lang.Exception

class DefaultFeedNetworkSource(
        private val feedType: FeedType,
        private val client: MastodonClient
): NetworkDataSource<Status> {

    override suspend fun loadMore(config: LoadConfig<Status>): List<Status> = coroutineScope {
        val builder = feedType.getRequestBuilder(client)

        val range = when (config) {
            is Initial -> Range()
            is Before -> Range(minId = config.item.id)
            is After -> Range(maxId = config.item.id)
        }

        when (val result = builder(range).run()) {
            is Either.Left -> {
                //throw Exception("Failed to load more results with config $config. Error: ${result.a}")
                return@coroutineScope emptyList<Status>()
            }
            is Either.Right -> {
                return@coroutineScope result.b.part
            }
        }
    }
}