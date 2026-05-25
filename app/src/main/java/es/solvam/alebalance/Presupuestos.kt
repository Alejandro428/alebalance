package es.solvam.alebalance

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Presupuestos : AppCompatActivity() {

    private lateinit var dbHelper: AplicacionDBHelper
    private lateinit var adaptadorPresupuestos: AdaptadorPresupuestos
    private lateinit var recyclerViewPresupuestos: RecyclerView
    private lateinit var presupuestos: MutableList<Presupuesto>
    private var fechaSeleccionada: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.presupuestos)

        dbHelper = AplicacionDBHelper(this)
        recyclerViewPresupuestos = findViewById(R.id.recyclerPresupuestos)
        recyclerViewPresupuestos.layoutManager = LinearLayoutManager(this)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation_presupuestos)
        bottomNavigationView.setSelectedItemId(R.id.presupuestos)

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

        var botonPresupuesto : FloatingActionButton = findViewById(R.id.accionPresupuesto)
        botonPresupuesto.setOnClickListener(){view ->
            val mediaPlayer = MediaPlayer.create(this, R.raw.botontransaccionprincipal)
            mediaPlayer.start()

            mediaPlayer.setOnCompletionListener {
                it.release()
            }
            mostrarPopupMenuPresupuestos(view, this)
        }

    }

    fun mostrarPopupMenuPresupuestos(view: View, contexto: Context) {
        val popupMenu = PopupMenu(contexto, view)
        popupMenu.menuInflater.inflate(R.menu.menupresupuesto, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.crear_presupuesto -> {
                    val dbHelper = AplicacionDBHelper(this)
                    if (dbHelper.existenCategoriasDeTipo("Gasto") == true || dbHelper.existenCategoriasDeTipo("Ingreso") == true) {
                        crearPresupuesto()
                    } else {
                        Toast.makeText(this, "Primero debes crear al menos una categoría de Gasto o Ingreso", Toast.LENGTH_SHORT).show()
                    }
                    dbHelper.close()
                    true
                }
                R.id.mostrar_segun_mes -> {
                    val dbHelper = AplicacionDBHelper(this)
                    if (dbHelper.existenPresupuestos()) {
                        mostrarSegunMesIntroducido()
                    } else {
                        Toast.makeText(this, "No hay presupuestos registrados.", Toast.LENGTH_SHORT).show()
                    }
                    dbHelper.close()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    fun crearPresupuesto() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.crearpresupuesto, null)

        builder.setTitle("Crear presupuesto")
        builder.setView(dialogLayout)

        val dbHelper = AplicacionDBHelper(this)
        val categorias = dbHelper.getAllCategorias()

        val spinnerPresupuesto: Spinner = dialogLayout.findViewById(R.id.crearPresupuestoSpinner)
        val cantidadPresupuesto: EditText = dialogLayout.findViewById(R.id.crearPresupuestoCantidad)
        val fechaPresupuesto: TextView = dialogLayout.findViewById(R.id.crearFechaPresupuesto)

        val nombresCategorias = categorias.map { it.nombre }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresCategorias).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerPresupuesto.adapter = adapter

        fechaPresupuesto.setOnClickListener {
            val datePickerFragment = SeleccionFechas(fechaPresupuesto)
            datePickerFragment.show(supportFragmentManager, "datePicker")
        }

        builder.setPositiveButton("Crear") { _, _ ->
            val categoriaSeleccionada = spinnerPresupuesto.selectedItem.toString()
            val cantidad = cantidadPresupuesto.text.toString()
            val fecha = fechaPresupuesto.text.toString()

            if (categoriaSeleccionada.isNotEmpty() && cantidad.isNotEmpty() && fecha.isNotEmpty()) {
                val cantidadEnDouble = cantidad.toDoubleOrNull() ?: 0.0
                val cantidadCon1Decimal = Math.round(cantidadEnDouble * 10.0) / 10.0
                val categoria = dbHelper.obtenerCategoriaPorNombre(categoriaSeleccionada)

                val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaDate = formatoFecha.parse(fecha)

                if (categoria != null && fechaDate != null && cantidadCon1Decimal != null) {
                    val calendar = Calendar.getInstance()
                    calendar.time = fechaDate
                    val mes = calendar.get(Calendar.MONTH) + 1
                    val anio = calendar.get(Calendar.YEAR)

                    val presupuestoExistente = dbHelper.obtenerPresupuestoPorCategoriaYMes(categoria.id, mes, anio)

                    if (presupuestoExistente != null) {
                        Toast.makeText(this, "Ya existe un presupuesto para la categoría en el mes.", Toast.LENGTH_SHORT).show()
                    } else {
                        val nuevoPresupuesto = Presupuesto(0, categoria.id, mes, anio, cantidadCon1Decimal)
                        dbHelper.insertarPresupuesto(nuevoPresupuesto)
                        dbHelper.close()

                        Toast.makeText(this, "Presupuesto creado exitosamente", Toast.LENGTH_SHORT).show()
                        fechaSeleccionada = fechaSeleccionada ?: obtenerFechaActual()
                        cargarPresupuestos(fechaSeleccionada!!)

                        val mediaPlayer = MediaPlayer.create(this, R.raw.notificacionpresupuesto)
                        mediaPlayer.start()

                        mediaPlayer.setOnCompletionListener {
                            it.release()
                        }
                    }
                } else {
                    Toast.makeText(this, "Cantidad, categoría o fecha incorrecta", Toast.LENGTH_SHORT).show()
                }
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    fun mostrarSegunMesIntroducido() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.mostrarsegunfecha, null)
        builder.setView(dialogLayout)

        builder.setTitle("Mostrar según mes introducido")

        val fechaBuscarPresupuesto: TextView = dialogLayout.findViewById(R.id.fechaBuscarPresupuesto)

        fechaBuscarPresupuesto.setOnClickListener {
            val datePickerFragment = SeleccionFechas(fechaBuscarPresupuesto)
            datePickerFragment.show(supportFragmentManager, "datePicker")
        }

        builder.setPositiveButton("Aceptar") { dialog, _ ->
            val fechaSeleccionadaInt = fechaBuscarPresupuesto.text.toString().trim()

            if (fechaSeleccionadaInt.isEmpty()) {
                Toast.makeText(this, "Por favor, selecciona una fecha", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaDate = formatoFecha.parse(fechaSeleccionadaInt)

            val fechaSeleccionadaLong = fechaDate?.time ?: obtenerFechaActual()

            this.fechaSeleccionada = fechaSeleccionadaLong
            val presupuestos = dbHelper.obtenerPresupuestosPorFecha(fechaSeleccionadaInt)

            fechaSeleccionada = fechaSeleccionada ?: obtenerFechaActual()
            cargarPresupuestosConListaDirectamente(presupuestos)

            if (presupuestos.isNotEmpty()) {
                Toast.makeText(this, "Se encontraron ${presupuestos.size} presupuestos.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No se encontraron presupuestos para la fecha $fechaSeleccionadaInt.", Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        cargarPresupuestos(obtenerFechaActual())
    }

    fun cargarPresupuestos(fecha: Long) {
        presupuestos = dbHelper.obtenerPresupuestosDelMes(fecha)
        adaptadorPresupuestos = AdaptadorPresupuestos(presupuestos, this, true, fechaSeleccionada)
        recyclerViewPresupuestos.adapter = adaptadorPresupuestos

        val textViewNoRegistros = findViewById<TextView>(R.id.mostrarNoHayRegistros)

        if (presupuestos.isNotEmpty()) {
            textViewNoRegistros.visibility = View.GONE
        } else {
            textViewNoRegistros.visibility = View.VISIBLE
        }
    }

    fun cargarPresupuestosConListaDirectamente(presupuestos: MutableList<Presupuesto>) {
        adaptadorPresupuestos = AdaptadorPresupuestos(presupuestos, this, true, fechaSeleccionada)
        recyclerViewPresupuestos.adapter = adaptadorPresupuestos

        val textViewNoRegistros = findViewById<TextView>(R.id.mostrarNoHayRegistros)

        if (presupuestos.isNotEmpty()) {
            textViewNoRegistros.visibility = View.GONE
        } else {
            textViewNoRegistros.visibility = View.VISIBLE
        }
    }

    fun obtenerFechaActual(): Long {
        val calendario = Calendar.getInstance()
        val mesActual = calendario.get(Calendar.MONTH) + 1
        val anioActual = calendario.get(Calendar.YEAR)
        return obtenerFechaDesdeMesAnio(mesActual, anioActual)
    }

    fun obtenerFechaDesdeMesAnio(mes: Int, anio: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, anio)
        calendar.set(Calendar.MONTH, mes - 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return calendar.timeInMillis
    }

    fun obtenerFechaFormateada(fechaEnMilisegundos: Long): String {
        val formato = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val calendario = Calendar.getInstance()
        calendario.timeInMillis = fechaEnMilisegundos
        return formato.format(calendario.time)
    }
}