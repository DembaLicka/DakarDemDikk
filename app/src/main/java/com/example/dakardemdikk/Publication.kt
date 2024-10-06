package com.example.dakardemdikk

data class Publication(
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val description: String = "",
    val isVideo: Boolean = false // Ajoutez ce champ pour identifier le type
)
 {
    // Constructeur par défaut
    constructor() : this("", "", "", false)
}

