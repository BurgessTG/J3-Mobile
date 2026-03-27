package io.j3mobile.di

import org.koin.dsl.module

val appModule = module {
    single { ConnectionManager() }
}
