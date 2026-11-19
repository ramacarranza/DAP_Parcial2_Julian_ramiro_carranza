package ar.jornadas.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import retrofit2.http.GET
import kotlinx.coroutines.flow.Flow

data class FichaRemota(val id: String, val title: String, val track: String, val day: Int,
    val time: String, val room: String, val speaker: String, val summary: String)

interface ProgramaApi {
    @GET("programa") suspend fun consultar(): List<FichaRemota>
}

@Entity(tableName = "charlas")
data class Charla(@PrimaryKey val id: String, val titulo: String, val tema: String, val dia: Int,
    val hora: String, val sala: String, val ponente: String, val resumen: String,
    val favorita: Boolean = false)

/** Una sola fila: existe si alguna vez se descargó una edición completa (aunque tenga cero charlas). */
@Entity(tableName = "edicion")
data class Edicion(@PrimaryKey val id: Int = 0, val descargada: Long)

@Dao
abstract class CarteleraDao {
    @Query("SELECT * FROM charlas ORDER BY dia, hora, sala")
    abstract fun observar(): Flow<List<Charla>>

    @Query("SELECT EXISTS(SELECT 1 FROM edicion)")
    abstract fun hayEdicion(): Flow<Boolean>

    /** Solo agrega charlas nuevas (sin marcar); las existentes no se pisan. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertarNuevas(fichas: List<Charla>)

    /** Actualiza el contenido por id estable sin tocar la columna favorita. */
    @Query("UPDATE charlas SET titulo = :titulo, tema = :tema, dia = :dia, hora = :hora, " +
        "sala = :sala, ponente = :ponente, resumen = :resumen WHERE id = :id")
    abstract suspend fun actualizarContenido(id: String, titulo: String, tema: String, dia: Int,
        hora: String, sala: String, ponente: String, resumen: String)

    @Query("DELETE FROM charlas WHERE id NOT IN (:vigentes)")
    abstract suspend fun retirarAusentes(vigentes: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun registrar(edicion: Edicion)

    /** La organización publica instantáneas completas: lo que no viene, ya no está en el programa. */
    @Transaction
    open suspend fun reemplazarEdicion(fichas: List<Charla>, momento: Long) {
        // Se retiran las ausentes: si una charla vuelve más adelante, vuelve sin marcar.
        retirarAusentes(fichas.map { it.id })
        // La favorita se lee de la fila en el momento de escribir, así que una marca hecha
        // durante la petición se respeta.
        fichas.forEach { actualizarContenido(it.id, it.titulo, it.tema, it.dia, it.hora, it.sala, it.ponente, it.resumen) }
        insertarNuevas(fichas)
        registrar(Edicion(descargada = momento))
    }

    @Query("UPDATE charlas SET favorita = NOT favorita WHERE id = :id")
    abstract suspend fun alternar(id: String)
}

@Database(entities = [Charla::class, Edicion::class], version = 2, exportSchema = false)
abstract class Archivo : RoomDatabase() {
    abstract fun cartelera(): CarteleraDao
}

/** v1 → v2: agrega la tabla edicion sin tocar las charlas ni las favoritas ya guardadas. */
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `edicion` (`id` INTEGER NOT NULL, `descargada` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        // Quien ya tenía charlas descargadas tiene una edición válida.
        db.execSQL("INSERT INTO edicion (id, descargada) SELECT 0, 0 WHERE EXISTS (SELECT 1 FROM charlas)")
    }
}
