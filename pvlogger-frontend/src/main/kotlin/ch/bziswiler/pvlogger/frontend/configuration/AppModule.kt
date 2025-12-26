package ch.bziswiler.pvlogger.frontend.configuration

import org.koin.dsl.module

val appModule =
    module {
        single { AppConfig() }
    }
