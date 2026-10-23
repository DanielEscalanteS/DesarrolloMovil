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
- `ui`: bienvenida provisional y tema Material 3.

## Ejecutar y verificar

1. Abrir la carpeta raíz en Android Studio compatible con AGP 9.2.1.
2. Configurar JDK 21 e instalar Android SDK Platform 37 y las herramientas que solicite Gradle.
3. Sincronizar Gradle y ejecutar `./gradlew testDebugUnitTest assembleDebug` (en Windows, `gradlew.bat testDebugUnitTest assembleDebug`).
4. Ejecutar la app en emulador o dispositivo API 24 o superior. La pantalla actual muestra una bienvenida provisional.
5. Las pruebas instrumentadas requieren emulador o dispositivo: `./gradlew connectedDebugAndroidTest`.

## Estado de validación

Se revisaron conflictos, referencias de DAO y configuración del arranque. La compilación y los tests Android no pudieron ejecutarse en el entorno de integración: la descarga de Gradle falló por falta de acceso de red; tampoco hay SDK Android configurado y el Java instalado es 17. Los tests añadidos y conservados deben ejecutarse en Android Studio antes de fusionar el PR. No se afirma que esta versión haya compilado.

## Trabajo pendiente

La integración reúne el trabajo existente; todavía no implementa el flujo completo.

- Construir navegación y pantallas definitivas.
- Ampliar el ViewModel con los antecedentes de `Candidata` y conectarlo al repositorio mediante una fábrica de ViewModels.
- Incorporar el perfil Postulante y definir su sesión.
- Precargar criterios de evaluación antes de registrar evaluaciones.
- Completar permisos y recursos nativos: las dependencias presentes no equivalen a recursos implementados.
- Revisar reglas de repositorio: validación de puntajes antes de guardar, estados permitidos y atomicidad del registro de candidatura con su historial.
- Completar nombres de integrantes y enlace al tablero de planificación.

El PR #1 queda incluido por ascendencia en esta integración; no es necesario volver a fusionarlo después. No se han eliminado ni reescrito las ramas originales.
