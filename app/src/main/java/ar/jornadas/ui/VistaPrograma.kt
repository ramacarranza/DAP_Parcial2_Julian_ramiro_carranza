package ar.jornadas.ui

import ar.jornadas.data.Charla

/** Lo que muestra la pestaña Programa, decidido sin depender de Android para poder testearlo. */
data class VistaPrograma(
    val charlas: List<Charla> = emptyList(),
    val aviso: String? = null,
    val ofrecerReintento: Boolean = false,
    val sinCharlas: Boolean = false,
)

const val AVISO_DATOS_GUARDADOS = "Sin conexión: se están usando datos guardados"
const val AVISO_SIN_DESCARGA = "No se pudo descargar el programa"

fun armarVista(hayEdicion: Boolean, charlas: List<Charla>, fallo: Boolean): VistaPrograma = when {
    hayEdicion -> VistaPrograma(charlas, if (fallo) AVISO_DATOS_GUARDADOS else null, sinCharlas = charlas.isEmpty())
    fallo -> VistaPrograma(aviso = AVISO_SIN_DESCARGA, ofrecerReintento = true)
    else -> VistaPrograma() // primera descarga en curso
}

const val TODOS = "Todos"
val TEMAS = listOf(TODOS, "Android", "Datos", "Calidad")

/** dia = 0 significa ambos días. */
fun filtrar(charlas: List<Charla>, dia: Int, tema: String): List<Charla> =
    charlas.filter { (dia == 0 || it.dia == dia) && (tema == TODOS || it.tema == tema) }
