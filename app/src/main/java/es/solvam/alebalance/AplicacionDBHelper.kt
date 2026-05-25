package es.solvam.alebalance

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AplicacionDBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_CATEGORIAS)
        db.execSQL(SQL_CREATE_TRANSACCIONES)
        db.execSQL(SQL_CREATE_DIAS_TOTALES)
        db.execSQL(SQL_CREATE_PRESUPUESTOS)
        db.execSQL("PRAGMA foreign_keys = ON;")  // Activar las restricciones de claves foráneas
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_CATEGORIAS)
        db.execSQL(SQL_DELETE_TRANSACCIONES)
        db.execSQL(SQL_DELETE_DIAS_TOTALES)
        db.execSQL(SQL_DELETE_PRESUPUESTOS)
        onCreate(db)
    }

    fun insertarCategoria(categoria: Categoria) {
        val db = writableDatabase

        val values = ContentValues().apply {
            put(NOMBRE_CATEGORIA, categoria.nombre)
            put(TIPO_CATEGORIA, categoria.tipo)
        }
        db.insert(NOMBRE_TABLA_CATEGORIA, null, values)
        db.close()
    }

    fun insertarTransaccion(transaccion: Transacciones, categoria: Categoria) {
        val db = writableDatabase

        val values = ContentValues().apply {
            put(CANTIDAD_TRANSACCIONES, transaccion.cantidad)
            put(FECHA_TRANSACCIONES, transaccion.fecha)
            put(DESCRIPCION_TRANSACCIONES, transaccion.descripcion)
            put(RUTAIMAGEN_TRANSACCIONES, transaccion.imagenRuta)
            put(ID_CATEGORIA_EN_TRANSACCIONES, transaccion.idCategoriaEnTransacciones)
        }
        val transaccionId = db.insert(NOMBRE_TABLA_TRANSACCIONES, null, values)

        val fechaDia = transaccion.fecha

        val cursorTransacciones: Cursor = db.rawQuery(
            """
        SELECT t.$CANTIDAD_TRANSACCIONES, c.$TIPO_CATEGORIA
        FROM $NOMBRE_TABLA_TRANSACCIONES t
        JOIN $NOMBRE_TABLA_CATEGORIA c
        ON t.$ID_CATEGORIA_EN_TRANSACCIONES = c.$ID_CATEGORIA
        WHERE t.$FECHA_TRANSACCIONES = ?
        """,
            arrayOf(fechaDia.toString())
        )

        var ingresosDia = 0.0
        var gastosDia = 0.0

        while (cursorTransacciones.moveToNext()) {
            val cantidad = cursorTransacciones.getDouble(cursorTransacciones.getColumnIndexOrThrow(CANTIDAD_TRANSACCIONES))
            val tipoCategoria = cursorTransacciones.getString(cursorTransacciones.getColumnIndexOrThrow(TIPO_CATEGORIA))

            if (tipoCategoria == "Ingreso") {
                ingresosDia += cantidad
            } else if (tipoCategoria == "Gasto") {
                gastosDia += cantidad
            }
        }
        cursorTransacciones.close()

        val cursorDia = db.query(
            NOMBRE_TABLA_DIAS_TOTALES,
            arrayOf(FECHA_DIA),
            "$FECHA_DIA = ?",
            arrayOf(fechaDia.toString()),
            null, null, null
        )

        if (cursorDia.moveToFirst()) {
            db.delete(NOMBRE_TABLA_DIAS_TOTALES, "$FECHA_DIA = ?", arrayOf(fechaDia.toString()))
        }
        cursorDia.close()

        val valuesTotales = ContentValues().apply {
            put(FECHA_DIA, fechaDia)
            put(INGRESOS_DIA, ingresosDia)
            put(GASTOS_DIA, gastosDia)
        }

        db.insert(NOMBRE_TABLA_DIAS_TOTALES, null, valuesTotales)

        db.close()
    }

    fun editarTransaccion(transaccion: Transacciones, fechaAntigua: Long) {
        val db = writableDatabase
        val fechaOriginal = fechaAntigua
        val fechaNueva = transaccion.fecha
        val fechaCambio = (fechaOriginal != fechaNueva)

        if (fechaCambio) {
            eliminarDiaSiExiste(db, fechaOriginal)
            eliminarDiaSiExiste(db, fechaNueva)
        }

        val valuesTransaccion = ContentValues().apply {
            put(CANTIDAD_TRANSACCIONES, transaccion.cantidad)
            put(FECHA_TRANSACCIONES, fechaNueva)
            put(DESCRIPCION_TRANSACCIONES, transaccion.descripcion)
            put(RUTAIMAGEN_TRANSACCIONES, transaccion.imagenRuta)
            put(ID_CATEGORIA_EN_TRANSACCIONES, transaccion.idCategoriaEnTransacciones)
        }
        db.update(NOMBRE_TABLA_TRANSACCIONES, valuesTransaccion, "$ID_TRANSACCIONES = ?", arrayOf(transaccion.id.toString()))

        transaccion.fecha = fechaNueva
        transaccion.cantidad = transaccion.cantidad

        if (!fechaCambio) {
            eliminarDiaSiExiste(db, fechaOriginal)
            recalcularYRecrearDia(db, fechaOriginal)
        } else {
            val cursorTransacciones = db.query(
                NOMBRE_TABLA_TRANSACCIONES,
                arrayOf(ID_TRANSACCIONES),
                "$FECHA_TRANSACCIONES = ?",
                arrayOf(fechaOriginal.toString()),
                null, null, null
            )

            if (cursorTransacciones.moveToFirst()) {
                recalcularYRecrearDia(db, fechaOriginal)
            }
            cursorTransacciones.close()
            recalcularYRecrearDia(db, fechaNueva)
        }
        db.close()
    }

    fun eliminarDiaSiExiste(db: SQLiteDatabase, fechaDia: Long) {
        val cursorDia = db.query(
            NOMBRE_TABLA_DIAS_TOTALES,
            arrayOf(FECHA_DIA),
            "$FECHA_DIA = ?",
            arrayOf(fechaDia.toString()),
            null, null, null
        )

        if (cursorDia.moveToFirst()) {
            db.delete(NOMBRE_TABLA_DIAS_TOTALES, "$FECHA_DIA = ?", arrayOf(fechaDia.toString()))
        }
        cursorDia.close()
    }

    fun recalcularYRecrearDia(db: SQLiteDatabase, fechaDia: Long) {
        val cursorTransacciones: Cursor = db.rawQuery(
            """
        SELECT t.$CANTIDAD_TRANSACCIONES, c.$TIPO_CATEGORIA
        FROM $NOMBRE_TABLA_TRANSACCIONES t
        JOIN $NOMBRE_TABLA_CATEGORIA c
        ON t.$ID_CATEGORIA_EN_TRANSACCIONES = c.$ID_CATEGORIA
        WHERE t.$FECHA_TRANSACCIONES = ?
        """,
            arrayOf(fechaDia.toString())
        )

        var ingresosDia = 0.0
        var gastosDia = 0.0

        while (cursorTransacciones.moveToNext()) {
            val cantidad = cursorTransacciones.getDouble(cursorTransacciones.getColumnIndexOrThrow(CANTIDAD_TRANSACCIONES))
            val tipoCategoria = cursorTransacciones.getString(cursorTransacciones.getColumnIndexOrThrow(TIPO_CATEGORIA))

            if (tipoCategoria == "Ingreso") {
                ingresosDia += cantidad
            } else if (tipoCategoria == "Gasto") {
                gastosDia += cantidad
            }
        }
        cursorTransacciones.close()

        val valuesTotales = ContentValues().apply {
            put(FECHA_DIA, fechaDia)
            put(INGRESOS_DIA, ingresosDia)
            put(GASTOS_DIA, gastosDia)
        }

        val rowsUpdated = db.update(
            NOMBRE_TABLA_DIAS_TOTALES,
            valuesTotales,
            "$FECHA_DIA = ?",
            arrayOf(fechaDia.toString())
        )

        if (rowsUpdated == 0) {
            db.insert(NOMBRE_TABLA_DIAS_TOTALES, null, valuesTotales)
        }
    }

    fun obtenerTotalPorCategoria(db: SQLiteDatabase, fecha: Long, tipoCategoria: String): Double {
        val cursor = db.rawQuery(
            """
        SELECT SUM($CANTIDAD_TRANSACCIONES) 
        FROM $NOMBRE_TABLA_TRANSACCIONES t
        JOIN $NOMBRE_TABLA_CATEGORIA c ON t.$ID_CATEGORIA_EN_TRANSACCIONES = c.$ID_CATEGORIA
        WHERE t.$FECHA_TRANSACCIONES = ? 
        AND c.$TIPO_CATEGORIA = ?
        """,
            arrayOf(fecha.toString(), tipoCategoria)
        )

        var total = 0.0
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }
        cursor.close()

        return total
    }

    fun recalcularTotalesDelDia(db: SQLiteDatabase, fecha: Long) {
        val cursorSumaIngresos = db.rawQuery(
            """
        SELECT SUM($CANTIDAD_TRANSACCIONES) 
        FROM $NOMBRE_TABLA_TRANSACCIONES t
        JOIN $NOMBRE_TABLA_CATEGORIA c ON t.$ID_CATEGORIA_EN_TRANSACCIONES = c.$ID_CATEGORIA
        WHERE t.$FECHA_TRANSACCIONES = ? 
        AND c.$TIPO_CATEGORIA = 'Ingreso'
        """,
            arrayOf(fecha.toString())
        )

        val cursorSumaGastos = db.rawQuery(
            """
        SELECT SUM($CANTIDAD_TRANSACCIONES) 
        FROM $NOMBRE_TABLA_TRANSACCIONES t
        JOIN $NOMBRE_TABLA_CATEGORIA c ON t.$ID_CATEGORIA_EN_TRANSACCIONES = c.$ID_CATEGORIA
        WHERE t.$FECHA_TRANSACCIONES = ? 
        AND c.$TIPO_CATEGORIA = 'Gasto'
        """,
            arrayOf(fecha.toString())
        )

        var ingresosDia = 0.0
        var gastosDia = 0.0

        if (cursorSumaIngresos.moveToFirst()) {
            ingresosDia = cursorSumaIngresos.getDouble(0)
        }
        cursorSumaIngresos.close()

        if (cursorSumaGastos.moveToFirst()) {
            gastosDia = cursorSumaGastos.getDouble(0)
        }
        cursorSumaGastos.close()

        val valuesTotales = ContentValues().apply {
            put(INGRESOS_DIA, ingresosDia)
            put(GASTOS_DIA, gastosDia)
        }

        val rowsUpdated = db.update(NOMBRE_TABLA_DIAS_TOTALES, valuesTotales, "$FECHA_DIA = ?", arrayOf(fecha.toString()))

        if (rowsUpdated == 0) {
            valuesTotales.put(FECHA_DIA, fecha.toString())
            db.insert(NOMBRE_TABLA_DIAS_TOTALES, null, valuesTotales)
        }
    }

    fun eliminarDiaSiEsNecesario(db: SQLiteDatabase, fecha: Long) {
        val cursorTransacciones = db.query(
            NOMBRE_TABLA_TRANSACCIONES,
            arrayOf(ID_TRANSACCIONES),
            "$FECHA_TRANSACCIONES = ?",
            arrayOf(fecha.toString()),
            null, null, null
        )

        if (!cursorTransacciones.moveToFirst()) {
            db.delete(NOMBRE_TABLA_DIAS_TOTALES, "$FECHA_DIA = ?", arrayOf(fecha.toString()))
        }
        cursorTransacciones.close()
    }

    fun obtenerTotalesDelMes(fecha: Long): Array<Double> {
        val db = this.readableDatabase

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = fecha

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val primerDiaDelMes = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val ultimoDiaDelMes = calendar.timeInMillis

        val cursorSumaIngresos = db.rawQuery(
            """
        SELECT SUM($CANTIDAD_TRANSACCIONES) 
        FROM $NOMBRE_TABLA_TRANSACCIONES t
        JOIN $NOMBRE_TABLA_CATEGORIA c ON t.$ID_CATEGORIA_EN_TRANSACCIONES = c.$ID_CATEGORIA
        WHERE t.$FECHA_TRANSACCIONES BETWEEN ? AND ? 
        AND c.$TIPO_CATEGORIA = 'Ingreso'
        """,
            arrayOf(primerDiaDelMes.toString(), ultimoDiaDelMes.toString())
        )

        val cursorSumaGastos = db.rawQuery(
            """
        SELECT SUM($CANTIDAD_TRANSACCIONES) 
        FROM $NOMBRE_TABLA_TRANSACCIONES t
        JOIN $NOMBRE_TABLA_CATEGORIA c ON t.$ID_CATEGORIA_EN_TRANSACCIONES = c.$ID_CATEGORIA
        WHERE t.$FECHA_TRANSACCIONES BETWEEN ? AND ? 
        AND c.$TIPO_CATEGORIA = 'Gasto'
        """,
            arrayOf(primerDiaDelMes.toString(), ultimoDiaDelMes.toString())
        )

        var ingresosMes = 0.0
        var gastosMes = 0.0

        if (cursorSumaIngresos.moveToFirst()) {
            ingresosMes = cursorSumaIngresos.getDouble(0)
        } else {
            ingresosMes = 0.0
        }
        cursorSumaIngresos.close()

        if (cursorSumaGastos.moveToFirst()) {
            gastosMes = cursorSumaGastos.getDouble(0)
        } else {
            gastosMes = 0.0
        }
        cursorSumaGastos.close()

        val totalFinalMes = ingresosMes - gastosMes
        return arrayOf(ingresosMes, gastosMes, totalFinalMes)
    }

    fun obtenerTotalCuenta(): Double {
        val db = this.readableDatabase

        val cursorSumaIngresos = db.rawQuery(
            """
        SELECT SUM($CANTIDAD_TRANSACCIONES) 
        FROM $NOMBRE_TABLA_TRANSACCIONES t
        JOIN $NOMBRE_TABLA_CATEGORIA c ON t.$ID_CATEGORIA_EN_TRANSACCIONES = c.$ID_CATEGORIA
        WHERE c.$TIPO_CATEGORIA = 'Ingreso'
        """,
        null
        )

        val cursorSumaGastos = db.rawQuery(
            """
        SELECT SUM($CANTIDAD_TRANSACCIONES) 
        FROM $NOMBRE_TABLA_TRANSACCIONES t
        JOIN $NOMBRE_TABLA_CATEGORIA c ON t.$ID_CATEGORIA_EN_TRANSACCIONES = c.$ID_CATEGORIA
        WHERE c.$TIPO_CATEGORIA = 'Gasto'
        """,
           null)

        var ingresosMes = 0.0
        var gastosMes = 0.0

        if (cursorSumaIngresos.moveToFirst()) {
            ingresosMes = cursorSumaIngresos.getDouble(0)
        } else {
            ingresosMes = 0.0
        }
        cursorSumaIngresos.close()

        if (cursorSumaGastos.moveToFirst()) {
            gastosMes = cursorSumaGastos.getDouble(0)
        } else {
            gastosMes = 0.0
        }
        cursorSumaGastos.close()

        val totalFinal = ingresosMes - gastosMes

        return totalFinal
    }

    fun eliminarTransaccion(transaccion: Transacciones) {
        val db = writableDatabase
        val fechaTransaccion = transaccion.fecha

        db.delete(NOMBRE_TABLA_TRANSACCIONES, "$ID_TRANSACCIONES = ?", arrayOf(transaccion.id.toString()))

        recalcularTotalesDelDia(db, fechaTransaccion)
        eliminarDiaSiEsNecesario(db, fechaTransaccion)

        db.close()
    }


    fun getAllCategorias(): MutableList<Categoria> {
        val categorias = mutableListOf<Categoria>()
        val db = readableDatabase

        val cursor: Cursor = db.query(
            NOMBRE_TABLA_CATEGORIA,
            null,
            null,
            null,
            null,
            null,
            null
        )

        cursor.use {
            with(cursor) {
                while (moveToNext()) {
                    val id_categoria = getInt(getColumnIndexOrThrow(ID_CATEGORIA))
                    val nombre_categoria = getString(getColumnIndexOrThrow(NOMBRE_CATEGORIA))
                    val tipo_categoria = getString(getColumnIndexOrThrow(TIPO_CATEGORIA))

                    val categoria = Categoria(id_categoria, nombre_categoria, tipo_categoria)
                    categorias.add(categoria)
                }
            }
        }
        db.close()
        return categorias
    }

    fun obtenerTransaccionesDelMes(fecha: Long): MutableList<Transacciones> {
        val transacciones = mutableListOf<Transacciones>()

        val calendar = Calendar.getInstance().apply {
            timeInMillis = fecha
        }

        val mes = calendar.get(Calendar.MONTH) + 1
        val año = calendar.get(Calendar.YEAR)

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val primerDiaDelMes = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val ultimoDiaDelMes = calendar.timeInMillis
        val db = readableDatabase

        val cursor: Cursor = db.query(
            NOMBRE_TABLA_TRANSACCIONES,
            null,
            "$FECHA_TRANSACCIONES BETWEEN ? AND ?",
            arrayOf(primerDiaDelMes.toString(), ultimoDiaDelMes.toString()),
            null,
            null,
            "$FECHA_TRANSACCIONES ASC"
        )

        cursor.use {
            while (cursor.moveToNext()) {
                val id_transaccion = cursor.getInt(cursor.getColumnIndexOrThrow(ID_TRANSACCIONES))
                val cantidad = cursor.getDouble(cursor.getColumnIndexOrThrow(CANTIDAD_TRANSACCIONES))
                val fechaTransaccion = cursor.getLong(cursor.getColumnIndexOrThrow(FECHA_TRANSACCIONES))
                val descripcion = cursor.getString(cursor.getColumnIndexOrThrow(DESCRIPCION_TRANSACCIONES))
                val rutaImagen = cursor.getString(cursor.getColumnIndexOrThrow(RUTAIMAGEN_TRANSACCIONES))
                val idCategoriaEnTransacciones = cursor.getInt(cursor.getColumnIndexOrThrow(ID_CATEGORIA_EN_TRANSACCIONES))

                val transaccion = Transacciones(
                    id_transaccion,
                    cantidad,
                    fechaTransaccion,
                    descripcion,
                    rutaImagen,
                    idCategoriaEnTransacciones
                )
                transacciones.add(transaccion)
            }
        }

        db.close()
        return transacciones
    }

    fun obtenerDiasDelMes(fecha: Long): MutableList<DiasTotales> {
        val diasTotales = mutableListOf<DiasTotales>()

        val calendar = Calendar.getInstance().apply {
            timeInMillis = fecha
        }

        val mes = calendar.get(Calendar.MONTH) + 1
        val año = calendar.get(Calendar.YEAR)

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val primerDiaDelMes = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val ultimoDiaDelMes = calendar.timeInMillis

        val db = readableDatabase

        val cursor: Cursor = db.query(
            NOMBRE_TABLA_DIAS_TOTALES,
            arrayOf(FECHA_DIA, INGRESOS_DIA, GASTOS_DIA),
            "$FECHA_DIA BETWEEN ? AND ?",
            arrayOf(primerDiaDelMes.toString(), ultimoDiaDelMes.toString()),
            null,
            null,
            "$FECHA_DIA ASC"
        )

        cursor.use {
            while (cursor.moveToNext()) {
                val fechaDia = cursor.getLong(cursor.getColumnIndexOrThrow(FECHA_DIA))
                val ingresosDia = cursor.getDouble(cursor.getColumnIndexOrThrow(INGRESOS_DIA))
                val gastosDia = cursor.getDouble(cursor.getColumnIndexOrThrow(GASTOS_DIA))

                val diaTotales = DiasTotales(0, fechaDia, ingresosDia, gastosDia)
                diasTotales.add(diaTotales)
            }
        }

        db.close()
        return diasTotales
    }

    fun obtenerCategoriaPorId(idCategoria: Int): Categoria? {
        val db = this.readableDatabase

        val cursor = db.query(
            NOMBRE_TABLA_CATEGORIA,
            arrayOf(ID_CATEGORIA, NOMBRE_CATEGORIA, TIPO_CATEGORIA),
            "$ID_CATEGORIA = ?",
            arrayOf(idCategoria.toString()),
            null, null, null
        )

        var categoria: Categoria? = null
        if (cursor != null && cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(ID_CATEGORIA))
            val nombre = cursor.getString(cursor.getColumnIndexOrThrow(NOMBRE_CATEGORIA))
            val tipo = cursor.getString(cursor.getColumnIndexOrThrow(TIPO_CATEGORIA))

            categoria = Categoria(id, nombre, tipo)
        }
        cursor.close()
        db.close()

        return categoria
    }

    fun obtenerCategoriaPorNombre(nombreCategoria: String): Categoria? {
        val db = this.readableDatabase

        val cursor = db.query(
            NOMBRE_TABLA_CATEGORIA,
            arrayOf(ID_CATEGORIA, NOMBRE_CATEGORIA, TIPO_CATEGORIA),
            "$NOMBRE_CATEGORIA = ?",
            arrayOf(nombreCategoria),
            null, null, null
        )

        var categoria: Categoria? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(ID_CATEGORIA))
            val nombre = cursor.getString(cursor.getColumnIndexOrThrow(NOMBRE_CATEGORIA))
            val tipo = cursor.getString(cursor.getColumnIndexOrThrow(TIPO_CATEGORIA))

            categoria = Categoria(id, nombre, tipo)
        }
        cursor.close()
        db.close()

        return categoria
    }

    fun obtenerCategoriasPorTipo(tipoCategoria: String): MutableList<Categoria> {
        val categorias = mutableListOf<Categoria>()
        val db = readableDatabase

        val cursor: Cursor = db.query(
            NOMBRE_TABLA_CATEGORIA,
            arrayOf(ID_CATEGORIA, NOMBRE_CATEGORIA, TIPO_CATEGORIA),
            "$TIPO_CATEGORIA = ?",
            arrayOf(tipoCategoria),
            null,
            null,
            null
        )

        cursor.use {
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(ID_CATEGORIA))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(NOMBRE_CATEGORIA))
                val tipo = cursor.getString(cursor.getColumnIndexOrThrow(TIPO_CATEGORIA))

                val categoria = Categoria(id, nombre, tipo)
                categorias.add(categoria)
            }
        }
        db.close()
        return categorias
    }

    fun existeCategoriaPorNombre(nombreCategoria: String): Boolean {
        val db = readableDatabase

        val nombreCategoriaMinusculas = nombreCategoria.lowercase(Locale.getDefault())

        val cursor: Cursor = db.query(
            NOMBRE_TABLA_CATEGORIA,
            arrayOf(ID_CATEGORIA),
            "$NOMBRE_CATEGORIA = ?",
            arrayOf(nombreCategoriaMinusculas),
            null,
            null,
            null
        )
        val existe = cursor.count > 0

        cursor.close()
        db.close()

        return existe
    }

    fun existenCategoriasDeTipo(tipoCategoria: String): Boolean {
        val db = readableDatabase

        val cursor: Cursor = db.query(
            NOMBRE_TABLA_CATEGORIA,
            arrayOf(ID_CATEGORIA),
            "$TIPO_CATEGORIA = ?",
            arrayOf(tipoCategoria),
            null,
            null,
            null
        )

        val existen = cursor.count > 0

        cursor.close()
        db.close()

        return existen
    }

    fun editarCategoria(categoria: Categoria) {
        val db = writableDatabase

        val contentValues = ContentValues()
        contentValues.put(NOMBRE_CATEGORIA, categoria.nombre)
        contentValues.put(TIPO_CATEGORIA, categoria.tipo)

        db.update(NOMBRE_TABLA_CATEGORIA, contentValues, "${ID_CATEGORIA} = ?", arrayOf(categoria.id.toString()))
        db.close()
    }

    fun eliminarCategoriaYActualizarDias(nombreCategoria: String) {
        val db = writableDatabase
        db.execSQL("PRAGMA foreign_keys = ON;")

        val cursorCategoria = db.rawQuery(
            "SELECT $ID_CATEGORIA FROM $NOMBRE_TABLA_CATEGORIA WHERE $NOMBRE_CATEGORIA = ?",
            arrayOf(nombreCategoria)
        )

        var categoriaId: Int? = null

        if (cursorCategoria.moveToFirst()) {
            val idCategoriaIndex = cursorCategoria.getColumnIndex(ID_CATEGORIA)
            if (idCategoriaIndex != -1) {
                categoriaId = cursorCategoria.getInt(idCategoriaIndex)
                Log.d("EliminarCategoria", "ID de la categoría encontrada: $categoriaId")
            } else {
                Log.e("EliminarCategoria", "No se encontró la columna ID_CATEGORIA")
            }
        } else {
            Log.e("EliminarCategoria", "No se encontró la categoría con el nombre: $nombreCategoria")
        }

        cursorCategoria.close()

        if (categoriaId != null) {
            val fechasAfectadas = obtenerFechasAfectadas(db, categoriaId)

            val rowsDeletedCategoria = db.delete(
                NOMBRE_TABLA_CATEGORIA,
                "$ID_CATEGORIA = ?",
                arrayOf(categoriaId.toString())
            )

            if (rowsDeletedCategoria > 0) {
                Log.d("EliminarCategoria", "Categoría eliminada con éxito: $nombreCategoria")
            } else {
                Log.e("EliminarCategoria", "Error al eliminar la categoría: $nombreCategoria")
            }

            recalcularDiasAfectados(db, fechasAfectadas)
            val cursorTransacciones = db.rawQuery(
                "SELECT * FROM $NOMBRE_TABLA_TRANSACCIONES",
                null
            )

            if (cursorTransacciones.moveToFirst()) {
                val idTransaccionIndex = cursorTransacciones.getColumnIndex(ID_TRANSACCIONES)
                val categoriaIndex = cursorTransacciones.getColumnIndex(ID_CATEGORIA_EN_TRANSACCIONES)
                val cantidadIndex = cursorTransacciones.getColumnIndex(CANTIDAD_TRANSACCIONES)
                val fechaIndex = cursorTransacciones.getColumnIndex(FECHA_TRANSACCIONES)

                do {
                    val idTransaccion = cursorTransacciones.getInt(idTransaccionIndex)
                    val categoria = cursorTransacciones.getInt(categoriaIndex)
                    val cantidad = cursorTransacciones.getDouble(cantidadIndex)
                    val fecha = cursorTransacciones.getLong(fechaIndex)
                } while (cursorTransacciones.moveToNext())
            } else {
                Log.d("EliminarCategoria", "No hay transacciones restantes.")
            }

            cursorTransacciones.close()

        } else {
            Log.e("EliminarCategoria", "No se pudo encontrar la categoría o su ID es nulo.")
        }

        db.close()
    }

    fun obtenerFechasAfectadas(db: SQLiteDatabase, categoriaId: Int): List<Long> {
        val fechas = mutableListOf<Long>()
        val cursor = db.rawQuery(
            "SELECT DISTINCT $FECHA_TRANSACCIONES FROM $NOMBRE_TABLA_TRANSACCIONES WHERE $ID_CATEGORIA_EN_TRANSACCIONES = ?",
            arrayOf(categoriaId.toString())
        )

        val fechaColumnIndex = cursor.getColumnIndex(FECHA_TRANSACCIONES)

        if (fechaColumnIndex != -1) {
            while (cursor.moveToNext()) {
                val fecha = cursor.getLong(fechaColumnIndex)
                fechas.add(fecha)
            }
        } else {
            Log.e("ObtenerFechas", "No se encontraron fechas para la categoría ID: $categoriaId")
        }

        cursor.close()

        return fechas
    }

    fun recalcularDiasAfectados(db: SQLiteDatabase, fechasAfectadas: List<Long>) {
        for (fecha in fechasAfectadas) {
            val cursorTransacciones = db.rawQuery(
                "SELECT COUNT(*) FROM $NOMBRE_TABLA_TRANSACCIONES WHERE $FECHA_TRANSACCIONES = ?",
                arrayOf(fecha.toString())
            )

            cursorTransacciones.moveToFirst()
            val existenTransacciones = cursorTransacciones.getInt(0) > 0
            cursorTransacciones.close()

            if (!existenTransacciones) {
                val rowsDeleted = db.delete(
                    NOMBRE_TABLA_DIAS_TOTALES,
                    "$FECHA_DIA = ?",
                    arrayOf(fecha.toString())
                )
                if (rowsDeleted > 0) {
                    Log.d("RecalcularDias", "Día eliminado: $fecha")
                } else {
                    Log.d("RecalcularDias", "No se encontró el día para eliminar: $fecha")
                }
            } else {
                recalcularTotalesDelDia(db, fecha)
            }
        }
    }

    fun obtenerTransaccionesPorTipoYFecha(tipo: String, fecha: String, busquedaPor: String): MutableList<Transacciones> {
        val db = this.readableDatabase
        val transaccionesList = mutableListOf<Transacciones>()

        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaDate = formatoFecha.parse(fecha) ?: return transaccionesList // Si hay error en la fecha, devolvemos lista vacía
        val fechaLong = fechaDate.time

        val queryParams = mutableListOf<String>()
        var query = """
        SELECT t.$ID_TRANSACCIONES, t.$CANTIDAD_TRANSACCIONES, t.$FECHA_TRANSACCIONES, 
               t.$DESCRIPCION_TRANSACCIONES, t.$RUTAIMAGEN_TRANSACCIONES, c.$ID_CATEGORIA 
        FROM $NOMBRE_TABLA_TRANSACCIONES t 
        JOIN $NOMBRE_TABLA_CATEGORIA c ON t.$ID_CATEGORIA_EN_TRANSACCIONES = c.$ID_CATEGORIA 
    """.trimIndent()

        if (busquedaPor == "dia") {
            query += " WHERE t.$FECHA_TRANSACCIONES = ?"
            queryParams.add(fechaLong.toString())
        } else {
            val calendario = Calendar.getInstance().apply { time = fechaDate }
            calendario.set(Calendar.DAY_OF_MONTH, 1)
            val inicioMes = calendario.timeInMillis

            calendario.add(Calendar.MONTH, 1)
            calendario.add(Calendar.MILLISECOND, -1)
            val finMes = calendario.timeInMillis

            query += " WHERE t.$FECHA_TRANSACCIONES BETWEEN ? AND ?"
            queryParams.add(inicioMes.toString())
            queryParams.add(finMes.toString())
        }

        if (tipo != "Todo") {
            query += " AND c.$TIPO_CATEGORIA = ?"
            queryParams.add(tipo)
        }

        query += " ORDER BY t.$FECHA_TRANSACCIONES ASC"

        val cursor = db.rawQuery(query, queryParams.toTypedArray())

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(ID_TRANSACCIONES))
                val cantidad = cursor.getDouble(cursor.getColumnIndexOrThrow(CANTIDAD_TRANSACCIONES))
                val fechaTransaccion = cursor.getLong(cursor.getColumnIndexOrThrow(FECHA_TRANSACCIONES))
                val descripcion = cursor.getString(cursor.getColumnIndexOrThrow(DESCRIPCION_TRANSACCIONES))
                val imagenRuta = cursor.getString(cursor.getColumnIndexOrThrow(RUTAIMAGEN_TRANSACCIONES))
                val idCategoria = cursor.getInt(cursor.getColumnIndexOrThrow(ID_CATEGORIA))

                val transaccion = Transacciones(id, cantidad, fechaTransaccion, descripcion, imagenRuta, idCategoria)
                transaccionesList.add(transaccion)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return transaccionesList
    }

    fun insertarPresupuesto(presupuesto: Presupuesto): Long {
        val db = this.writableDatabase

        val values = ContentValues().apply {
            put(PRESUPUESTO_CATEGORIA_ID, presupuesto.presupuesto_categoria_id)
            put(MES_PRESUPUESTO, presupuesto.mes_presupuesto)
            put(ANIO_PRESUPUESTO, presupuesto.anio_presupuesto)
            put(TOTAL_PRESUPUESTO, presupuesto.total_presupuesto)
        }

        val id = db.insert(NOMBRE_TABLA_PRESUPUESTOS, null, values)

        db.close()
        return id
    }

    fun obtenerCantidadConsumida(presupuesto: Presupuesto): Double {
        val db = this.readableDatabase

        val calendario = Calendar.getInstance()
        calendario.set(Calendar.YEAR, presupuesto.anio_presupuesto)
        calendario.set(Calendar.MONTH, presupuesto.mes_presupuesto - 1)
        calendario.set(Calendar.DAY_OF_MONTH, 1)
        calendario.set(Calendar.HOUR_OF_DAY, 0)
        calendario.set(Calendar.MINUTE, 0)
        calendario.set(Calendar.SECOND, 0)
        calendario.set(Calendar.MILLISECOND, 0)
        val fechaInicio = calendario.timeInMillis

        calendario.set(Calendar.DAY_OF_MONTH, calendario.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendario.set(Calendar.HOUR_OF_DAY, 23)
        calendario.set(Calendar.MINUTE, 59)
        calendario.set(Calendar.SECOND, 59)
        calendario.set(Calendar.MILLISECOND, 999)
        val fechaFin = calendario.timeInMillis

        val query = """
        SELECT SUM($CANTIDAD_TRANSACCIONES) 
        FROM $NOMBRE_TABLA_TRANSACCIONES 
        WHERE $ID_CATEGORIA_EN_TRANSACCIONES = ? 
        AND $FECHA_TRANSACCIONES BETWEEN ? AND ?
    """

        val cursor = db.rawQuery(query, arrayOf(presupuesto.presupuesto_categoria_id.toString(), fechaInicio.toString(), fechaFin.toString()))

        var cantidadConsumida = 0.0
        if (cursor.moveToFirst() && !cursor.isNull(0)) { // Evita errores si la suma es NULL
            cantidadConsumida = cursor.getDouble(0)
        }

        cursor.close()
        db.close()

        return cantidadConsumida
    }

    fun obtenerPresupuestosDelMes(fecha: Long): MutableList<Presupuesto> {
        val presupuestos = mutableListOf<Presupuesto>()
        val db = this.readableDatabase

        val calendario = Calendar.getInstance()
        calendario.timeInMillis = fecha
        val mes = calendario.get(Calendar.MONTH) + 1
        val año = calendario.get(Calendar.YEAR)

        val cursor = db.query(
            NOMBRE_TABLA_PRESUPUESTOS,
            arrayOf(ID_PRESUPUESTO, PRESUPUESTO_CATEGORIA_ID, MES_PRESUPUESTO, ANIO_PRESUPUESTO, TOTAL_PRESUPUESTO),
            "$MES_PRESUPUESTO = ? AND $ANIO_PRESUPUESTO = ?",
            arrayOf(mes.toString(), año.toString()),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val idPresupuesto = cursor.getInt(cursor.getColumnIndexOrThrow(ID_PRESUPUESTO))
                val idCategoria = cursor.getInt(cursor.getColumnIndexOrThrow(PRESUPUESTO_CATEGORIA_ID))
                val mesPresupuesto = cursor.getInt(cursor.getColumnIndexOrThrow(MES_PRESUPUESTO))
                val añoPresupuesto = cursor.getInt(cursor.getColumnIndexOrThrow(ANIO_PRESUPUESTO))
                val total = cursor.getDouble(cursor.getColumnIndexOrThrow(TOTAL_PRESUPUESTO))

                val presupuesto = Presupuesto(idPresupuesto, idCategoria, mesPresupuesto, añoPresupuesto, total)
                presupuestos.add(presupuesto)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return presupuestos
    }

    fun obtenerPresupuestoPorCategoriaYMes(categoriaId: Int, mes: Int, anio: Int): Presupuesto? {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $NOMBRE_TABLA_PRESUPUESTOS WHERE $PRESUPUESTO_CATEGORIA_ID = ? AND $MES_PRESUPUESTO = ? AND $ANIO_PRESUPUESTO = ?",
            arrayOf(categoriaId.toString(), mes.toString(), anio.toString())
        )

        if (cursor.moveToFirst()) {
            val idColumnIndex = cursor.getColumnIndex(ID_PRESUPUESTO)
            val categoriaIdColumnIndex = cursor.getColumnIndex(PRESUPUESTO_CATEGORIA_ID)
            val mesColumnIndex = cursor.getColumnIndex(MES_PRESUPUESTO)
            val anioColumnIndex = cursor.getColumnIndex(ANIO_PRESUPUESTO)
            val cantidadColumnIndex = cursor.getColumnIndex(TOTAL_PRESUPUESTO)

            if (idColumnIndex >= 0 && categoriaIdColumnIndex >= 0 && mesColumnIndex >= 0 && anioColumnIndex >= 0 && cantidadColumnIndex >= 0) {
                val presupuesto = Presupuesto(
                    cursor.getInt(idColumnIndex),
                    cursor.getInt(categoriaIdColumnIndex),
                    cursor.getInt(mesColumnIndex),
                    cursor.getInt(anioColumnIndex),
                    cursor.getDouble(cantidadColumnIndex)
                )
                cursor.close()
                return presupuesto
            } else {
                Log.e("DBError", "Algunas columnas no se encuentran en la consulta")
            }
        } else {
            Log.d("DBInfo", "No se encontró un presupuesto para la categoría $categoriaId en $mes/$anio")
        }
        cursor.close()
        return null
    }

    fun obtenerPresupuestosPorFecha(fecha: String): MutableList<Presupuesto> {
        val listaPresupuestos = mutableListOf<Presupuesto>()
        val db = this.readableDatabase

        val formatoEntrada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formatoSalidaMes = SimpleDateFormat("M", Locale.getDefault())
        val formatoSalidaAnio = SimpleDateFormat("yyyy", Locale.getDefault())

        val date = formatoEntrada.parse(fecha)!!
        val mes = formatoSalidaMes.format(date).toInt()
        val anio = formatoSalidaAnio.format(date).toInt()

        val query = "SELECT * FROM presupuestos WHERE mes_presupuesto = ? AND anio_presupuesto = ?"
        val cursor = db.rawQuery(query, arrayOf(mes.toString(), anio.toString()))

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(ID_PRESUPUESTO))
                val total = cursor.getDouble(cursor.getColumnIndexOrThrow(TOTAL_PRESUPUESTO))
                val categoriaId = cursor.getInt(cursor.getColumnIndexOrThrow(PRESUPUESTO_CATEGORIA_ID))

                val presupuesto = Presupuesto(id, categoriaId, mes, anio, total)
                listaPresupuestos.add(presupuesto)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return listaPresupuestos
    }

    fun existenPresupuestos(): Boolean {
        val db = this.readableDatabase
        val query = "SELECT COUNT(*) FROM presupuestos"
        val cursor = db.rawQuery(query, null)

        var existen = false
        if (cursor.moveToFirst()) {
            existen = cursor.getInt(0) > 0
        }

        cursor.close()
        db.close()
        return existen
    }

    fun actualizarPresupuesto(presupuesto: Presupuesto): Boolean {
        val db = this.writableDatabase

        val values = ContentValues().apply {
            put(PRESUPUESTO_CATEGORIA_ID, presupuesto.presupuesto_categoria_id)
            put(MES_PRESUPUESTO, presupuesto.mes_presupuesto)
            put(ANIO_PRESUPUESTO, presupuesto.anio_presupuesto)
            put(TOTAL_PRESUPUESTO, presupuesto.total_presupuesto)
        }

        val result = db.update(
            NOMBRE_TABLA_PRESUPUESTOS,
            values,
            "$ID_PRESUPUESTO = ?",
            arrayOf(presupuesto.id_presupuesto.toString())
        )

        db.close()

        return result > 0
    }

    fun eliminarPresupuesto(presupuesto: Presupuesto): Boolean {
        val db = this.writableDatabase

        val result = db.delete(
            NOMBRE_TABLA_PRESUPUESTOS,
            "$ID_PRESUPUESTO = ?",
            arrayOf(presupuesto.id_presupuesto.toString())
        )
        db.close()
        return result > 0
    }

    fun obtenerTotalGastosIngresosPorCategoriaYMes(categoriaId: Int, mes: Int, anio: Int): Double {
        val db = this.readableDatabase

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, anio)
        calendar.set(Calendar.MONTH, mes - 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val fechaInicio = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val fechaFin = calendar.timeInMillis

        val query = """
        SELECT SUM(${CANTIDAD_TRANSACCIONES}) 
        FROM ${NOMBRE_TABLA_TRANSACCIONES}
        WHERE ${ID_CATEGORIA_EN_TRANSACCIONES} = ? 
        AND ${FECHA_TRANSACCIONES} BETWEEN ? AND ?
    """

        val cursor = db.rawQuery(query, arrayOf(categoriaId.toString(), fechaInicio.toString(), fechaFin.toString()))

        var total = 0.0
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            total = cursor.getDouble(0)
        }

        cursor.close()
        db.close()
        return total
    }

    companion object {
        const val DATABASE_VERSION = 44
        const val DATABASE_NAME = "aplicacion.db"

        const val NOMBRE_TABLA_CATEGORIA = "categorias"
        const val ID_CATEGORIA = "id_categoria"
        const val NOMBRE_CATEGORIA = "nombre"
        const val TIPO_CATEGORIA = "tipo"

        const val NOMBRE_TABLA_TRANSACCIONES = "transacciones"
        const val ID_TRANSACCIONES = "id_transaccion"
        const val CANTIDAD_TRANSACCIONES = "cantidad"
        const val FECHA_TRANSACCIONES = "fecha"
        const val DESCRIPCION_TRANSACCIONES = "descripcion"
        const val RUTAIMAGEN_TRANSACCIONES = "ruta"
        const val ID_CATEGORIA_EN_TRANSACCIONES = "id_categoria_en_transacciones"

        const val NOMBRE_TABLA_DIAS_TOTALES = "dias_totales"
        const val ID_DIA = "id_dias"
        const val FECHA_DIA = "fecha_dia"
        const val INGRESOS_DIA = "ingresos_dia"
        const val GASTOS_DIA = "gastos_dia"

        const val NOMBRE_TABLA_PRESUPUESTOS = "presupuestos"
        const val ID_PRESUPUESTO = "id_presupuesto"
        const val PRESUPUESTO_CATEGORIA_ID = "presupuesto_categoria_id"
        const val MES_PRESUPUESTO = "mes_presupuesto"
        const val ANIO_PRESUPUESTO = "anio_presupuesto"
        const val TOTAL_PRESUPUESTO = "total_presupuesto"

        private const val SQL_CREATE_DIAS_TOTALES = """
            CREATE TABLE $NOMBRE_TABLA_DIAS_TOTALES (
                $ID_DIA INTEGER PRIMARY KEY AUTOINCREMENT,
                $FECHA_DIA LONG UNIQUE,
                $INGRESOS_DIA REAL DEFAULT 0.0,
                $GASTOS_DIA REAL DEFAULT 0.0
            )
        """

        private const val SQL_CREATE_CATEGORIAS =
            "CREATE TABLE ${NOMBRE_TABLA_CATEGORIA} (" +
                    "${ID_CATEGORIA} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${NOMBRE_CATEGORIA} TEXT," +
                    "${TIPO_CATEGORIA} TEXT)"

        private const val SQL_CREATE_TRANSACCIONES =
            "CREATE TABLE ${NOMBRE_TABLA_TRANSACCIONES} (" +
                    "${ID_TRANSACCIONES} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${CANTIDAD_TRANSACCIONES} DOUBLE," +
                    "${FECHA_TRANSACCIONES} LONG," +
                    "${DESCRIPCION_TRANSACCIONES} TEXT," +
                    "${RUTAIMAGEN_TRANSACCIONES} TEXT," +
                    "${ID_CATEGORIA_EN_TRANSACCIONES} INTEGER," +
                    "FOREIGN KEY(${ID_CATEGORIA_EN_TRANSACCIONES}) REFERENCES ${NOMBRE_TABLA_CATEGORIA}(${ID_CATEGORIA}) ON DELETE CASCADE)"

        private const val SQL_CREATE_PRESUPUESTOS =
            "CREATE TABLE ${NOMBRE_TABLA_PRESUPUESTOS} (" +
                    "${ID_PRESUPUESTO} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${PRESUPUESTO_CATEGORIA_ID} INTEGER," +
                    "${MES_PRESUPUESTO} INTEGER," +
                    "${ANIO_PRESUPUESTO} INTEGER," +
                    "${TOTAL_PRESUPUESTO} REAL," +
                    "FOREIGN KEY (${PRESUPUESTO_CATEGORIA_ID}) REFERENCES categorias(${ID_CATEGORIA}) ON DELETE CASCADE)"

        private const val SQL_DELETE_CATEGORIAS = "DROP TABLE IF EXISTS ${NOMBRE_TABLA_CATEGORIA}"
        private const val SQL_DELETE_TRANSACCIONES = "DROP TABLE IF EXISTS ${NOMBRE_TABLA_TRANSACCIONES}"
        private const val SQL_DELETE_DIAS_TOTALES = "DROP TABLE IF EXISTS ${NOMBRE_TABLA_DIAS_TOTALES}"
        private const val SQL_DELETE_PRESUPUESTOS = "DROP TABLE IF EXISTS ${NOMBRE_TABLA_PRESUPUESTOS}"

    }
}