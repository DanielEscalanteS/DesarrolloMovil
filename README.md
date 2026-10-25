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
| Evaluadora | Bandeja, CV y evaluación única por candidata. Consulta las notas y edita su propia evaluación con el lápiz mientras Dirección no haya resuelto la candidatura. El acceso sigue siendo una demostración por nombre y rol. |
| Dirección | Requiere verificación del dispositivo; consulta fichas visuales, elige de dos a tres candidatas de la misma área para comparar, ordena por total o criterio, resuelve y consulta el historial. |

Cerrar sesión limpia la navegación hasta Inicio y elimina las pantallas de la sesión. Atrás no vuelve a las pantallas de la sesión cerrada. La sesión no se conserva tras la muerte del proceso.

## Reglas de resolución

Dirección puede seleccionar o rechazar una candidatura únicamente si está en estado Evaluada y tiene una evaluación completa. Se mantiene la constante `Evaluacion.MINIMO_PARA_CONSOLIDAR = 1` y la matriz existente (Trayectoria 25%, Conocimientos 25%, Liderazgo 20%, Visión 15%, Comunicación 15%). La fórmula y la escala de 1 a 5 no cambian.

Cada candidata tiene una sola evaluación. Ni la misma evaluadora ni otra pueden crear una segunda. La responsable puede editar el registro existente, siempre que la candidatura no esté resuelta. Dirección puede consultar, comparar y resolver; no puede editar notas. Los permisos se verifican en el Repository, además de controlar las acciones visibles en las pantallas.

La edición conserva el id, la responsable y la fecha original; actualiza las notas, observaciones, total y fecha de modificación. Guarda un detalle anterior/nuevo en Historial dentro de la misma transacción. Una revisión evita guardar encima de cambios posteriores realizados desde otra pantalla. Si se cancela, no se escribe nada.

## Consulta y comparación

La ficha de evaluación muestra un indicador circular del total, tarjetas con barras por criterio, justificaciones y observación general. Las fortalezas son notas 4 o 5; el nivel adecuado es 3; los aspectos por mejorar son 1 o 2. Estas ayudas se basan exclusivamente en las notas registradas, no en inferencias sobre el CV. Se combinan etiquetas, íconos y colores.

En Dirección se marcan de dos a tres candidatas evaluadas de la misma área. «Comparar» muestra a las elegidas y sus notas por criterio, con justificaciones. La selección se conserva al ordenar, filtrar o volver desde la comparación. «Limpiar» permite elegir otro grupo. La comparación no elige automáticamente a una ganadora.

«Filtros y orden» permite seleccionar estado y área, ordenar por puntaje total o cualquiera de los cinco criterios, y cambiar entre mayor/menor. Las candidatas sin nota quedan al final. Cambiar el orden no modifica los pesos ni los puntajes.

El formulario de postulación y su selector «Área de especialidad» se mantienen sin cambios. El caso sigue siendo la adaptación académica a ciberseguridad; no se agregan puestos ni categorías del programa minero real.

## Base existente: migración 1 a 2

Room se actualiza a versión 2 con una migración explícita, sin vaciar la base. Agrega la fecha de edición, la revisión y un índice único por candidata. Si la versión anterior contiene más de una evaluación por candidata, conserva la primera registrada (menor id). Antes de retirar las adicionales, archiva en Historial su responsable, fecha original, total, observación general y notas/justificaciones por criterio. Se conservan las candidaturas, sus CV y los historiales existentes. Las candidaturas ya resueltas siguen resueltas.

## Datos de ejemplo automáticos

La aplicación prepara ocho candidatas ficticias al primer inicio de esta versión y registra que la preparación terminó. No existe botón de carga. Los siguientes inicios conservan los avances y no duplican los perfiles ni los PDF. Cada ejemplo tiene su CV ficticio.

| Candidata | Área | Evaluadora y puntaje inicial | Estado inicial |
|---|---|---|---|
| Camila Rojas | Blue Team | Sin evaluación | Postulada |
| Francisca Muñoz | Blue Team | Ana Pérez, 74/100 | Evaluada |
| Paula Herrera | Blue Team | Ana Pérez, 82/100 | Evaluada |
| Daniela Vega | Blue Team | Beatriz López, 77/100 | Evaluada |
| Catalina Torres | Blue Team | Ana Pérez, 80/100 | Seleccionada |
| Valentina Soto | Red Team | Ana Pérez, 88/100 | Evaluada |
| Antonia Silva | Red Team | Beatriz López, 79/100 | Evaluada |
| Fernanda Díaz | Red Team | Beatriz López, 47/100 | No seleccionada |

