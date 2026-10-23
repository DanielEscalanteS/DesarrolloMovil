# WIM100

Base Android del equipo para el flujo académico de postulación y evaluación, con datos ficticios.

## Integración de ramas

La rama `integracion/base-unificada` parte de `viewmodel` (que contiene `main`) e integra `base-de-datos`, cuyo historial era independiente. El commit de integración conserva ambos historiales como padres.

- Se conserva el arranque corregido y el formulario de tres campos de `viewmodel`.
- Se incorporan entidades, DAO, repositorios y validadores de `base-de-datos`.
- Se agregan los cuatro accesos DAO que faltaban en `WimDatabase`.
- `WimApplication` y `AppContainer` proporcionan una base y repositorios compartidos mediante inicialización diferida.
- `CandidataValidator` centraliza nombre, correo y teléfono. `FormularioValidator` conserva su interfaz y delega en ese validador. Los correos de prueba usan `@ejemplo.cl` o `@example.com`.
- Se conserva la configuración Gradle de `viewmodel`, con `compileSdk` y `targetSdk` 37. El namespace queda en `com.example.wim100`; el applicationId sigue siendo `com.example.appsemestral` para conservar la identidad de instalación.

## Organización

- `model`: entidades y cálculo de puntajes.
- `data/local`: Room, conversores y DAO.
- `data/repository`: operaciones de datos.
- `validation`: reglas de campos.
- `viewmodel`: estado y coordinación del formulario.
- `di`: construcción de dependencias compartidas.
- `ui`: inicio, formulario de postulación, bandeja local, componentes y tema Material 3.
- `nativo`: importación y comprobación de PDF mediante el selector del sistema.

## Ejecutar y verificar

1. Abrir la carpeta raíz en Android Studio compatible con AGP 9.2.1.
2. Configurar JDK 21 e instalar Android SDK Platform 37 y las herramientas que solicite Gradle.
3. Sincronizar Gradle y ejecutar `./gradlew testDebugUnitTest assembleDebug` (en Windows, `gradlew.bat testDebugUnitTest assembleDebug`).
4. Ejecutar la app en emulador o dispositivo API 24 o superior. El inicio permite completar una postulación o consultar la bandeja local. Usa un correo @example.com o @ejemplo.cl, un enlace ficticio https://www.linkedin.com/in/persona-ficticia y un PDF real de prueba de hasta 5 MB.
5. Las pruebas instrumentadas requieren emulador o dispositivo: `./gradlew connectedDebugAndroidTest`.

## Estado de validación

Se revisaron conflictos, referencias de DAO y configuración del arranque. La compilación y los tests Android no pudieron ejecutarse en el entorno de integración: la descarga de Gradle falló por falta de acceso de red; tampoco hay SDK Android configurado y el Java instalado es 17. Los tests añadidos y conservados deben ejecutarse en Android Studio antes de fusionar el PR. No se afirma que esta versión haya compilado.

## Formulario y persistencia

El formulario conserva las funciones previas de nombre, correo y teléfono y añade área, años de experiencia, resumen, enlace y CV. `validarFormulario()` comprueba los datos personales; `validarPostulacion()` comprueba todos los campos y es la validación usada al enviar.

`FormularioViewModel` recibe `CandidataRepository` mediante `AppViewModelFactory`. La pantalla observa el estado con `collectAsStateWithLifecycle()` y llama a funciones del ViewModel, siguiendo los ejemplos de formularios y canciones de `amaurelia/basic-app`.

- La confirmación aparece únicamente cuando termina el guardado.
- La candidatura y su historial se insertan dentro de una transacción Room.
- El repositorio normaliza el correo y rechaza duplicados; el índice único proporciona una segunda barrera.
- El selector nativo abre un PDF; se comprueba que pueda leerse y se copia al almacenamiento privado para conservarlo después de reiniciar. No requiere permiso general de almacenamiento. La referencia es interna y no está preparada para compartir mediante otras aplicaciones.
- La bandeja observa Room y permite comprobar el registro desde otra vista de la misma instalación.
- Los accesos del inicio son una simulación académica: todavía no hay autenticación ni control de acceso por perfil. El enum incorpora Postulante como preparación del flujo.
- Los datos guardados sobreviven al cierre normal. El borrador del formulario no se persiste frente a la muerte del proceso.

## Pruebas añadidas

Se adaptaron los tests existentes a la dependencia requerida del formulario, usando DAO de prueba para la validación. Se añadieron casos para comprobar que los datos personales no bastan para enviar y que un CV demasiado grande bloquea la postulación. Las pruebas instrumentadas verifican registro con historial, duplicados y rollback si falla el historial. Todas deben ejecutarse en Android Studio: la descarga de Gradle sigue bloqueada en este entorno.

## Trabajo pendiente

- Autenticación y sesión de cada perfil.
- Detalle y visor del CV, matriz de evaluación y pantallas de resultados e historial.
- Precargar criterios de evaluación antes de registrar evaluaciones.
- Completar el segundo recurso nativo y los permisos que requiera.
- Revisar en evaluación las reglas del repositorio sobre puntajes y estados permitidos.
- Completar nombres de integrantes y enlace al tablero de planificación.

El PR #1 queda incluido por ascendencia en esta integración; no es necesario volver a fusionarlo después. No se han eliminado ni reescrito las ramas originales.
