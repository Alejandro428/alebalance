package es.solvam.alebalance

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AcercaDe : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acercade)

        val botonRegresar: Button = findViewById(R.id.regresarInicio)

        botonRegresar.setOnClickListener {

            val mediaPlayer = MediaPlayer.create(this, R.raw.regresar)
            mediaPlayer.start()

            mediaPlayer.setOnCompletionListener {
                it.release()
            }

            Toast.makeText(this, "Regresando al inicio", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}