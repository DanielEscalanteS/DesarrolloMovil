package com.example.wim100.data.remote

import com.example.wim100.model.Candidata
import com.example.wim100.model.EstadoCandidatura

fun PostulanteDto.aCandidata(cvUri: String? = null): Candidata {
    return Candidata(
        id = id,
        nombre = nombre,
        correo = correo,
        telefono = telefono,
        area = area,
        aniosExperiencia = aniosExperiencia,
        resumenExperiencia = resumenExperiencia,
        linkedinUrl = linkedinUrl,
        cvUri = cvUri,
        cvNombre = cvNombre,
        estado = EstadoCandidatura.valueOf(estado),
        fechaRegistro = fechaRegistro,
        registradaPor = registradaPor
    )
}

fun Candidata.aPostulanteRequest(): PostulanteRequest {
    return PostulanteRequest(
        nombre = nombre,
        correo = correo,
        telefono = telefono,
        area = area,
        aniosExperiencia = aniosExperiencia,
        resumenExperiencia = resumenExperiencia,
        linkedinUrl = linkedinUrl,
        cvNombre = cvNombre,
        registradaPor = registradaPor
    )
}
