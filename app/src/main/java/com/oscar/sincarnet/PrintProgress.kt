package com.oscar.sincarnet

data class PrintProgress(
    val isVisible: Boolean = false,
    val currentDoc: String = "",
    val currentIndex: Int = 0,
    val totalDocs: Int = 0,
    val isError: Boolean = false,
    val errorMessage: String = ""
)
