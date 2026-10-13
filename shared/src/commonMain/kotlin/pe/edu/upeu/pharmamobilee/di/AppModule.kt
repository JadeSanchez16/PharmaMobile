package pe.edu.upeu.pharmamobilee.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pe.edu.upeu.pharmamobilee.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobilee.data.remote.AuthApi
import pe.edu.upeu.pharmamobilee.data.remote.GestorSesionHttp
import pe.edu.upeu.pharmamobilee.data.remote.ProductoApi
import pe.edu.upeu.pharmamobilee.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobilee.data.repository.ProductoRepositorioEnMemoria
import pe.edu.upeu.pharmamobilee.data.repository.ProductoRepositoryImpl
import pe.edu.upeu.pharmamobilee.data.repository.AuthRepositorioRest
import pe.edu.upeu.pharmamobilee.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobilee.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobilee.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobilee.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobilee.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobilee.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobilee.domain.usecase.CerrarSesion
import pe.edu.upeu.pharmamobilee.domain.usecase.IniciarSesion
import pe.edu.upeu.pharmamobilee.domain.usecase.RecuperarSesion
import pe.edu.upeu.pharmamobilee.presentation.sesion.SesionViewModel
import pe.edu.upeu.pharmamobilee.presentation.producto.ProductoViewModel

val dataModule = module {
    single { GestorSesionHttp(get<AlmacenSeguro>()) }
    single {
        crearHttpClient(
            engine = get(),
            urlBase = get(named("urlBase")),
            almacenSeguro = get(),
            gestorSesionHttp = get(),
            esDepuracion = get(named("esDepuracion"))
        )
    }
    single { AuthApi(get()) }
    single<AuthRepository> { AuthRepositorioRest(get(), get(), get()) }
    single { ProductoApi(get()) }
    single { ProductoRepositorioEnMemoria() }
    single<ProductoRepository> {
        ProductoRepositoryImpl(
            api = get(),
            categoriaPorDefecto = 1L
        )
    }
}

val domainModule = module {
    factory { IniciarSesion(get()) }
    factory { CerrarSesion(get()) }
    factory { RecuperarSesion(get()) }
    factory {
        RegistrarProductoUseCase(get())
    }
    factory {
        ActualizarProductoUseCase(get())
    }
    factory {
        ListarProductosUseCase(get())
    }
    factory {
        EliminarProductoUseCase(get())
    }
}

val presentationModule = module {
    viewModel {
        SesionViewModel(
            iniciarSesion = get(),
            cerrarSesion = get(),
            recuperarSesion = get(),
            gestorSesionHttp = get()
        )
    }
    viewModel {
        ProductoViewModel(
            listarProductosUseCase = get(),
            registrarProductoUseCase = get(),
            actualizarProductoUseCase = get(),
            eliminarProductoUseCase = get(),
            compartidor = get()
        )
    }
}

expect val platformModule: Module

fun initKoin(
    config: KoinApplication.() -> Unit = {}
) {
    startKoin {
        config()
        modules(
            dataModule,
            domainModule,
            presentationModule,
            platformModule
        )
    }
}
