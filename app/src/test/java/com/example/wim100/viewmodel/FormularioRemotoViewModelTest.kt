package com.example.wim100.viewmodel

import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.RandomUserRepository
import com.example.wim100.data.repository.HistorialRepository
import com.example.wim100.model.*
import io.mockk.*
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

internal fun candidataRemotaTest(id: Long = 10) = Candidata(
    id = id, nombre = "Ana Demo", correo = "ana@example.com", telefono = "+56 9 1111 2222",
    area = AREAS_ESPECIALIDAD.first(), aniosExperiencia = 5,
    resumenExperiencia = "Experiencia ficticia suficientemente larga para validar el formulario.",
    linkedinUrl = "https://www.linkedin.com/in/ana-demo", cvUri = "file:///cv.pdf",
    cvNombre = "cv.pdf", registradaPor = "Evaluadora Original", fechaRegistro = 123L)
