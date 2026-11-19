package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.*
import ar.jornadas.Servicios
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

class ProgramaModel(app: Application, private val estado: SavedStateHandle) : AndroidViewModel(app) {
    private val servicios = Servicios(app)
    val mesa = servicios.mesa
    private val repo = servicios.agenda
    val charlas = repo.programa.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    // Los filtros viven en el estado guardado: sobreviven a rotación, a abrir una ficha y a la
    // muerte del proceso; una tarea nueva no tiene estado guardado y arranca sin filtros.
    val dia = estado.getStateFlow("dia", 0)
    val tema = estado.getStateFlow("tema", TODOS)
    fun elegirDia(indice: Int) { estado["dia"] = indice }
    fun elegirTema(nombre: String) { estado["tema"] = nombre }
    private val fallo = MutableStateFlow(false)
    val vista = combine(repo.hayEdicion, charlas, fallo, ::armarVista)
        .stateIn(viewModelScope, SharingStarted.Eagerly, VistaPrograma())
    val cargando = MutableStateFlow(false)

    private val descargas = ControlDescargas(viewModelScope)

    /** Carga automática: una sola por proceso, sin importar rotaciones ni pestañas. */
    fun cargaInicial() {
        if (cargaDeSesion.reclamar()) recargar()
    }

    /** Actualización explícita: si ya hay una petición en curso, el toque se ignora. */
    fun recargar() {
        descargas.pedir {
            cargando.value = true
            try {
                repo.sincronizar()
                fallo.value = false
            } catch (e: IOException) {
                fallo.value = true
            } finally {
                cargando.value = false
            }
        }
    }

    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }

    companion object {
        // El companion vive lo mismo que el proceso: al matarlo, vuelve a haber una carga automática.
        private val cargaDeSesion = UnaVez()
    }
}
