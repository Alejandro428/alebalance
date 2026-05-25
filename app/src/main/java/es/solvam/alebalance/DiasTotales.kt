package es.solvam.alebalance

data class DiasTotales(
    val id_dia: Int,
    val fecha_dia: Long,
    var ingresos_dia: Double,
    var gastos_dia: Double
)

