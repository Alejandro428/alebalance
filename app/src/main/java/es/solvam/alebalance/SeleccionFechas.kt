package es.solvam.alebalance

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SeleccionFechas(private val textViewFecha: TextView) : DialogFragment(), DatePickerDialog.OnDateSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        return DatePickerDialog(requireContext(), this, year, month, day)
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        val calendario = Calendar.getInstance()
        calendario.set(year, month, day)

        val fechaEnMilisegundos = calendario.timeInMillis

        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaFormateada = formato.format(fechaEnMilisegundos)

        textViewFecha.text = fechaFormateada

        if (activity is MainActivity) {
            val mainActivity = activity as MainActivity
            mainActivity.cargarDias(fechaEnMilisegundos, mostrarToast = true)
            mainActivity.cargarTotalMes(fechaEnMilisegundos)
        }
    }
}