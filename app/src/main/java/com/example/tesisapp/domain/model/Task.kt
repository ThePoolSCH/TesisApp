package com.example.tesisapp.domain.model

data class Task(
    val id: Int,
    val locationId: Int,
    val name: String,
    val description: String,
    val priority: String,
    val status: String,
    val requiresEvidence: Boolean,
    val requiresAnnotation: Boolean,
    val evidenceUri: String?,
    val annotationText: String?
)