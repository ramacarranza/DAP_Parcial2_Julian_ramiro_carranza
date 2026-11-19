package ar.jornadas.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** Permite una sola descarga a la vez: los toques extra durante una petición no generan otra. */
class ControlDescargas(private val alcance: CoroutineScope) {
    private var tarea: Job? = null

    val enCurso: Boolean get() = tarea?.isActive == true

    /** Lanza la descarga si no hay otra en curso. Devuelve si la lanzó. */
    fun pedir(descarga: suspend () -> Unit): Boolean {
        if (enCurso) return false
        tarea = alcance.launch { descarga() }
        return true
    }
}

/** Marca que se consume una única vez; vive tanto como el objeto que la contiene. */
class UnaVez {
    private val usada = AtomicBoolean(false)
    fun reclamar(): Boolean = usada.compareAndSet(false, true)
}
