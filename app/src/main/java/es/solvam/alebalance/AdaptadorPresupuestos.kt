package es.solvam.alebalance

import android.content.Context
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdaptadorPresupuestos(var presupuestosIntroducidos: MutableList<Presupuesto>, var contexto: Context, var modificable: Boolean, var fechaSeleccionada: Long?) : RecyclerView.Adapter<AdaptadorPresupuestos.VistaPresupuestos>() {

    class VistaPresupuestos(view: View) : RecyclerView.ViewHolder(view) {
        var cartaPresupuesto = view.findViewById<MaterialCardView>(R.id.cartaPresupuesto)
        var vistaCategoriaPresupuesto = view.findViewById<TextView>(R.id.vistaCategoriaPresupuesto)
        var vistaMesPresupuesto = view.findViewById<TextView>(R.id.vistaMesPresupuesto)
        var vistaAñoPresupuesto = view.findViewById<TextView>(R.id.vistaAñoPresupuesto)
        var vistaMontoPresupuesto = view.findViewById<TextView>(R.id.vistaMontoPresupuesto)
        var vistaCantidadConsumida = view.findViewById<TextView>(R.id.vistaCantidadConsumida)

        fun bind(presupuesto: Presupuesto, contexto: Context, modificable: Boolean, adaptadorPresupuestos: AdaptadorPresupuestos) {
            val dbHelper = AplicacionDBHelper(contexto)
            val cantidadConsumida = dbHelper.obtenerCantidadConsumida(presupuesto)

            val categoria = dbHelper.obtenerCategoriaPorId(presupuesto.presupuesto_categoria_id)
            vistaCategoriaPresupuesto.text = categoria?.nombre ?: "Categoría no encontrada"

            vistaMesPresupuesto.text = obtenerNombreMes(presupuesto.mes_presupuesto)
            vistaAñoPresupuesto.text = presupuesto.anio_presupuesto.toString()
            vistaMontoPresupuesto.text = (presupuesto.total_presupuesto.toString()+"€")
            vistaCantidadConsumida.text = (cantidadConsumida.toString()+"€")

            if (modificable) {
                cartaPresupuesto.setOnClickListener { view ->
                    adaptadorPresupuestos.mostrarPopupMenuEditarEliminarPresupuesto(view, contexto, presupuesto)
                }
            }
        }

        private fun obtenerNombreMes(mes: Int): String {
            val formatoMes = SimpleDateFormat("MMMM", Locale("es", "ES"))
            val fecha = Calendar.getInstance()
            fecha.set(Calendar.MONTH, mes - 1)
            return formatoMes.format(fecha.time)
        }
    }

    fun mostrarPopupMenuEditarEliminarPresupuesto(view: View, contexto: Context, presupuesto: Presupuesto) {
        val popupMenu = PopupMenu(contexto, view)
        popupMenu.menuInflater.inflate(R.menu.menupresupuestoedicion, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.editar_presupuesto -> {
                    editarPresupuesto(contexto, presupuesto)
                    true
                }
                R.id.eliminar_presupuesto -> {
                    eliminarPresupuesto(contexto, presupuesto)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }


    fun editarPresupuesto(contexto: Context, presupuesto: Presupuesto) {
        val builder = AlertDialog.Builder(contexto)
        val inflater = LayoutInflater.from(contexto)
        val dialogLayout = inflater.inflate(R.layout.editarpresupuesto, null)

        builder.setTitle("Editar presupuesto")
        builder.setView(dialogLayout)

        val dbHelper = AplicacionDBHelper(contexto)
        val categoria = dbHelper.obtenerCategoriaPorId(presupuesto.presupuesto_categoria_id)

        if (categoria == null) {
            Toast.makeText(contexto, "Error: No se encontró la categoría", Toast.LENGTH_SHORT).show()
            return
        }
        dbHelper.close()

        val categoriaPresupuesto: TextView = dialogLayout.findViewById(R.id.editarCategoriaPresupuesto)
        val cantidadPresupuesto: EditText = dialogLayout.findViewById(R.id.editarPresupuestoCantidad)
        val fechaPresupuesto: TextView = dialogLayout.findViewById(R.id.editarFechaPresupuesto)

        // Rellenar datos previos
        categoriaPresupuesto.text = categoria.nombre
        cantidadPresupuesto.setText(presupuesto.total_presupuesto.toString())

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, presupuesto.anio_presupuesto)
        calendar.set(Calendar.MONTH, presupuesto.mes_presupuesto - 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaFormateada = formatoFecha.format(calendar.time)

        fechaPresupuesto.text = fechaFormateada

        fechaPresupuesto.setOnClickListener {
            if (contexto is AppCompatActivity) {
                val fragmentManager = contexto.supportFragmentManager
                val datePickerFragment = SeleccionFechas(fechaPresupuesto)
                datePickerFragment.show(fragmentManager, "datePicker")
            }
        }

        builder.setPositiveButton("Guardar") { _, _ ->
            val cantidad = cantidadPresupuesto.text.toString()
            val fecha = fechaPresupuesto.text.toString()

            if (cantidad.isNotEmpty() && fecha.isNotEmpty()) {
                val cantidadEnDouble = cantidad.toDoubleOrNull()

                val fechaDate = formatoFecha.parse(fecha)

                if (fechaDate != null && cantidadEnDouble != null) {
                    val calendar = Calendar.getInstance()
                    calendar.time = fechaDate
                    val mes = calendar.get(Calendar.MONTH) + 1
                    val anio = calendar.get(Calendar.YEAR)

                    val presupuestoExistente = dbHelper.obtenerPresupuestoPorCategoriaYMes(categoria.id, mes, anio)

                    if (presupuestoExistente != null && presupuestoExistente.id_presupuesto != presupuesto.id_presupuesto) {
                        Toast.makeText(contexto, "Ya existe un presupuesto de la categoria en el mes", Toast.LENGTH_SHORT).show()
                    } else {
                        val presupuestoActualizado = Presupuesto(
                            presupuesto.id_presupuesto, categoria.id, mes, anio, cantidadEnDouble
                        )
                        dbHelper.actualizarPresupuesto(presupuestoActualizado)

                        Toast.makeText(contexto, "Presupuesto actualizado exitosamente", Toast.LENGTH_SHORT).show()
                        if (fechaSeleccionada == null) {
                            (contexto as? Presupuestos)?.cargarPresupuestos(obtenerFechaActual())
                        } else {
                            (contexto as? Presupuestos)?.cargarPresupuestos(fechaSeleccionada!!)
                        }

                        val mediaPlayer = MediaPlayer.create(contexto, R.raw.editado)
                        mediaPlayer.start()

                        mediaPlayer.setOnCompletionListener {
                            it.release()
                        }

                    }
                } else {
                    Toast.makeText(contexto, "Cantidad o fecha incorrecta", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(contexto, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    fun eliminarPresupuesto(contexto: Context, presupuesto: Presupuesto) {
        val builder = AlertDialog.Builder(contexto)
        val inflater = LayoutInflater.from(contexto)
        val dialogLayout = inflater.inflate(R.layout.eliminarpresupuesto, null)

        builder.setView(dialogLayout)

        builder.setPositiveButton("Eliminar") { _, _ ->
            val dbHelper = AplicacionDBHelper(contexto)

            dbHelper.eliminarPresupuesto(presupuesto)
            if (fechaSeleccionada == null) {
                (contexto as? Presupuestos)?.cargarPresupuestos(obtenerFechaActual())
            } else {
                (contexto as? Presupuestos)?.cargarPresupuestos(fechaSeleccionada!!)
            }

            val mediaPlayer = MediaPlayer.create(contexto, R.raw.eliminar)
            mediaPlayer.start()

            mediaPlayer.setOnCompletionListener {
                it.release()
            }

            Toast.makeText(contexto, "Transacción eliminada correctamente.", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            Toast.makeText(contexto, "Transacción cancelada de ser eliminada.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        builder.show()
    }

    fun obtenerFechaDesdeMesAnio(mes: Int, anio: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.set(anio, mes - 1, 1)
        return calendar.time
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VistaPresupuestos {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.vistapresupuesto, parent, false)
        return VistaPresupuestos(view)
    }

    override fun onBindViewHolder(holder: VistaPresupuestos, position: Int) {
        val presupuesto = presupuestosIntroducidos[position]
        holder.bind(presupuesto, contexto, modificable, this)
    }

    override fun getItemCount(): Int {
        return presupuestosIntroducidos.size
    }
}