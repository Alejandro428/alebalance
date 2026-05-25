package es.solvam.alebalance

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdaptadorDias(var diasIntroducidos: MutableList<DiasTotales>, var contexto: Context, var modificable: Boolean) : RecyclerView.Adapter<AdaptadorDias.VistaDias>() {

    class VistaDias(view: View) : RecyclerView.ViewHolder(view) {
        var cartaDia = view.findViewById<MaterialCardView>(R.id.cartaDia)
        var vistaDiaActual = view.findViewById<TextView>(R.id.vistaDiaActual)
        var vistaDiaPalabra = view.findViewById<TextView>(R.id.vistaDiaPalabra)
        var vistaMesYAño = view.findViewById<TextView>(R.id.vistaMesYAño)
        var vistaDiaIngreso = view.findViewById<TextView>(R.id.vistaMontoPresupuesto)
        var vistaDiaGasto = view.findViewById<TextView>(R.id.vistaDiaGasto)

        fun bind(dia: DiasTotales, contexto: Context, modificable: Boolean) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = dia.fecha_dia

            val diaActual = calendar.get(Calendar.DAY_OF_MONTH)
            val mesActual = calendar.get(Calendar.MONTH) + 1
            val añoActual = calendar.get(Calendar.YEAR)
            val diaSemanaNumero = calendar.get(Calendar.DAY_OF_WEEK)
            val diasSemana = arrayOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")

            val formato = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
            val mesAnio = formato.format(Date(dia.fecha_dia))

            val diaSemanaTexto = diasSemana[diaSemanaNumero - 1]

            vistaDiaActual.text = diaActual.toString()
            vistaDiaPalabra.text = diaSemanaTexto
            vistaMesYAño.text = mesAnio

            vistaDiaIngreso.text = "${dia.ingresos_dia}€"
            vistaDiaGasto.text = "${dia.gastos_dia}€"

            if (modificable) {
                cartaDia.setOnClickListener { view ->
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VistaDias {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.vistadia, parent, false)
        return VistaDias(view)
    }

    override fun onBindViewHolder(holder: VistaDias, position: Int) {
        val diaTotales = diasIntroducidos[position]
        holder.bind(diaTotales, contexto, modificable)
    }

    override fun getItemCount(): Int {
        return diasIntroducidos.size
    }
}