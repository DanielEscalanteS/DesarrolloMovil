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
| Evaluadora | Bandeja, apertura de CV y registro de evaluaciones. El nombre ingresado identifica a la evaluadora en el bloqueo de duplicados. |
| Dirección | Requiere verificación del dispositivo; consulta resultados, resuelve candidaturas y accede al historial. |

Cerrar sesión limpia la navegación hasta Inicio y elimina las pantallas de la sesión. Atrás no vuelve a las pantallas de la sesión cerrada. La sesión no se conserva tras la muerte del proceso.

## Reglas de resolución

Dirección puede seleccionar o rechazar una candidatura únicamente si está en estado Evaluada y tiene las evaluaciones mínimas requeridas. El proyecto conserva `Evaluacion.MINIMO_PARA_CONSOLIDAR = 1`, heredado de la rama de Dirección. El equipo debe confirmar ese criterio de negocio antes de la entrega; los criterios de la matriz son de demostración.

La comprobación y el cambio de estado se realizan en una transacción Room junto con el historial. Una resolución no puede sobrescribirse desde el flujo actual. La evaluación valida criterios, observaciones y existencia de la candidatura antes de guardar; una candidatura resuelta no acepta nuevas evaluaciones.

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
- `ui/screens`: postulación, candidatas, evaluación, Dirección e historial. El inicio y acceso por rol continúan en WimApp.
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

## Demostración manual

1. Entrar como Postulante con un nombre de prueba, completar antecedentes y adjuntar un PDF real de prueba. Usar @example.com o @ejemplo.cl. Enviar y cerrar sesión.
2. Entrar como Evaluadora con nombre propio ficticio. Abrir el CV con un lector PDF instalado, asignar todos los puntajes y justificar los de 1 o 2. Guardar.
3. Entrar como Dirección. Cancelar la biometría y comprobar que no se ingresa. Repetir y verificar correctamente.
4. Consultar resultados, confirmar Seleccionar o Rechazar y revisar la acción, responsable y fecha en Historial.
5. Cerrar sesión, presionar Atrás y comprobar que no se recupera Dirección. Reiniciar la app y verificar que los datos persisten, pero la sesión vuelve a solicitarse.
6. Probar formulario inválido, evaluación incompleta, evaluación duplicada, ausencia de bloqueo/huella y ausencia de lector PDF.

## Tareas para Trello

| Tarea | Criterio de aceptación |
|---|---|
| Revisar resolución de candidaturas | Solo Dirección resuelve evaluadas; no se permite resolver dos veces. |
| Probar historial | Muestra acción, responsable, fecha y candidatura. |
| Probar navegación por rol | Cada perfil accede a sus pantallas y Atrás no recupera una sesión cerrada. |
| Probar biometría | Éxito, cancelación, ausencia de registro y retorno desde ajustes. |
| Ejecutar pruebas y demo | Compilación y tests con evidencia del equipo antes del PR final. |

Estas tareas están preparadas para copiar al tablero; no se ha actualizado Trello desde este proyecto.
