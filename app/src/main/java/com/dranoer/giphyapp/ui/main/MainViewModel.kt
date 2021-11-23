package com.dranoer.giphyapp.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dranoer.giphyapp.data.model.Giphy
import com.dranoer.giphyapp.data.remote.Resource
import com.dranoer.giphyapp.domain.GiphyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ContentState {
    object Loading : ContentState()
    object Data : ContentState()
    data class Error(val message: String) : ContentState()
}

@HiltViewModel
class MainViewModel @ExperimentalCoroutinesApi
@Inject constructor(
    var repository: GiphyRepository
) : ViewModel() {

    data class MainViewState(
        val layoutState: ContentState = ContentState.Loading,
        val pageLoading: Boolean = false,
        val searchTerms: String = "",
        val giphyList: List<Giphy> = listOf(),
        val favGiphyList: List<Giphy> = listOf(),
    )

    val viewStateLiveData: MutableLiveData<MainViewState> = MutableLiveData(MainViewState())

    init {
        getTrends()
        getFavorites()
    }

    fun getTrends() {
        viewModelScope.launch {
            viewStateLiveData.value = viewStateLiveData.value?.copy(
                layoutState = ContentState.Loading,
                giphyList = listOf()
            )
            val result = repository.getTrending()
            when (result) {
                is Resource.Success -> {
                    viewStateLiveData.value = viewStateLiveData.value?.copy(
                        layoutState = ContentState.Data,
                        giphyList = result.data
                    )
                }
                is Resource.Failure -> {
                    viewStateLiveData.value = viewStateLiveData.value?.copy(
                        layoutState = ContentState.Data,
                        giphyList = listOf()
                    )
                }
            }
        }
    }

    fun search(name: String) {
        viewModelScope.launch {
            viewStateLiveData.value = viewStateLiveData.value?.copy(
                layoutState = ContentState.Loading,
                giphyList = listOf()
            )
            val result = repository.search(name)
            when (result) {
                is Resource.Success -> {
                    viewStateLiveData.value = viewStateLiveData.value?.copy(
                        searchTerms = name,
                        layoutState = ContentState.Data,
                        giphyList = result.data
                    )
                }
                is Resource.Failure -> {
                    viewStateLiveData.value = viewStateLiveData.value?.copy(
                        layoutState = ContentState.Data,
                        giphyList = listOf()
                    )
                }
            }
        }
    }

    fun getFavorites() {
        viewModelScope.launch {
            repository.getFavorites()
                .collect { faveList ->

                    val updatedMainList = viewStateLiveData.value?.giphyList
                        ?.map {
                            val isFav =
                                faveList.find { fav -> fav.id == it.id }?.isFavorite ?: false
                            it.copy(isFavorite = isFav)
                        } ?: listOf()

                    viewStateLiveData.value = viewStateLiveData.value?.copy(
                        favGiphyList = faveList,
                        giphyList = updatedMainList
                    )
                }
        }
    }

    fun updateFavorite(giphy: Giphy) {
        viewModelScope.launch {
            repository.updateFavGiphy(giphy)
        }
    }
}