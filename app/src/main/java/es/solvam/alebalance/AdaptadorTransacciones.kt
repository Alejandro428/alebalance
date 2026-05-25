package es.solvam.alebalance

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdaptadorTransacciones(var transaccionesIntroducidos: MutableList<Transacciones>, var contexto: Context, var modificable: Boolean, var fechaSeleccionada : Long?, private val onEditListener: (Transacciones) -> Unit) : RecyclerView.Adapter<AdaptadorTransacciones.VistaTransaccion>() {

    class VistaTransaccion(view: View) : RecyclerView.ViewHolder(view) {

        var cartaGastoIngreso = view.findViewById<MaterialCardView>(R.id.vistaCartaGastoCategoria)
        var tipoIngresoGasto = view.findViewById<TextView>(R.id.vistaTipoIngresoGasto)
        var fechaIngresoGasto = view.findViewById<TextView>(R.id.vistaFechaIngresoGasto)
        var categoriaIngresoGasto = view.findViewById<TextView>(R.id.vistaCategoriaIngresoGasto)
        var cantidadIngresoGasto = view.findViewById<TextView>(R.id.vistaCantidadIngresoGasto)
        var descripcionIngresoGasto = view.findViewById<TextView>(R.id.vistaDescripcionGastoIngreso)
        var facturaIngresoGasto = view.findViewById<ImageView>(R.id.vistaFacturaIngresoGasto)

        fun bind(transaccion: Transacciones, contexto: Context, modificable: Boolean, adaptadorTransacciones: AdaptadorTransacciones) {

            var aplicacionDBHelper = AplicacionDBHelper(contexto)
            var categoriaTransaccion = aplicacionDBHelper.obtenerCategoriaPorId(transaccion.idCategoriaEnTransacciones)

            if (categoriaTransaccion != null) {
                tipoIngresoGasto.text = categoriaTransaccion.tipo
            }

            if (categoriaTransaccion != null) {
                categoriaIngresoGasto.text = categoriaTransaccion.nombre
            }

            val fechaFormateada = convertirFecha(transaccion.fecha)
            fechaIngresoGasto.text = fechaFormateada

            cantidadIngresoGasto.text = (transaccion.cantidad.toString() + "€")
            descripcionIngresoGasto.text = transaccion.descripcion

            val archivoImagen = File(transaccion.imagenRuta)
            facturaIngresoGasto.setImageURI(Uri.fromFile(archivoImagen))

            if (modificable == true) {
                cartaGastoIngreso.setOnClickListener { view ->
                    adaptadorTransacciones.mostrarPopupMenuTransaccion(view, contexto, transaccion)
                }
            }
        }

        fun convertirFecha(longFecha: Long, formato: String = "dd/MM/yyyy"): String {
            val date = Date(longFecha) // Convertir Long a Date
            val dateFormat = SimpleDateFormat(formato, Locale.getDefault()) // Aplicar formato
            return dateFormat.format(date) // Devolver fecha formateada
        }
    }

    fun mostrarPopupMenuTransaccion(view: View, contexto: Context, transaccion: Transacciones) {
        val popupMenu = PopupMenu(contexto, view)
        popupMenu.menuInflater.inflate(R.menu.menueditareliminar, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.editar_transaccion -> {
                    onEditListener(transaccion)
                    true
                }
                R.id.eliminar_transaccion -> {
                    eliminarTransaccion(transaccion)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    fun eliminarTransaccion(transaccion: Transacciones) {
        val builder = AlertDialog.Builder(contexto)
        val inflater = LayoutInflater.from(contexto)
        val dialogLayout = inflater.inflate(R.layout.eliminartransaccion, null)

        builder.setView(dialogLayout)

        builder.setPositiveButton("Eliminar") { _, _ ->
            val dbHelper = AplicacionDBHelper(contexto)
            var categoria = dbHelper.obtenerCategoriaPorId(transaccion.idCategoriaEnTransacciones)
            dbHelper.eliminarTransaccion(transaccion)

            val calendar = Calendar.getInstance().apply { timeInMillis = transaccion.fecha }
            val mes = calendar.get(Calendar.MONTH) + 1
            val anio = calendar.get(Calendar.YEAR)

            val presupuestoExistente = dbHelper.obtenerPresupuestoPorCategoriaYMes(transaccion.idCategoriaEnTransacciones, mes, anio)

            if (presupuestoExistente != null) {
                val totalPresupuesto = presupuestoExistente.total_presupuesto
                val totalActual = dbHelper.obtenerTotalGastosIngresosPorCategoriaYMes(transaccion.idCategoriaEnTransacciones, mes, anio)
                val faltante = totalPresupuesto - totalActual
                val porcentajeFaltante = (faltante / totalPresupuesto) * 100

                var mensaje = ""
                if(contexto is Categorias){
                    if (categoria != null) {
                        mensaje = (contexto as Categorias).obtenerMensajePresupuesto(categoria.tipo, faltante, porcentajeFaltante, totalPresupuesto, totalActual)
                    }
                }


                AlertDialog.Builder(contexto)
                    .setTitle("Estado del Presupuesto")
                    .setMessage(mensaje)
                    .setPositiveButton("Aceptar") { dialog, _ -> dialog.dismiss() }
                    .show()
            } else {
                Toast.makeText(contexto, "No hay presupuesto para este mes y categoría.", Toast.LENGTH_LONG).show()
            }

            if (fechaSeleccionada == null) {
                (contexto as? Categorias)?.cargarTransacciones(obtenerFechaActual())
            } else {
                (contexto as? Categorias)?.cargarTransacciones(fechaSeleccionada!!)
            }

            if(presupuestoExistente == null){
                val mediaPlayer = MediaPlayer.create(contexto, R.raw.eliminar)
                mediaPlayer.start()

                mediaPlayer.setOnCompletionListener {
                    it.release()
                }
            }


            Toast.makeText(contexto, "Transacción eliminada correctamente.", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            Toast.makeText(contexto, "Transacción cancelada de ser eliminada.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        builder.show()
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VistaTransaccion {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.vistagastoingreso, parent, false)
        return VistaTransaccion(view)
    }


    override fun onBindViewHolder(holder: VistaTransaccion, position: Int) {
        val transaccion = transaccionesIntroducidos[position]
        holder.bind(transaccion, contexto, modificable, this)
    }

    override fun getItemCount(): Int {
        return transaccionesIntroducidos.size
    }
}