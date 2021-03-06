package io.github.koss.mammut.feed.dagger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.dagger.viewmodel.ViewModelKey
import io.github.koss.mammut.feed.presentation.FeedViewModel
import io.github.koss.mammut.feed.ui.status.StatusViewModel

@Module
abstract class FeedViewModelModule {

    @Binds
    @IntoMap
    @FeedScope
    @ViewModelKey(FeedViewModel::class)
    abstract fun bindFeedViewModel(viewModel: FeedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(StatusViewModel::class)
    abstract fun bindStatusViewModel(viewModel: StatusViewModel): ViewModel

    @Binds
    @FeedScope
    abstract fun bindViewModelFactory(factory: MammutViewModelFactory): ViewModelProvider.Factory

}