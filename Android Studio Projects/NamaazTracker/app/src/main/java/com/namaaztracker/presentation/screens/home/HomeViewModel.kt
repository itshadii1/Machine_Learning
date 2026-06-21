package com.namaaztracker.presentation.screens.home

import androidx.lifecycle.ViewModel
import com.namaaztracker.domain.model.NamaazType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    val namaazTypes: List<NamaazType> = NamaazType.entries
}
