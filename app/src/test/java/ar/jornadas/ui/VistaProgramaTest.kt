package ar.jornadas.ui

import ar.jornadas.data.Charla
import org.junit.Assert.*
import org.junit.Test

class VistaProgramaTest {
    private fun charla(id: String, dia: Int, tema: String) =
        Charla(id, "Charla $id", tema, dia, "09:00", "Aula", "Ponente", "Resumen")

    private val programa = listOf(
        charla("11", 1, "Android"), charla("21", 2, "Datos"),
        charla("22", 2, "Datos"), charla("23", 2, "Calidad"),
    )

    // Pedido 41
    @Test fun fallaConEdicionPreviaConservaListaYAvisa() {
        val vista = armarVista(hayEdicion = true, charlas = programa, fallo = true)
        assertEquals(programa, vista.charlas)
        assertEquals(AVISO_DATOS_GUARDADOS, vista.aviso)
        assertFalse(vista.ofrecerReintento)
    }

    @Test fun fallaSinDescargaPreviaOfreceReintento() {
        val vista = armarVista(hayEdicion = false, charlas = emptyList(), fallo = true)
        assertTrue(vista.ofrecerReintento)
        assertEquals(AVISO_SIN_DESCARGA, vista.aviso)
    }

    @Test fun edicionVaciaExitosaMuestraSinCharlas() {
        val vista = armarVista(hayEdicion = true, charlas = emptyList(), fallo = false)
        assertTrue(vista.sinCharlas)
        assertNull(vista.aviso)
    }

    @Test fun edicionVaciaSigueValidaAlReabrirSinSenal() {
        val vista = armarVista(hayEdicion = true, charlas = emptyList(), fallo = true)
        assertTrue(vista.sinCharlas)
        assertFalse(vista.ofrecerReintento)
    }

    // Pedido 43
    @Test fun filtrarDia2YDatosDejaDosResultados() {
        assertEquals(listOf("21", "22"), filtrar(programa, dia = 2, tema = "Datos").map { it.id })
    }

    @Test fun sinFiltrosDevuelveTodo() {
        assertEquals(programa, filtrar(programa, dia = 0, tema = TODOS))
    }
}
