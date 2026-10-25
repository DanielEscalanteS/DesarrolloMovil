# WIM100

Aplicación Android académica para postular, evaluar candidaturas y consultar resultados con datos ficticios. Desarrollada con Kotlin, Compose, Material 3 y Room.

## Integrantes y planificación

- DanielEscalanteS. Completar los nombres de los demás integrantes.
- Tablero Trello: pendiente agregar enlace del equipo.

## Acceso por rol

El acceso es de demostración: se eligen un nombre ficticio y un rol. No existen cuentas verificadas ni autenticación en servidor.

| Rol | Acceso |
|---|---|
| Postulante | Formulario y envío de antecedentes. |
| Evaluadora | Bandeja con filtros, indicador «Ya evaluaste», CV y registro de evaluaciones. Puede consultar y comparar todas las evaluaciones. El nombre ingresado identifica a la evaluadora en el bloqueo de duplicados. |
| Dirección | Requiere verificación del dispositivo; filtra resultados, consulta y compara las notas por criterio, resuelve candidaturas y accede al historial. |

Cerrar sesión limpia la navegación hasta Inicio y elimina las pantallas de la sesión. Atrás no vuelve a las pantallas de la sesión cerrada. La sesión no se conserva tras la muerte del proceso.

## Reglas de resolución

Dirección puede seleccionar o rechazar una candidatura únicamente si está en estado Evaluada y tiene las evaluaciones mínimas requeridas. El proyecto conserva `Evaluacion.MINIMO_PARA_CONSOLIDAR = 1`, heredado de la rama de Dirección. El equipo debe confirmar ese criterio de negocio antes de la entrega; los criterios de la matriz son de demostración.

La comprobación y el cambio de estado se realizan en una transacción Room junto con el historial. Una resolución no puede sobrescribirse desde el flujo actual. La evaluación valida criterios, observaciones y existencia de la candidatura antes de guardar; una candidatura resuelta no acepta nuevas evaluaciones.

La tarjeta de Dirección explica por qué una candidatura todavía no puede resolverse. Mientras no esté resuelta, otra evaluadora puede calificarla aunque ya figure como Evaluada. Una misma evaluadora no puede volver a evaluarla.

## Consulta de evaluaciones

«Ver evaluaciones» abre una pantalla común para Evaluadora y Dirección. En «Por evaluadora» muestra el nombre, la fecha, las cinco notas con sus pesos y justificaciones, la observación general y el total guardado. Se identifica «Tu evaluación» cuando corresponde. «Comparar notas» agrupa las notas y justificaciones de todas las evaluadoras por criterio y muestra sus totales. La consulta es de solo lectura y también funciona tras seleccionar o rechazar una candidatura.

La ficha se consulta con una transacción Room que relaciona candidata, evaluaciones y puntajes. No se modificó el esquema de la base ni su versión. Los filtros conservan la posición de cada candidata en el ranking completo.

## Datos de ejemplo

Desde Inicio, sin sesión abierta, pulsar «Preparar datos de ejemplo». Para retirar la prueba que ya existe en el emulador, seleccionar su nombre y revisar el CV antes de confirmar «Reemplazar y cargar ejemplos». También se pueden conservar todos los registros actuales y solo cargar los ejemplos. No se borra la base completa.

| Candidata ficticia | Datos preparados |
|---|---|
| Camila Rojas | Postulada, sin evaluaciones: permite completar la matriz desde cero. |
| Valentina Soto | Evaluada por Ana Pérez: 88/100. |
| Francisca Muñoz | Ana Pérez: 88/100; Beatriz López: 64/100. Incluye una justificación de nota baja. Promedio: 76/100. |

Cada ejemplo recibe un PDF ficticio generado en el almacenamiento privado. Para probar «Ya evaluaste», ingresar como Ana Pérez o Beatriz López. Son nombres de demostración, no cuentas con contraseña. Repetir la carga conserva las notas y resoluciones existentes y no duplica los ejemplos.

El reemplazo elimina únicamente la candidata elegida, sus notas y su historial asociado dentro de una transacción. Se registra la acción de reemplazo. Después se retira su CV si es un archivo privado de la app y ninguna otra candidatura lo utiliza. Si falla la preparación, se revierte el cambio de datos y se limpian los archivos nuevos.

## Recursos nativos

