package eu.kanade.presentation.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.domain.manga.model.Manga
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.components.ScrollbarLazyColumn
import eu.kanade.presentation.util.bottomNavPaddingValues
import eu.kanade.presentation.util.plus
import eu.kanade.presentation.util.topPaddingValues
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.source.feed.SourceFeedPresenter
import exh.savedsearches.models.FeedSavedSearch
import exh.savedsearches.models.SavedSearch

sealed class SourceFeedUI {
    abstract val id: Long

    abstract val title: String
        @Composable
        @ReadOnlyComposable
        get

    abstract val results: List<Manga>?

    abstract fun withResults(results: List<Manga>?): SourceFeedUI

    data class Latest(override val results: List<Manga>?) : SourceFeedUI() {
        override val id: Long = -1
        override val title: String
            @Composable
            @ReadOnlyComposable
            get() = stringResource(R.string.latest)

        override fun withResults(results: List<Manga>?): SourceFeedUI {
            return copy(results = results)
        }
    }
    data class Browse(override val results: List<Manga>?) : SourceFeedUI() {
        override val id: Long = -2
        override val title: String
            @Composable
            @ReadOnlyComposable
            get() = stringResource(R.string.browse)

        override fun withResults(results: List<Manga>?): SourceFeedUI {
            return copy(results = results)
        }
    }
    data class SourceSavedSearch(
        val feed: FeedSavedSearch,
        val savedSearch: SavedSearch,
        override val results: List<Manga>?,
    ) : SourceFeedUI() {
        override val id: Long
            get() = feed.id

        override val title: String
            @Composable
            @ReadOnlyComposable
            get() = savedSearch.name

        override fun withResults(results: List<Manga>?): SourceFeedUI {
            return copy(results = results)
        }
    }
}

@Composable
fun SourceFeedScreen(
    nestedScrollInterop: NestedScrollConnection,
    presenter: SourceFeedPresenter,
    onClickBrowse: () -> Unit,
    onClickLatest: () -> Unit,
    onClickSavedSearch: (SavedSearch) -> Unit,
    onClickDelete: (FeedSavedSearch) -> Unit,
    onClickManga: (Manga) -> Unit,
) {
    when {
        presenter.isLoading -> LoadingScreen()
        else -> {
            SourceFeedList(
                nestedScrollConnection = nestedScrollInterop,
                state = presenter,
                onClickBrowse = onClickBrowse,
                onClickLatest = onClickLatest,
                onClickSavedSearch = onClickSavedSearch,
                onClickDelete = onClickDelete,
                onClickManga = onClickManga,
            )
        }
    }
}

@Composable
fun SourceFeedList(
    nestedScrollConnection: NestedScrollConnection,
    state: SourceFeedState,
    onClickBrowse: () -> Unit,
    onClickLatest: () -> Unit,
    onClickSavedSearch: (SavedSearch) -> Unit,
    onClickDelete: (FeedSavedSearch) -> Unit,
    onClickManga: (Manga) -> Unit,
) {
    ScrollbarLazyColumn(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        contentPadding = bottomNavPaddingValues + WindowInsets.navigationBars.asPaddingValues() + topPaddingValues,
    ) {
        items(
            state.items.orEmpty(),
            key = { it.id },
        ) { item ->
            SourceFeedItem(
                modifier = Modifier.animateItemPlacement(),
                item = item,
                onClickTitle = when (item) {
                    is SourceFeedUI.Browse -> onClickBrowse
                    is SourceFeedUI.Latest -> onClickLatest
                    is SourceFeedUI.SourceSavedSearch -> {
                        { onClickSavedSearch(item.savedSearch) }
                    }
                },
                onClickDelete = onClickDelete,
                onClickManga = onClickManga,
            )
        }
    }
}

@Composable
fun SourceFeedItem(
    modifier: Modifier,
    item: SourceFeedUI,
    onClickTitle: () -> Unit,
    onClickDelete: (FeedSavedSearch) -> Unit,
    onClickManga: (Manga) -> Unit,
) {
    Column(
        modifier then Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .let {
                    if (item is SourceFeedUI.SourceSavedSearch) {
                        it.combinedClickable(
                            onLongClick = {
                                onClickDelete(item.feed)
                            },
                            onClick = onClickTitle,
                        )
                    } else {
                        it.clickable(onClick = onClickTitle)
                    }
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.padding(start = 16.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward_24dp),
                contentDescription = stringResource(R.string.label_more),
                modifier = Modifier.padding(16.dp),
            )
        }
        val results = item.results
        when {
            results == null -> {
                CircularProgressIndicator()
            }
            results.isEmpty() -> {
                Text(stringResource(R.string.no_results_found), modifier = Modifier.padding(bottom = 16.dp))
            }
            else -> {
                LazyRow(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    items(results) {
                        FeedCardItem(
                            manga = it,
                            onClickManga = onClickManga,
                        )
                    }
                }
            }
        }
    }
}