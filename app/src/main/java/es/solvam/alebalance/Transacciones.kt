package es.solvam.alebalance

data class Transacciones(
    val id: Int = 0,
    var cantidad: Double,
    var fecha: Long,
    val descripcion: String,
    val imagenRuta: String,
    val idCategoriaEnTransacciones: Int
){}