Estos valores corresponden a una instalación nueva. Si ya existen ejemplos de la versión anterior, se conservan sus evaluaciones y resoluciones; se reorganiza únicamente su área para disponer de grupos comparables. Los registros ajenos a los ejemplos no se modifican. Para probar el lápiz, ingresar como Ana Pérez o Beatriz López con rol Evaluadora. Son identidades de demostración, no cuentas verificadas.

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
- `ui/screens`: postulación, candidatas, evaluación, Dirección, historial, ficha visual y comparación entre candidatas. El inicio y acceso por rol continúan en WimApp.
- `ui/WimApp.kt`: navegación y restricciones por sesión.
- `nativo`: PDF y biometría.
- `AppContainer` y `AppViewModelFactory`: construcción de dependencias.

## Ejecutar

Android Studio compatible con AGP 9.2.1, JDK 21 y Android SDK 37. Abrir la carpeta del proyecto, sincronizar Gradle y ejecutar en API 24 o superior. Para biometría es recomendable un emulador API 30+ con bloqueo configurado o un dispositivo con biometría.

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
.\gradlew.bat connectedDebugAndroidTest
```

Validación realizada: `testDebugUnitTest`, `assembleDebug` y `assembleDebugAndroidTest` completaron con BUILD SUCCESSFUL usando JDK 21 y SDK 37.0. Pasaron 27 pruebas unitarias sin fallos. Se comprobó con SQLite que la migración conserva los datos y que su esquema coincide con las columnas, claves e índices generados por Room/KSP.

La segunda orden del bloque requiere dispositivo o emulador conectado. Las pruebas instrumentadas quedaron compiladas, pero su ejecución y la revisión visual en dispositivo están pendientes; no hubo emulador ni teléfono conectado en este entorno.

Pruebas añadidas/adaptadas: evaluación única incluso con otra evaluadora, edición conservando el registro, permisos y bloqueo tras resolver, revisiones desactualizadas, rollback de la edición si falla el historial, ejemplos idempotentes con PDF, migración de una base v1 con duplicados, comparación por área y ordenamiento por criterio. Los tests anteriores de postulación se conservan.

## Demostración manual

1. Abrir la app: los ejemplos se preparan automáticamente. En una base anterior, comprobar que siguen las candidaturas y que las evaluaciones adicionales quedaron archivadas en Historial.
2. Entrar como Ana Pérez, rol Evaluadora. Abrir Francisca o Paula y tocar el lápiz. Comprobar notas precargadas, cancelar sin guardar y volver a abrir. Luego editar, guardar y revisar el nuevo total e historial.
3. Entrar como Beatriz López. Las evaluaciones de Ana son de consulta, sin lápiz ni opción de crear otra. Daniela sí permite edición. Evaluar a Camila desde cero.
4. Entrar como Dirección tras verificar el dispositivo. Ordenar por total y después por Liderazgo: la lista debe cambiar sin alterar las notas. Probar ambos sentidos y los filtros.
5. Marcar Francisca y Paula, pulsar Comparar y revisar sus fortalezas y notas. Volver, añadir Daniela y comparar tres. Con esa selección, intentar marcar una candidata Red Team: se debe explicar que las áreas son distintas.
6. Limpiar la selección y comparar Valentina con Antonia. Seleccionar o rechazar una candidata no resuelta. Sus notas siguen consultables, pero el lápiz deja de estar disponible para su responsable.
7. Reiniciar la app y comprobar que persisten las ediciones, resoluciones y cantidad de ejemplos. Probar también el formulario de postulación, PDF, validaciones, cierre de sesión y cancelación de biometría.

## Tareas para Trello

| Tarea | Criterio de aceptación |
|---|---|
| Revisar resolución de candidaturas | Solo Dirección resuelve evaluadas; no se permite resolver dos veces. |
| Probar edición y comparación | Una evaluación por candidata; solo su responsable edita antes de resolver; comparación de dos o tres de la misma área. |
| Probar historial | Muestra acción, responsable, fecha y candidatura. |
| Probar navegación por rol | Cada perfil accede a sus pantallas y Atrás no recupera una sesión cerrada. |
| Probar biometría | Éxito, cancelación, ausencia de registro y retorno desde ajustes. |
| Ejecutar pruebas y demo | Compilación y tests con evidencia del equipo antes del PR final. |

Estas tareas están preparadas para copiar al tablero; no se ha actualizado Trello desde este proyecto.
