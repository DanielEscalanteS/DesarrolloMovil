# WIM100

Aplicación académica Android para registrar postulaciones y revisar antecedentes profesionales mediante datos ficticios.

## Integrantes

- DanielEscalanteS
- Pendiente completar los demás integrantes.

Tablero de planificación: pendiente agregar enlace.

## Funcionalidades implementadas

- Formulario de postulación con nombre, correo, teléfono, área, experiencia y enlace de perfil ficticio.
- Validaciones por campo y mensajes de error.
- Selección de un CV en PDF de hasta 5 MB y copia al almacenamiento privado del dispositivo.
- Registro de la postulación y su historial en una transacción Room.
- Control de correos duplicados.
- Bandeja local de postulaciones recibidas.
- Navegación entre inicio, formulario y bandeja.

Las vistas comparten la base del mismo dispositivo. Todavía no hay autenticación ni sincronización entre dispositivos. El borrador del formulario no se conserva después de la muerte del proceso.

## Organización

- `model`: entidades, estados y cálculo de puntajes.
- `data/local`: base Room, conversores y DAO.
- `data/repository`: operaciones de datos.
- `viewmodel`: estado y acciones de las pantallas.
- `validation`: validaciones de campos y antecedentes.
- `ui`: pantallas, componentes y tema Material 3.
- `di`: construcción de dependencias.
- `nativo`: importación y comprobación del PDF seleccionado.

## Ejecutar

1. Abrir la carpeta del proyecto en Android Studio compatible con AGP 9.2.1.
2. Configurar JDK 21 e instalar Android SDK Platform 37 y las herramientas solicitadas por Gradle.
3. Sincronizar Gradle y ejecutar en un emulador o dispositivo API 24 o superior.

En Windows:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

En Linux o macOS:

```bash
bash gradlew testDebugUnitTest assembleDebug
```

Las pruebas instrumentadas requieren un emulador o dispositivo conectado:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

La compilación y la ejecución de las pruebas de esta versión están pendientes de verificación en Android Studio.

## Datos de prueba

Usar correos ficticios con `@example.com` o `@ejemplo.cl`, un enlace como `https://www.linkedin.com/in/persona-ficticia` y un PDF real con contenido de prueba.

Recorrido: Quiero postular → completar antecedentes → adjuntar PDF → enviar → aceptar confirmación → Ver postulaciones. Cerrar y reabrir para comprobar persistencia y repetir un correo para comprobar el bloqueo de duplicados.

## Pendientes

- Completar los datos de integrantes y planificación.
- Autenticación y sesión por perfil.
- Pantalla de detalle y visor de CV.
- Matriz de evaluación y pantallas de resultados e historial.
- Precarga de criterios de evaluación.
- Segundo recurso nativo.
