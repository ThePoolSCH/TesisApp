package com.example.tesisapp.utils

// T es un tipo genérico (puede ser User, List<Product>, etc.)
sealed class Resource<T>(val data: T? = null, val message: String? = null) {

    // Cuando la petición es exitosa, devolvemos los datos
    class Success<T>(data: T) : Resource<T>(data)

    // Cuando hay un error, devolvemos el mensaje y opcionalmente datos (ej. caché antigua)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)

    // Cuando estamos esperando respuesta (útil para mostrar ProgressBars)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}