/*
 * Copyright 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.tivi.home.discover

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import app.tivi.SharedElementHelper
import app.tivi.data.entities.TiviShow
import app.tivi.extensions.toFlowable
import app.tivi.home.HomeFragmentViewModel
import app.tivi.home.HomeNavigator
import app.tivi.interactors.UpdatePopularShows
import app.tivi.interactors.UpdateTrendingShows
import app.tivi.interactors.UpdateUserDetails
import app.tivi.tmdb.TmdbManager
import app.tivi.trakt.TraktManager
import app.tivi.util.AppRxSchedulers
import app.tivi.util.Logger
import app.tivi.util.NetworkDetector
import app.tivi.util.RxLoadingCounter
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.plusAssign
import javax.inject.Inject

class DiscoverViewModel @Inject constructor(
    schedulers: AppRxSchedulers,
    private val updatePopularShows: UpdatePopularShows,
    private val updateTrendingShows: UpdateTrendingShows,
    traktManager: TraktManager,
    tmdbManager: TmdbManager,
    updateUserDetails: UpdateUserDetails,
    private val networkDetector: NetworkDetector,
    logger: Logger
) : HomeFragmentViewModel(traktManager, updateUserDetails, networkDetector, logger) {
    private val _data = MutableLiveData<DiscoverViewState>()
    val data: LiveData<DiscoverViewState>
        get() = _data

    private val loadingState = RxLoadingCounter()

    init {
        disposables += Flowables.combineLatest(
                updateTrendingShows.observe(),
                updatePopularShows.observe(),
                tmdbManager.imageProvider,
                loadingState.observable.toFlowable(),
                ::DiscoverViewState)
                .observeOn(schedulers.main)
                .subscribe(_data::setValue, logger::e)

        refresh()
    }

    fun refresh() {
        disposables += networkDetector.waitForConnection()
                .subscribe({ onRefresh() }, logger::e)
    }

    private fun onRefresh() {
        loadingState.addLoader()
        launchInteractor(updatePopularShows, UpdatePopularShows.Params(UpdatePopularShows.Page.REFRESH))
                .invokeOnCompletion { loadingState.removeLoader() }

        loadingState.addLoader()
        launchInteractor(updateTrendingShows, UpdateTrendingShows.Params(UpdateTrendingShows.Page.REFRESH))
                .invokeOnCompletion { loadingState.removeLoader() }
    }

    fun onTrendingHeaderClicked(navigator: HomeNavigator, sharedElementHelper: SharedElementHelper? = null) {
        navigator.showTrending(sharedElementHelper)
    }

    fun onPopularHeaderClicked(navigator: HomeNavigator, sharedElementHelper: SharedElementHelper? = null) {
        navigator.showPopular(sharedElementHelper)
    }

    fun onItemPostedClicked(navigator: HomeNavigator, show: TiviShow, sharedElements: SharedElementHelper?) {
        navigator.showShowDetails(show, sharedElements)
    }
}
