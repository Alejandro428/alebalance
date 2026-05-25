package es.solvam.alebalance

data class Presupuesto(
    val id_presupuesto: Int,
    val presupuesto_categoria_id: Int,
    val mes_presupuesto: Int,
    val anio_presupuesto: Int,
    val total_presupuesto: Double
)