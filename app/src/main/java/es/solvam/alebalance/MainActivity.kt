package es.solvam.alebalance

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var fechaActual: String
    private lateinit var adaptadorDias: AdaptadorDias
    private lateinit var aplicacionDBHelper: AplicacionDBHelper
    private lateinit var recyclerViewDias: RecyclerView
    private lateinit var diasTotales: MutableList<DiasTotales>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var textoCambiarMes : TextView = findViewById(R.id.textoCambiarMes)
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        textoCambiarMes.setText(formato.format(obtenerFechaActual()))

        val toolbar : Toolbar = findViewById(R.id.toolbarMain)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        aplicacionDBHelper = AplicacionDBHelper(this)
        recyclerViewDias = findViewById(R.id.recyclerGastosMensuales)
        recyclerViewDias.layoutManager = LinearLayoutManager(this)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation_main)

        bottomNavigationView.setSelectedItemId(R.id.inicio)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.inicio -> {
                    val mediaPlayer = MediaPlayer.create(this, R.raw.menuabajo)
                    mediaPlayer.start()

                    mediaPlayer.setOnCompletionListener {
                        it.release()
                    }
                    startActivity(Intent(this, MainActivity::class.java))
                    textoCambiarMes.setText(formato.format(obtenerFechaActual()))
                    true
                }
                R.id.categorias -> {
                    val mediaPlayer = MediaPlayer.create(this, R.raw.menuabajo)
                    mediaPlayer.start()

                    mediaPlayer.setOnCompletionListener {
                        it.release()
                    }
                    startActivity(Intent(this, Categorias::class.java))
                    textoCambiarMes.setText(formato.format(obtenerFechaActual()))
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

        var cartaConTotal : MaterialCardView = findViewById(R.id.cartaCrearCategoria)

        cartaConTotal.setOnClickListener() {
            val datePickerFragment = SeleccionFechas(textoCambiarMes)
            datePickerFragment.show(supportFragmentManager, "datePicker")

            val mediaPlayer = MediaPlayer.create(this, R.raw.escogerfecha)
            mediaPlayer.start()

            mediaPlayer.setOnCompletionListener {
                it.release()
            }

            var textoSinFormateo = textoCambiarMes.text.toString()
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fecha = formato.parse(textoSinFormateo)
            val fechaEnMilisegundos = fecha?.time ?: 0

            textoCambiarMes.setText(formato.format(fechaEnMilisegundos))
            cargarDias(fechaEnMilisegundos)
            cargarTotalMes(fechaEnMilisegundos)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menutoolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.acercaDe -> {
                val mediaPlayer = MediaPlayer.create(this, R.raw.okay)
                mediaPlayer.start()

                mediaPlayer.setOnCompletionListener {
                    it.release()
                }

                val intent = Intent(this, AcercaDe::class.java)
                startActivity(intent)
                Toast.makeText(this, "Abriendo acerca de", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDias(obtenerFechaActual())
        cargarTotalMes(obtenerFechaActual())
        cargarTotal()
    }

    fun cargarDias(fechaSinTransformar: Long, mostrarToast: Boolean = false) {
        val formatoFecha = SimpleDateFormat("MMMM 'de' yyyy", Locale("es", "ES"))
        val fechaFormateada = formatoFecha.format(Date(fechaSinTransformar)).replaceFirstChar { it.uppercase() }

        if (mostrarToast) {
            Toast.makeText(this, "Cargando días del mes $fechaFormateada", Toast.LENGTH_SHORT).show()
        }

        diasTotales = aplicacionDBHelper.obtenerDiasDelMes(fechaSinTransformar)
        adaptadorDias = AdaptadorDias(diasTotales, this, false)
        recyclerViewDias.adapter = adaptadorDias

        val textViewNoRegistrosDias = findViewById<TextView>(R.id.mostrarNoHayRegistrosDias)

        if (diasTotales.isNotEmpty()) {
            textViewNoRegistrosDias.visibility = View.GONE
        } else {
            textViewNoRegistrosDias.visibility = View.VISIBLE
        }
    }

    fun cargarTotalMes(fechaSinTransformar: Long) {
        val datosTotales = aplicacionDBHelper.obtenerTotalesDelMes(fechaSinTransformar)

        val ingresosMes = datosTotales[0]
        val gastosMes = datosTotales[1]
        val totalMes = datosTotales[2]

        val ingresoNumero: TextView = findViewById(R.id.ingresoNumero)
        val gastoNumero: TextView = findViewById(R.id.gastoNumero)
        val balanceNumero: TextView = findViewById(R.id.balanceNumero)
        val gastoTotalMes: TextView = findViewById(R.id.gastoTotalMensual)
        val mesYAño: TextView = findViewById(R.id.mesYAñoResumen)

        ingresoNumero.text = (ingresosMes.toString()+"€")
        gastoNumero.text = (gastosMes.toString()+"€")
        balanceNumero.text = (totalMes.toString()+"€")
        gastoTotalMes.text = (totalMes.toString()+"€")

        val formatoFecha = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        val fechaFormateada = formatoFecha.format(Date(fechaSinTransformar))

        val fechaCapitalizada = fechaFormateada.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
        mesYAño.text = fechaCapitalizada
    }

    fun cargarTotal(){
        val totalCuenta = aplicacionDBHelper.obtenerTotalCuenta()
        val totalCuentaTexto : TextView = findViewById(R.id.gastoTotal)
        totalCuentaTexto.setText(totalCuenta.toString()+"€")
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
