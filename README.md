# WIM100 – Gestión de Talentos

Aplicación móvil para Android desarrollada como proyecto académico de la asignatura Desarrollo de Aplicaciones Móviles de Duoc UC.

WIM100 permite gestionar postulaciones, evaluar candidatas mediante criterios ponderados y apoyar la toma de decisiones desde un panel de Dirección.

## 1. Funcionalidades principales

### Postulante
- Registro de datos personales y antecedentes profesionales.
- Validación de nombre, correo electrónico y teléfono.
- Selección de área de especialidad.
- Registro de años y resumen de experiencia profesional.
- Validación de enlace de LinkedIn.
- Selección de currículum en formato PDF (máximo 5 MB).
- Confirmación del registro con número de postulación.

### Evaluadora
- Visualización y filtrado de candidatas.
- Consulta de antecedentes y evaluaciones.
- Asignación de notas del 1 al 5 según los criterios definidos.
- Justificación obligatoria para notas bajas.
- Registro y edición de evaluaciones cuando corresponde.
- Cálculo automático del puntaje ponderado.

### Dirección
- Acceso protegido mediante autenticación del dispositivo.
- Visualización del ranking de candidatas.
- Filtrado y consulta de evaluaciones.
- Comparación de candidatas por criterios.
- Selección o rechazo de postulaciones.
- Consulta del historial de acciones.

## 2. Tecnologías utilizadas

- **Kotlin:** lenguaje principal.
- **Jetpack Compose:** desarrollo de interfaces.
- **Material Design 3:** componentes visuales.
- **Navigation Compose:** navegación entre pantallas.
- **MVVM:** separación de responsabilidades.
- **ViewModel y StateFlow:** gestión de estados.
- **Room (SQLite):** persistencia de datos locales.
- **Kotlin Coroutines:** operaciones asíncronas.
- **JUnit:** pruebas unitarias.
- **Gradle:** compilación y gestión de dependencias.
- **Git y GitHub:** control de versiones y colaboración.

## 3. Recursos nativos de Android

### Selección y validación de archivos PDF

La aplicación utiliza `ActivityResultContracts.OpenDocument` para seleccionar archivos desde el dispositivo.

La clase `ArchivoPostulacion.kt` permite:

- Importar archivos PDF.
- Comprobar la extensión del documento.
- Validar un tamaño máximo de 5 MB.
- Rechazar documentos vacíos.
- Comprobar la apertura del PDF utilizando `PdfRenderer`.
- Guardar una copia en el almacenamiento privado de la aplicación.

### Autenticación biométrica

El acceso al módulo Dirección utiliza `BiometricPrompt`.

Dependiendo de la versión de Android y la configuración del dispositivo, permite autenticación biométrica o mediante credenciales compatibles.

## 4. Arquitectura del proyecto

WIM100 utiliza una arquitectura basada en MVVM (Model-View-ViewModel) y el patrón Repository.

### Organización del código

- `ui/screens/`: pantallas de la aplicación.
- `ui/components/`: componentes visuales reutilizables.
- `viewmodel/`: lógica de presentación y estados.
- `model/`: modelos del dominio.
- `validation/`: reglas de validación.
- `data/local/`: persistencia local con Room.
- `data/repository/`: acceso y gestión de datos.
- `nativo/`: integración con funciones nativas de Android.

### Componente reutilizable

Se implementó `PantallaDesplazable.kt` para centralizar la configuración del desplazamiento vertical en distintas pantallas.

Este componente permite mejorar la adaptación de la interfaz a orientaciones verticales y horizontales, reduciendo código duplicado.

## 5. Instalación y ejecución

### Requisitos

- Android Studio.
- SDK de Android instalado.
- Emulador Android o dispositivo físico compatible.
- Conexión a internet para descargar dependencias durante la configuración inicial.

La aplicación tiene configurada una versión mínima de Android API 24.

### Pasos

1. Clonar el repositorio.
2. Abrir el proyecto en Android Studio.
3. Esperar la sincronización de Gradle.
4. Seleccionar un emulador o dispositivo Android.
5. Ejecutar la aplicación.

