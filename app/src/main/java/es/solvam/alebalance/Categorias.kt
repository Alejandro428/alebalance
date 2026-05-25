package es.solvam.alebalance

import ImagePickerManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class Categorias : AppCompatActivity() {

    private lateinit var dbHelper: AplicacionDBHelper
    private lateinit var imagePickerManager: ImagePickerManager
    private lateinit var imagenGastoIngreso: ImageView
    private lateinit var fechaActual: String
    private lateinit var adaptadorTransacciones: AdaptadorTransacciones
    private lateinit var recyclerViewTransacciones: RecyclerView
    private lateinit var transacciones: MutableList<Transacciones>
    private var fechaSeleccionada: Long? = null
    private var evitarRecargaEnOnResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.categorias)

        imagePickerManager = ImagePickerManager(this)
        dbHelper = AplicacionDBHelper(this)
        recyclerViewTransacciones = findViewById(R.id.recyclerCategoriasGastosYIngresos)
        recyclerViewTransacciones.layoutManager = LinearLayoutManager(this)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation_categoria)
        bottomNavigationView.setSelectedItemId(R.id.categorias)

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.inicio -> {
                    val mediaPlayer = MediaPlayer.create(this, R.raw.menuabajo)
                    mediaPlayer.start()

                    mediaPlayer.setOnCompletionListener {
                        it.release()
                    }
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.categorias -> {
                    val mediaPlayer = MediaPlayer.create(this, R.raw.menuabajo)
                    mediaPlayer.start()

                    mediaPlayer.setOnCompletionListener {
                        it.release()
                    }
                    startActivity(Intent(this, Categorias::class.java))
                    true
                }
                R.id.presupuestos -> {
                    val mediaPlayer = MediaPlayer.create(this, R.raw.menuabajo)
                    mediaPlayer.start()

                    mediaPlayer.setOnCompletionListener {
                        it.release()
                    }
                    startActivity(Intent(this, Presupuestos::class.java))
                    true
                }
                else -> false
            }
        }

        val botonCategoria: FloatingActionButton = findViewById(R.id.accionCategoria)
        botonCategoria.setOnClickListener { view ->
            val mediaPlayer = MediaPlayer.create(this, R.raw.botontransaccionprincipal)
            mediaPlayer.start()

            mediaPlayer.setOnCompletionListener {
                it.release()
            }
            mostrarPopupMenuCategoria(view, this)
        }
    }

    fun mostrarPopupMenuCategoria(view: View, contexto: Context) {
        val popupMenu = PopupMenu(contexto, view)
        popupMenu.menuInflater.inflate(R.menu.menucontextual, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.crear_nueva_categoria -> {
                    crearCategoria()
                    true
                }
                R.id.crear_ingreso -> {
                    val dbHelper = AplicacionDBHelper(this)
                    if (dbHelper.existenCategoriasDeTipo("Ingreso")) {
                        crearIngresoGasto("Ingreso")
                    } else {
                        Toast.makeText(this, "Primero debes crear al menos una categoría de Ingreso", Toast.LENGTH_SHORT).show()
                    }
                    dbHelper.close()
                    true
                }
                R.id.crear_gasto -> {
                    val dbHelper = AplicacionDBHelper(this)
                    if (dbHelper.existenCategoriasDeTipo("Gasto")) {
                        crearIngresoGasto("Gasto")
                    } else {
                        Toast.makeText(this, "Primero debes crear al menos una categoría de Gasto", Toast.LENGTH_SHORT).show()
                    }
                    dbHelper.close()
                    true
                }
                R.id.filtrar_ingreso_gasto -> {
                    mostrarSegunIndicado()
                    true
                }
                R.id.eliminar_categoria -> {
                    eliminarCategoria()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    fun crearCategoria() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.crearcategoria, null)

        val nombreNuevaCategoria: EditText = dialogLayout.findViewById(R.id.crearCategoriaNombre)
        val radioTipoIngreso: RadioButton = dialogLayout.findViewById(R.id.crearRadioCategoriaIngreso)
        val radioTipoGasto: RadioButton = dialogLayout.findViewById(R.id.crearRadioCategoriaGasto)

        builder.setTitle("Crear Categoría")
        builder.setView(dialogLayout)

        builder.setPositiveButton("Crear nueva Categoría") { dialog, _ ->
            val nombre = nombreNuevaCategoria.text.toString().trim()

            if (nombre.isEmpty()) {
                Toast.makeText(this, "Por favor ingrese un nombre para la categoría", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val dbHelper = AplicacionDBHelper(this)
            if (dbHelper.existeCategoriaPorNombre(nombre)) {
                Toast.makeText(this, "Ya existe una categoría con ese nombre", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val tipo = when {
                radioTipoIngreso.isChecked -> "Ingreso"
                radioTipoGasto.isChecked -> "Gasto"
                else -> null
            }

            if (tipo == null) {
                Toast.makeText(this, "Por favor seleccione un tipo de categoría", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val categoria = Categoria(0, nombre, tipo)
            dbHelper.insertarCategoria(categoria)

            Toast.makeText(this, "Categoría '$nombre' creada", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    fun crearIngresoGasto(tipoCategoria: String) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.creargastoingreso, null)

        builder.setTitle("Crear nuevo gasto/ingreso")
        builder.setView(dialogLayout)

        val dbHelper = AplicacionDBHelper(this)
        val categorias = dbHelper.obtenerCategoriasPorTipo(tipoCategoria)

        val tipoGastoIngreso: TextView = dialogLayout.findViewById(R.id.crearGastoIngTipo)
        val spinnerGastoIngreso: Spinner = dialogLayout.findViewById(R.id.spinnerGastoIngreso)
        val cantidadGastoIngreso: EditText = dialogLayout.findViewById(R.id.crearGastoIngCantidad)
        val crearFecha: TextView = dialogLayout.findViewById(R.id.crearGastoIngFecha)
        val descripcionGastoIngreso: EditText = dialogLayout.findViewById(R.id.crearGastoIngDescripcion)
        val imagenGastoIngreso: ImageView = dialogLayout.findViewById(R.id.crearGastoIngFactura)

        tipoGastoIngreso.text = tipoCategoria
        this.imagenGastoIngreso = imagenGastoIngreso

        val nombresCategorias = categorias.map { it.nombre }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresCategorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGastoIngreso.adapter = adapter

        crearFecha.setOnClickListener {
            val datePickerFragment = SeleccionFechas(crearFecha)
            datePickerFragment.show(supportFragmentManager, "datePicker")
        }
        evitarRecargaEnOnResume = true
        imagenGastoIngreso.setOnClickListener {
            imagePickerManager.openImagePicker(this)
        }

        builder.setPositiveButton("Crear") { _, _ ->
            val tipo = tipoGastoIngreso.text.toString()
            val categoriaSeleccionada = spinnerGastoIngreso.selectedItem.toString()
            val cantidad = cantidadGastoIngreso.text.toString()
            val fecha = crearFecha.text.toString()
            val descripcion = descripcionGastoIngreso.text.toString()
            val imagenRuta = imagenGastoIngreso.tag?.toString()

            if (imagenRuta.isNullOrEmpty()) {
                Toast.makeText(this, "Por favor, seleccione una imagen antes de continuar", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (tipo.isNotEmpty() && categoriaSeleccionada.isNotEmpty() && cantidad.isNotEmpty() && fecha.isNotEmpty() && descripcion.isNotEmpty()) {
                val cantidadEnDouble = cantidad.toDoubleOrNull() ?: 0.0
                val cantidadCon1Decimal = Math.round(cantidadEnDouble * 10.0) / 10.0

                val categoria = dbHelper.obtenerCategoriaPorNombre(categoriaSeleccionada)
                val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaLong = formatoFecha.parse(fecha)?.time

                if (categoria != null && fechaLong != null && cantidadCon1Decimal != null) {

                    val transaccion = Transacciones(0, cantidadCon1Decimal, fechaLong, descripcion, imagenRuta, categoria.id)
                    dbHelper.insertarTransaccion(transaccion, categoria)

                    val calendar = Calendar.getInstance().apply { timeInMillis = fechaLong }
                    val mes = calendar.get(Calendar.MONTH) + 1
                    val anio = calendar.get(Calendar.YEAR)

                    val presupuestoExistente = dbHelper.obtenerPresupuestoPorCategoriaYMes(categoria.id, mes, anio)

                    if (presupuestoExistente != null) {
                        val totalPresupuesto = presupuestoExistente.total_presupuesto
                        val totalActual = dbHelper.obtenerTotalGastosIngresosPorCategoriaYMes(categoria.id, mes, anio)
                        val faltante = totalPresupuesto - totalActual
                        val porcentajeFaltante = (faltante / totalPresupuesto) * 100

                        val mensaje = obtenerMensajePresupuesto(tipoCategoria, faltante, porcentajeFaltante, totalPresupuesto, totalActual)

                        AlertDialog.Builder(this)
                            .setTitle("Estado del Presupuesto")
                            .setMessage(mensaje)
                            .setPositiveButton("Aceptar") { dialog, _ -> dialog.dismiss() }
                            .show()
                    }

                    dbHelper.close()

                    imagenGastoIngreso.setImageResource(R.drawable.click)
                    imagenGastoIngreso.tag = null
                    evitarRecargaEnOnResume = false

                    if(tipoCategoria == "Gasto" && presupuestoExistente == null){
                        val mediaPlayer = MediaPlayer.create(this, R.raw.creargasto)
                        mediaPlayer.start()

                        mediaPlayer.setOnCompletionListener {
                            it.release()
                        }
                    }else if(tipoCategoria == "Ingreso" && presupuestoExistente == null){
                        val mediaPlayer = MediaPlayer.create(this, R.raw.crearingreso)
                        mediaPlayer.start()

                        mediaPlayer.setOnCompletionListener {
                            it.release()
                        }
                    }

                    Toast.makeText(this, "Transacción creada exitosamente", Toast.LENGTH_SHORT).show()
                    fechaSeleccionada = fechaSeleccionada ?: obtenerFechaActual()
                    cargarTransacciones(fechaSeleccionada!!)

                } else {
                    Toast.makeText(this, "Cantidad, Categoría o Fecha inválida", Toast.LENGTH_SHORT).show()
                }
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    fun editarTransaccion(transaccion: Transacciones) {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogLayout = inflater.inflate(R.layout.editartransaccion, null)

        builder.setTitle("Editar transacción")
        builder.setView(dialogLayout)

        val dbHelper = AplicacionDBHelper(this)

        val categoria = dbHelper.obtenerCategoriaPorId(transaccion.idCategoriaEnTransacciones)
        if (categoria == null) {
            Toast.makeText(this, "Error: No se encontró la categoría", Toast.LENGTH_SHORT).show()
            return
        }
        val categorias = dbHelper.obtenerCategoriasPorTipo(categoria.tipo)

        val tipoGastoIngreso: TextView = dialogLayout.findViewById(R.id.editarGastoIngTipo)
        val spinnerGastoIngreso: Spinner = dialogLayout.findViewById(R.id.spinnerGastoIngresoEditar)
        val cantidadGastoIngreso: EditText = dialogLayout.findViewById(R.id.editarGastoIngCantidad)
        val crearFecha: TextView = dialogLayout.findViewById(R.id.editarGastoIngFecha)
        val descripcionGastoIngreso: EditText = dialogLayout.findViewById(R.id.editarGastoIngDescripcion)
        val imagenGastoIngreso: ImageView = dialogLayout.findViewById(R.id.editarGastoIngFactura)

        this.imagenGastoIngreso = imagenGastoIngreso

        tipoGastoIngreso.text = categoria.tipo
        cantidadGastoIngreso.setText(transaccion.cantidad.toString())
        crearFecha.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(transaccion.fecha)
        descripcionGastoIngreso.setText(transaccion.descripcion)

        if (!transaccion.imagenRuta.isNullOrEmpty()) {
            val archivoImagen = File(transaccion.imagenRuta)
            imagenGastoIngreso.setImageURI(Uri.fromFile(archivoImagen))
            imagenGastoIngreso.tag = transaccion.imagenRuta
        }

        val nombresCategorias = categorias.map { it.nombre }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresCategorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGastoIngreso.adapter = adapter

        val posicionCategoria = nombresCategorias.indexOf(categoria.nombre)
        if (posicionCategoria != -1) {
            spinnerGastoIngreso.setSelection(posicionCategoria)
        }

        crearFecha.setOnClickListener {
            val datePickerFragment = SeleccionFechas(crearFecha)
            datePickerFragment.show(supportFragmentManager, "datePicker")
        }

        val imagePickerManager = ImagePickerManager(this)
        evitarRecargaEnOnResume = true
        imagenGastoIngreso.setOnClickListener {
            imagePickerManager.openImagePicker(this)
        }

        builder.setPositiveButton("Guardar") { _, _ ->
            val tipo = tipoGastoIngreso.text.toString()
            val categoriaSeleccionada = spinnerGastoIngreso.selectedItem.toString()
            val cantidad = cantidadGastoIngreso.text.toString()
            val fecha = crearFecha.text.toString()
            val descripcion = descripcionGastoIngreso.text.toString()
            val imagenRuta = imagenGastoIngreso.tag?.toString()

            if (tipo.isEmpty() || categoriaSeleccionada.isEmpty() || cantidad.isEmpty() || fecha.isEmpty() || descripcion.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val cantidadEnDouble = cantidad.toDoubleOrNull() ?: 0.0
            val cantidadCon1Decimal = Math.round(cantidadEnDouble * 10.0) / 10.0

            val categoriaNueva = dbHelper.obtenerCategoriaPorNombre(categoriaSeleccionada)
            val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaLong = formatoFecha.parse(fecha)?.time

            if (categoriaNueva != null && fechaLong != null && cantidadCon1Decimal != null) {
                val transaccionActualizada = Transacciones(
                    transaccion.id, cantidadCon1Decimal, fechaLong, descripcion, imagenRuta ?: "", categoriaNueva.id
                )

                dbHelper.editarTransaccion(transaccionActualizada, transaccion.fecha)

                val calendar = Calendar.getInstance().apply { timeInMillis = fechaLong }
                val mes = calendar.get(Calendar.MONTH) + 1
                val anio = calendar.get(Calendar.YEAR)

                val presupuestoExistente = dbHelper.obtenerPresupuestoPorCategoriaYMes(categoriaNueva.id, mes, anio)

                if (presupuestoExistente != null) {
                    val totalPresupuesto = presupuestoExistente.total_presupuesto
                    val totalActual = dbHelper.obtenerTotalGastosIngresosPorCategoriaYMes(categoriaNueva.id, mes, anio)
                    val faltante = totalPresupuesto - totalActual
                    val porcentajeFaltante = (faltante / totalPresupuesto) * 100

                    val mensaje = obtenerMensajePresupuesto(tipoGastoIngreso.text.toString(), faltante, porcentajeFaltante, totalPresupuesto, totalActual)

                    AlertDialog.Builder(this)
                        .setTitle("Estado del Presupuesto")
                        .setMessage(mensaje)
                        .setPositiveButton("Aceptar") { dialog, _ -> dialog.dismiss() }
                        .show()
                } else {
                    Toast.makeText(this, "No hay presupuesto para este mes y categoría.", Toast.LENGTH_LONG).show()
                }

                dbHelper.close()
                evitarRecargaEnOnResume = false
                fechaSeleccionada = fechaSeleccionada ?: obtenerFechaActual()
                cargarTransacciones(fechaSeleccionada!!)

                if((tipo == "Gasto" ||  tipo == "Ingreso") && presupuestoExistente == null){
                    val mediaPlayer = MediaPlayer.create(this, R.raw.editado)
                    mediaPlayer.start()

                    mediaPlayer.setOnCompletionListener {
                        it.release()
                    }
                }
                Toast.makeText(this, "Transacción actualizada exitosamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error al actualizar la transacción", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        imagePickerManager.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ImagePickerManager.PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imagePath = imagePickerManager.handleImageSelection(requestCode, resultCode, data, this)
            if (imagePath != null) {
                val selectedImageUri = Uri.fromFile(File(imagePath))
                imagenGastoIngreso.setImageURI(selectedImageUri)
                imagenGastoIngreso.tag = imagePath
            } else {
                Toast.makeText(this, "No se seleccionó ninguna imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun mostrarSegunIndicado() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.mostrarsegunindicado, null)

        val radioGroupBuscar: RadioGroup = dialogLayout.findViewById(R.id.buscarCategoriaRadioGroupBuscar)
        val radioIngresoBuscar: RadioButton = dialogLayout.findViewById(R.id.buscarRadioCategoriaIngreso)
        val radioGastoBuscar: RadioButton = dialogLayout.findViewById(R.id.buscarRadioCategoriaGasto)
        val radioTodoBuscar: RadioButton = dialogLayout.findViewById(R.id.buscarRadioCategoriaTodo)
        val fechaBuscar: TextView = dialogLayout.findViewById(R.id.fechaBuscar)

        val radioGroupDiaOMes: RadioGroup = dialogLayout.findViewById(R.id.buscarCategoriaDiaGroup)
        val radioDiaBuscar: RadioButton = dialogLayout.findViewById(R.id.radioDiaBuscar)
        val radioMesBuscar: RadioButton = dialogLayout.findViewById(R.id.radioMesBuscar)

        builder.setTitle("Buscar según lo indicado")
        builder.setView(dialogLayout)

        fechaBuscar.setOnClickListener {
            val datePickerFragment = SeleccionFechas(fechaBuscar)
            datePickerFragment.show(supportFragmentManager, "datePicker")
        }

        builder.setPositiveButton("Realizar búsqueda") { _, _ ->
            if (!radioIngresoBuscar.isChecked && !radioGastoBuscar.isChecked && !radioTodoBuscar.isChecked) {
                Toast.makeText(this, "Seleccione una categoría", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val fecha = fechaBuscar.text.toString().trim()
            if (fecha.isEmpty()) {
                Toast.makeText(this, "Seleccione una fecha", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val tipoBusqueda = when {
                radioIngresoBuscar.isChecked -> "Ingreso"
                radioGastoBuscar.isChecked -> "Gasto"
                radioTodoBuscar.isChecked -> "Todo"
                else -> "Todo"
            }

            val busquedaPor = when {
                radioDiaBuscar.isChecked -> "dia"
                radioMesBuscar.isChecked -> "mes"
                else -> {
                    Toast.makeText(this, "Seleccione día o mes", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
            }

            val aplicacionDBHelper = AplicacionDBHelper(this)
            val transaccionesFiltradas = aplicacionDBHelper.obtenerTransaccionesPorTipoYFecha(tipoBusqueda, fecha, busquedaPor)

            val formatoDia = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
            val formatoMes = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
            val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            val fechaDate = formatoFecha.parse(fecha) ?: Date()
            this.fechaSeleccionada = fechaDate.time

            val fechaFormateada = if (busquedaPor == "dia") {
                formatoDia.format(fechaDate)
            } else {
                formatoMes.format(fechaDate)
            }

            if (transaccionesFiltradas.isEmpty()) {
                val mensaje = when {
                    tipoBusqueda == "Todo" && busquedaPor == "dia" -> "Sin transacciones para el día $fechaFormateada."
                    tipoBusqueda == "Todo" && busquedaPor == "mes" -> "Sin transacciones para el mes de $fechaFormateada."
                    tipoBusqueda == "Ingreso" && busquedaPor == "dia" -> "Sin ingresos para el día $fechaFormateada."
                    tipoBusqueda == "Ingreso" && busquedaPor == "mes" -> "Sin ingresos para el mes de $fechaFormateada."
                    tipoBusqueda == "Gasto" && busquedaPor == "dia" -> "Sin gastos para el día $fechaFormateada."
                    tipoBusqueda == "Gasto" && busquedaPor == "mes" -> "Sin gastos para el mes de $fechaFormateada."
                    else -> "Sin resultados para la fecha $fechaFormateada."
                }
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
            } else {
                val cantidad = transaccionesFiltradas.size
                val mensaje = if (cantidad == 1) {
                    "Encontrada 1 transacción para $fechaFormateada."
                } else {
                    "Encontradas $cantidad transacciones para $fechaFormateada."
                }
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
            }

            fechaSeleccionada = fechaSeleccionada ?: obtenerFechaActual()
            cargarTransaccionesFiltradas(transaccionesFiltradas)

        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    fun eliminarCategoria() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.eliminarcategoria, null)

        builder.setTitle("Eliminar categoría")
        builder.setView(dialogLayout)

        val radioCategoriaIngresos: RadioButton = dialogLayout.findViewById(R.id.eliminarRadioIngresos)
        val radioCategoriaGastos: RadioButton = dialogLayout.findViewById(R.id.eliminarRadioGastos)
        val spinnerEliminar: Spinner = dialogLayout.findViewById(R.id.spinnerEliminarCategoria)
        val radioGroupEliminarCategoria: RadioGroup = dialogLayout.findViewById(R.id.eliminarCategoriaRadioGroup)

        val dbHelper = AplicacionDBHelper(this)

        val categoriasGasto = dbHelper.obtenerCategoriasPorTipo("Gasto").map { it.nombre }
        val categoriasIngreso = dbHelper.obtenerCategoriasPorTipo("Ingreso").map { it.nombre }

        fun actualizarSpinner(categorias: List<String>) {
            if (categorias.isNotEmpty()) {
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerEliminar.adapter = adapter
                spinnerEliminar.isEnabled = true
            } else {
                val adapterVacio = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("No hay categorías disponibles"))
                adapterVacio.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerEliminar.adapter = adapterVacio
                spinnerEliminar.isEnabled = false
            }
        }

        actualizarSpinner(emptyList())

        radioGroupEliminarCategoria.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.eliminarRadioIngresos -> {
                    actualizarSpinner(categoriasIngreso)
                    if (categoriasIngreso.isEmpty()) {
                        Toast.makeText(this, "No hay categorías de Ingresos disponibles", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.eliminarRadioGastos -> {
                    actualizarSpinner(categoriasGasto)
                    if (categoriasGasto.isEmpty()) {
                        Toast.makeText(this, "No hay categorías de Gastos disponibles", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        builder.setPositiveButton("Eliminar") { _, _ ->
            val categoriaSeleccionada = spinnerEliminar.selectedItem as? String
            if (categoriaSeleccionada != null && categoriaSeleccionada != "No hay categorías disponibles") {
                confirmarEliminacion(categoriaSeleccionada)
            } else {
                Toast.makeText(this, "Debe seleccionar una categoría válida", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    fun confirmarEliminacion(categoria: String) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.eliminarcategoriaconfirmar, null)

        builder.setTitle("Eliminar categoría")
        builder.setView(dialogLayout)

        builder.setPositiveButton("Sí, eliminar") { _, _ ->
            val dbHelper = AplicacionDBHelper(this)
            dbHelper.eliminarCategoriaYActualizarDias(categoria)
            Toast.makeText(this, "Categoría eliminada: $categoria", Toast.LENGTH_SHORT).show()
            fechaSeleccionada = fechaSeleccionada ?: obtenerFechaActual()
            cargarTransacciones(fechaSeleccionada!!)

            val mediaPlayer = MediaPlayer.create(this, R.raw.eliminar)
            mediaPlayer.start()

            mediaPlayer.setOnCompletionListener {
                it.release()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    fun obtenerMensajePresupuesto(tipoCategoria: String, faltante: Double, porcentajeFaltante: Double, totalPresupuesto: Double, totalActual: Double): String {
        var mensaje = ""

        when (tipoCategoria) {
            "Gasto" -> {
                when {
                    faltante < 0 -> {
                        mensaje = "⚠️ ¡Has sobrepasado tu presupuesto!\n\nPresupuesto: $totalPresupuesto\nGastado: $totalActual\nExcedente: ${faltante * -1}"
                        val mediaPlayer = MediaPlayer.create(this, R.raw.fail)
                        mediaPlayer.start()

                        mediaPlayer.setOnCompletionListener {
                            it.release()
                        }
                    }
                    porcentajeFaltante == 0.0 -> {
                        mensaje = "⚠️ ¡Tu presupuesto está agotado! No te queda nada."
                        val mediaPlayer = MediaPlayer.create(this, R.raw.seacaba)
                        mediaPlayer.start()

                        mediaPlayer.setOnCompletionListener {
                            it.release()
                        }
                    }
                    porcentajeFaltante in 1.0..25.0 -> {
                        mensaje = "🛑 ¡Cuidado! Solo te queda el ${porcentajeFaltante.toInt()}% de tu presupuesto.\n\nPresupuesto: $totalPresupuesto\nGastado: $totalActual\nFaltante: $faltante"
                        val mediaPlayer = MediaPlayer.create(this, R.raw.peligro)
                        mediaPlayer.start()

                        mediaPlayer.setOnCompletionListener {
                            it.release()
                        }
                    }
                    porcentajeFaltante in 26.0..50.0 -> {
                        mensaje = "⚠️ Estás utilizando el 50% de tu presupuesto.\n\nPresupuesto: $totalPresupuesto\nGastado: $totalActual\nFaltante: $faltante"
                        val mediaPlayer = MediaPlayer.create(this, R.raw.estadonormal)
                        mediaPlayer.start()

                        mediaPlayer.setOnCompletionListener {
                            it.release()
                        }
                    }
                    porcentajeFaltante > 50.0 -> {
                        mensaje = "✅ ¡Todo bien! Te queda más del 50% de tu presupuesto.\n\nPresupuesto: $totalPresupuesto\nGastado: $totalActual\nFaltante: $faltante"
                        val mediaPlayer = MediaPlayer.create(this, R.raw.bien)
                        mediaPlayer.start()

                        mediaPlayer.setOnCompletionListener {
                            it.release()
                        }
                    }
                }
            }

            "Ingreso" -> {
                when {
                    faltante < 0 -> {
                        mensaje = "✅ ¡Has ingresado más de lo que planeaste! Tu ingreso excede el presupuesto."
                        val mediaPlayer = MediaPlayer.create(this, R.raw.superarobjetivos)
                        mediaPlayer.start()

                        mediaPlayer.setOnCompletionListener {
                            it.release()
                        }
                    }
                    porcentajeFaltante == 0.0 -> {
                        mensaje = "🎉 ¡Tu ingreso es exactamente lo que tenías presupuestado!"
                        val mediaPlayer = MediaPlayer.create(this, R.raw.completado)
                        mediaPlayer.start()

                        mediaPlayer.setOnCompletionListener {
                            it.release()
                        }
                    }
                    porcentajeFaltante in 1.0..25.0 -> {
                        mensaje = "🎯 ¡Casi alcanzas tu objetivo! Solo te queda el ${porcentajeFaltante.toInt()}% de tu presupuesto de ingreso."
                        val mediaPlayer = MediaPlayer.create(this, R.raw.casi)
                        mediaPlayer.start()

                        mediaPlayer.setOnCompletionListener {
                            it.release()
                        }
                    }
                    porcentajeFaltante in 26.0..50.0 -> {
                        val mediaPlayer = MediaPlayer.create(this, R.raw.correcto)
                        mediaPlayer.start()

                        mediaPlayer.setOnCompletionListener {
                            it.release()
                        }
                        mensaje = "👍 ¡Vas bien! Has cubierto más del 50% de tu ingreso previsto."
                    }
                    porcentajeFaltante > 50.0 -> {
                        mensaje = "💰 ¡Excelente! Estás por encima del 50% de tu objetivo de ingreso."
                        val mediaPlayer = MediaPlayer.create(this, R.raw.okay)
                        mediaPlayer.start()

                        mediaPlayer.setOnCompletionListener {
                            it.release()
                        }
                    }
                }
            }
        }

        return mensaje
    }

    override fun onResume() {
        super.onResume()
        if(evitarRecargaEnOnResume == true){
            return
        }
        cargarTransacciones(obtenerFechaActual())
    }

    fun cargarTransacciones(fechaSinTransformar: Long) {
        transacciones = dbHelper.obtenerTransaccionesDelMes(fechaSinTransformar)
        adaptadorTransacciones = AdaptadorTransacciones(transacciones, this, true, fechaSeleccionada) { transaccion ->
            editarTransaccion(transaccion)
        }
        recyclerViewTransacciones.adapter = adaptadorTransacciones

        val textViewNoRegistrosTransacciones = findViewById<TextView>(R.id.mostrarNoHayRegistrosTransacciones)

        if (transacciones.isNotEmpty()) {
            textViewNoRegistrosTransacciones.visibility = View.GONE
        } else {
            textViewNoRegistrosTransacciones.visibility = View.VISIBLE
        }

        dbHelper.close()
    }

    fun cargarTransaccionesFiltradas(transaccionesFiltradas: MutableList<Transacciones>) {
        adaptadorTransacciones = AdaptadorTransacciones(transaccionesFiltradas, this, true, fechaSeleccionada) { transaccion ->
            editarTransaccion(transaccion)
        }
        recyclerViewTransacciones.adapter = adaptadorTransacciones

        val textViewNoRegistrosTransacciones = findViewById<TextView>(R.id.mostrarNoHayRegistrosTransacciones)

        if (transaccionesFiltradas.isNotEmpty()) {
            textViewNoRegistrosTransacciones.visibility = View.GONE
        } else {
            textViewNoRegistrosTransacciones.visibility = View.VISIBLE
        }

        dbHelper.close()
    }

    private fun obtenerFechaActual(): Long {
        val calendario = Calendar.getInstance()

        calendario.set(Calendar.DAY_OF_MONTH, 1)

        calendario.set(Calendar.HOUR_OF_DAY, 0)
        calendario.set(Calendar.MINUTE, 0)
        calendario.set(Calendar.SECOND, 0)
        calendario.set(Calendar.MILLISECOND, 0)

        return calendario.timeInMillis
    }

}