package com.example.tesisapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks_table",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE // Si se borra una ubicación, se borran sus tareas
        )
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val locationId: Int, // Clave foránea que la vincula a una ubicación
    val name: String,
    val description: String,
    val priority: String, // "Alta", "Media", "Baja"
    val status: String, // "Pendiente", "Completada", "Incidente"

    // Campos para gestionar la evidencia y anotaciones
    val requiresEvidence: Boolean,
    val requiresAnnotation: Boolean,

    // Campos para guardar los resultados (inicialmente nulos)
    val evidenceUri: String? = null, // Guardará la URI de la foto/archivo
    val annotationText: String? = null // Guardará la nota del usuario
)