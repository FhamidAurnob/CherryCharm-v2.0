package com.example.cherrycharm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cherrycharm.data.local.PreferencesRepository
import com.example.cherrycharm.data.local.ThemeMode
import com.example.cherrycharm.data.repository.AuthRepository
import com.example.cherrycharm.data.repository.CartRepository
import com.example.cherrycharm.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    prefs: PreferencesRepository,
    auth: AuthRepository,
    cart: CartRepository,
    user: UserRepository,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> =
        prefs.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
    val isLoggedIn: StateFlow<Boolean> = auth.isLoggedIn
    val cartCount: StateFlow<Int> = cart.count
    val wishlistCount: StateFlow<Int> = user.wishlistIds
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
