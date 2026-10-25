package com.example.wim100.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migraciones {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Las evaluaciones anteriores adicionales quedan documentadas antes de retirarlas.
            db.execSQL("""
                INSERT INTO historial (candidataId, accion, detalle, usuario, fecha)
                SELECT e.candidataId, 'Evaluación anterior archivada',
                    'Evaluación #' || e.id || ' de ' || e.evaluadora ||
                    '. Fecha original: ' || e.fecha || '. Total: ' || e.puntajeTotal ||
                    '. Observación general: ' || e.observacionGeneral || '. Criterios: ' ||
                    COALESCE((SELECT GROUP_CONCAT(c.nombre || ' (' || c.peso || '%): ' ||
                        p.puntaje || '/5. ' || p.observacion, ' | ')
                        FROM puntajes_criterio p JOIN criterios c ON c.id = p.criterioId
                        WHERE p.evaluacionId = e.id), 'Sin puntajes'),
                    e.evaluadora, CAST(strftime('%s', 'now') AS INTEGER) * 1000
                FROM evaluaciones e
                WHERE e.id NOT IN (SELECT MIN(id) FROM evaluaciones GROUP BY candidataId)
            """.trimIndent())
            db.execSQL("""
                DELETE FROM puntajes_criterio WHERE evaluacionId IN (
                    SELECT id FROM evaluaciones WHERE id NOT IN (
                        SELECT MIN(id) FROM evaluaciones GROUP BY candidataId))
            """.trimIndent())
            db.execSQL("DELETE FROM evaluaciones WHERE id NOT IN (SELECT MIN(id) FROM evaluaciones GROUP BY candidataId)")
            db.execSQL("ALTER TABLE evaluaciones ADD COLUMN fechaEdicion INTEGER DEFAULT NULL")
            db.execSQL("ALTER TABLE evaluaciones ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_evaluaciones_candidataId ON evaluaciones(candidataId)")
            db.execSQL("""
                UPDATE candidatas SET estado = 'EVALUADA'
                WHERE estado IN ('POSTULADA', 'EN_EVALUACION')
                AND EXISTS (SELECT 1 FROM evaluaciones WHERE candidataId = candidatas.id)
            """.trimIndent())
        }
    }
}