1. Selector de PDF: valida un CV de prueba de hasta 5 MB y lo copia al almacenamiento privado. No necesita permiso general de almacenamiento. FileProvider permite abrir esa copia con una aplicación externa sin exponer una URI file://.
2. Biometría: usa BiometricPrompt y consulta disponibilidad antes de abrirlo. `USE_BIOMETRIC` es un permiso normal declarado en el Manifest. Desde Android 11 se admite también la credencial del dispositivo; en versiones anteriores este flujo requiere biometría registrada. No se permite entrar a Dirección si falla o se cancela la verificación. La pantalla ofrece abrir ajustes de seguridad.

La verificación comprueba el dispositivo; no vincula una huella a un usuario real o a un rol de una organización.

## Organización

- `model`: entidades, roles y cálculo de puntajes.
- `data/local`: Room, conversores y DAO.
- `data/repository`: operaciones y reglas de datos.
- `viewmodel`: estado y acciones de pantallas.
- `validation`: validadores.
- `ui/screens`: postulación, candidatas, evaluación, Dirección, historial, consulta de evaluaciones y preparación de ejemplos. El inicio y acceso por rol continúan en WimApp.
- `ui/WimApp.kt`: navegación y restricciones por sesión.
- `nativo`: PDF y biometría.
- `AppContainer` y `AppViewModelFactory`: construcción de dependencias.

## Ejecutar

Android Studio compatible con AGP 9.2.1, JDK 21 y Android SDK 37. Abrir la carpeta del proyecto, sincronizar Gradle y ejecutar en API 24 o superior. Para biometría es recomendable un emulador API 30+ con bloqueo configurado o un dispositivo con biometría.

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
.\gradlew.bat connectedDebugAndroidTest
```

La segunda orden requiere dispositivo o emulador conectado. Compilación y tests de esta rama están pendientes de ejecución en Android Studio: el entorno de preparación no pudo descargar Gradle por restricciones de red y no tiene SDK Android configurado. Se añadieron pruebas instrumentadas para acceso por rol a la resolución, candidatura no evaluada, segunda resolución y rollback del historial.

`ConsultaEvaluacionesRoomTest` añade comprobaciones de separación de notas por evaluadora, consulta tras resolver, indicador de evaluación propia, carga repetida sin duplicados, PDF válidos, reemplazo selectivo y rollback al fallar. Estas pruebas también están pendientes de ejecución en Android Studio.

## Demostración manual

1. En Inicio, preparar los ejemplos. Si corresponde, seleccionar y reemplazar el registro anterior con su archivo.
2. Entrar como Ana Pérez con rol Evaluadora. Valentina y Francisca deben mostrar «Ya evaluaste». Abrir Francisca y comparar las notas de Ana y Beatriz. Abrir también su CV con un lector PDF instalado.
3. Entrar como Beatriz López. Valentina debe permitir una nueva evaluación; Francisca debe indicar «Ya evaluaste». Guardar una evaluación de Camila y comprobar su consulta.
4. Entrar como Dirección. Cancelar la biometría y comprobar que no se ingresa. Repetir y verificar correctamente. Revisar filtros, notas, motivos de bloqueo y ranking.
5. Confirmar Seleccionar o Rechazar, consultar las notas de la candidatura resuelta y revisar la acción en Historial. La evaluadora debe poder consultar, pero ya no calificar esa candidatura.
6. Cerrar sesión, presionar Atrás y comprobar que no se recupera Dirección. Reiniciar la app: los datos deben persistir. Repetir la carga de ejemplos sin perder avances ni duplicarlos.
7. Probar también el formulario de Postulante con datos ficticios y PDF, formulario inválido, evaluación incompleta, evaluación duplicada, ausencia de bloqueo/huella y ausencia de lector PDF.

## Tareas para Trello

| Tarea | Criterio de aceptación |
|---|---|
| Revisar resolución de candidaturas | Solo Dirección resuelve evaluadas; no se permite resolver dos veces. |
| Probar historial | Muestra acción, responsable, fecha y candidatura. |
| Probar navegación por rol | Cada perfil accede a sus pantallas y Atrás no recupera una sesión cerrada. |
| Probar biometría | Éxito, cancelación, ausencia de registro y retorno desde ajustes. |
| Ejecutar pruebas y demo | Compilación y tests con evidencia del equipo antes del PR final. |

Estas tareas están preparadas para copiar al tablero; no se ha actualizado Trello desde este proyecto.
