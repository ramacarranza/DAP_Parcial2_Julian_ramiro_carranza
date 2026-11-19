package ar.jornadas.ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

// Pedido 44
class ControlDescargasTest {
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private suspend fun esperarFin(control: ControlDescargas) { while (control.enCurso) yield() }

    @Test fun dosToquesDuranteUnaPeticionGeneranUnSoloIntento() = runBlocking {
        val control = ControlDescargas(alcance)
        val intentos = AtomicInteger()
        val respuesta = CompletableDeferred<Unit>()
        val descarga: suspend () -> Unit = { intentos.incrementAndGet(); respuesta.await() }

        assertTrue(control.pedir(descarga))
        assertFalse(control.pedir(descarga))
        respuesta.complete(Unit)
        esperarFin(control)
        assertEquals(1, intentos.get())
    }

    @Test fun trasExitoOFallaSePuedeVolverAPedir() = runBlocking {
        val control = ControlDescargas(alcance)
        val intentos = AtomicInteger()

        control.pedir { intentos.incrementAndGet() }
        esperarFin(control)
        control.pedir {
            intentos.incrementAndGet()
            try { throw IOException("sin señal") } catch (e: IOException) { }
        }
        esperarFin(control)
        assertTrue(control.pedir { intentos.incrementAndGet() })
        esperarFin(control)
        assertEquals(3, intentos.get())
    }

    @Test fun cargaAutomaticaSeReclamaUnaSolaVez() {
        val sesion = UnaVez()
        assertTrue(sesion.reclamar())
        assertFalse(sesion.reclamar())
        assertFalse(sesion.reclamar())
    }
}
