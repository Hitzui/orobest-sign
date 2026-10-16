package com.jomiro.app.orobest_sign.screen.login.di

import com.jomiro.app.orobest_sign.screen.login.viewmodel.LoginViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val loginModule = module {
    viewModelOf(::LoginViewModel)
}
