package com.oscar.sincarnet

/**
 * Modelo de datos que representa el estado de progreso de la impresión de documentos.
 *
 * Se utiliza en la interfaz de usuario para mostrar el estado y el avance de la impresión
 * de atestados y otros documentos en las impresoras Bluetooth.
 *
 * @property isVisible Indica si el diálogo de progreso debe mostrarse al usuario.
 * @property currentDoc Nombre o descripción del documento que se está imprimiendo actualmente.
 * @property currentIndex Índice del documento actual en la secuencia (base 0).
 * @property totalDocs Número total de documentos a imprimir.
 * @property isError Indica si ha ocurrido un error durante la impresión.
 * @property errorMessage Mensaje descriptivo del error, vacío si no hay error.
 */
data class PrintProgress(
    val isVisible: Boolean = false,
    val currentDoc: String = "",
    val currentIndex: Int = 0,
    val totalDocs: Int = 0,
    val isError: Boolean = false,
    val errorMessage: String = ""
)
