package com.oscar.sincarnet

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Aplicación principal de SinCarnet.
 *
 * Inicializa el grafo de dependencias de Koin y expone el [MainViewModel]
 * para ser consumido desde [MainActivity].
 */
class SinCarnetApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SinCarnetApplication)
            modules(appModule)
        }
    }
}

/**
 * Módulo raíz de Koin.
 *
 * Registra el ViewModel principal. En fases posteriores se añadirán aquí los
 * repositorios, use cases y plataform-specific helpers con `expect/actual`.
 */
val appModule = module {
    viewModel { MainViewModel(get()) }
}
