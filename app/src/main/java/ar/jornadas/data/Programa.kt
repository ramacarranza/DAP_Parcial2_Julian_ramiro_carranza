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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun guardar(fichas: List<Charla>)

    @Query("DELETE FROM charlas WHERE id NOT IN (:vigentes)")
    abstract suspend fun retirarAusentes(vigentes: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun registrar(edicion: Edicion)

    /** La organización publica instantáneas completas: lo que no viene, ya no está en el programa. */
    @Transaction
    open suspend fun reemplazarEdicion(fichas: List<Charla>, momento: Long) {
        retirarAusentes(fichas.map { it.id })
        guardar(fichas)
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