### Repositorio

https://github.com/DanielEscalanteS/DesarrolloMovil

## 6. Acceso a la aplicación

WIM100 dispone de tres roles de demostración:

**Postulante:** permite completar y enviar formularios de postulación.

**Evaluadora:** permite consultar candidatas y registrar evaluaciones.

**Dirección:** permite revisar rankings, comparar candidatas, tomar decisiones y consultar el historial.

El acceso se realiza desde la pantalla principal utilizando un nombre ficticio.

El módulo Dirección requiere autenticación mediante un método compatible configurado en el dispositivo.

## 7. Persistencia de datos

La aplicación utiliza Room con SQLite para almacenar:

- Postulaciones.
- Evaluaciones.
- Puntajes por criterio.
- Historial de acciones.

Los archivos PDF seleccionados se copian al almacenamiento privado del dispositivo.

Los datos utilizados corresponden a una demostración académica, por lo que se recomienda utilizar información ficticia.

## 8. Validaciones

El formulario de postulación incorpora validaciones para:

- Nombre obligatorio.
- Formato de correo electrónico.
- Longitud y formato de teléfono.
- Selección de área profesional.
- Años de experiencia.
- Resumen de experiencia.
- Enlace de LinkedIn.
- Archivo CV obligatorio.
- Extensión PDF.
- Tamaño máximo de 5 MB.

Los errores se presentan visualmente para orientar al usuario.

## 9. Pruebas automatizadas

El proyecto incorpora pruebas unitarias con JUnit y pruebas instrumentadas que utilizan el entorno Android.

### Ejecutar pruebas unitarias

Desde la terminal de Android Studio o Visual Studio Code, en la raíz del proyecto, ejecutar:

`.\gradlew.bat :app:testDebugUnitTest`

### Compilar la aplicación

`.\gradlew.bat :app:assembleDebug`

### Pruebas del CV

El archivo `CandidataValidatorCvTest.kt` contiene ocho pruebas unitarias que verifican:

1. Rechazo de CV sin archivo.
2. Rechazo de nombre vacío.
3. Rechazo de extensiones incorrectas.
4. Rechazo de tamaño desconocido.
5. Rechazo de archivos vacíos.
6. Rechazo de archivos mayores a 5 MB.
7. Aceptación de archivos de exactamente 5 MB.
8. Aceptación de extensiones `.PDF` en mayúsculas.

Estas pruebas verifican las reglas de nombre, extensión y tamaño.

La comprobación del contenido real del documento mediante `PdfRenderer` requiere el entorno Android.

### Resultados

- Compilación de la aplicación: exitosa.
- Pruebas unitarias del CV: exitosas.
- Ejecución general de pruebas unitarias: exitosa.
- Verificación manual de interfaces en orientación horizontal: realizada.

## 10. Diseño responsivo

Se realizaron mejoras de desplazamiento para facilitar el uso de la aplicación en diferentes orientaciones del dispositivo.

Pantallas adaptadas:

- Bandeja de candidatas.
- Panel de Dirección.
- Detalle de evaluaciones.
- Historial de acciones.
- Comparación de candidatas.
- Registro de evaluaciones.

Se utiliza el componente reutilizable `PantallaDesplazable.kt` para mantener una configuración consistente.

## 11. Control de versiones

El proyecto utiliza Git y GitHub como herramientas de trabajo colaborativo.

Se desarrollan funcionalidades y correcciones en ramas independientes para posteriormente integrarlas a la rama principal.

Entre las mejoras implementadas se encuentran:

- Mejoras visuales del formulario de postulación.
- Validación y presentación de errores del CV.
- Componentes reutilizables.
- Adaptación de pantallas a orientación horizontal.
- Pruebas automatizadas de validación.

## 12. Contexto académico

**Proyecto:** WIM100 – Gestión de Talentos

**Asignatura:** Desarrollo de Aplicaciones Móviles (DSY1105)

**Institución:** Duoc UC

**Tipo de proyecto:** Aplicación móvil Android

**Finalidad:** Desarrollo y evaluación académica.