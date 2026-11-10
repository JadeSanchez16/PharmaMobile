# PharmaMobile

Aplicación académica multiplataforma de gestión farmacéutica construida con
Kotlin Multiplatform y Compose Multiplatform.

## Alcance actual

- Modelos de dominio para productos, clientes y pedidos.
- CRUD remoto de productos con validaciones locales y del backend.
- Inventario con pestañas de activos, inactivos y bajo stock (`stock <= 5`).
- Navegación adaptable para teléfono, tableta y pantallas amplias.
- Clean Architecture, MVVM y Koin compartidos entre Android e iOS.
- Catálogo real de PharmaSoft gestionado con Ktor Client mediante GET, POST,
  PUT y DELETE.

## Conectividad REST

PharmaMobile se comunica con la API REST v1 de PharmaSoft. La URL base se
proporciona desde el módulo Koin de cada plataforma:

- Android Emulator: `http://10.0.2.2:8080/api/v1/`
- iOS Simulator: `http://localhost:8080/api/v1/`

El contrato CRUD implementado utiliza estas rutas:

| Método | Ruta | Estado de implementación móvil |
| --- | --- | --- |
| GET | `/api/v1/productos` | Implementado |
| GET | `/api/v1/productos/{id}` | Implementado |
| POST | `/api/v1/productos` | Implementado |
| PUT | `/api/v1/productos/{id}` | Implementado |
| DELETE | `/api/v1/productos/{id}` | Implementado |

El listado envía `pagina=0` y `tamanio=20`. PharmaSoft admite además los
parámetros opcionales `ordenarPor` (`id`, `nombre`, `precio` o `stock`) y
`direccion` (`asc` o `desc`). Si PharmaMobile no los envía, el servidor usa
`id` y `asc`.

PharmaSoft devuelve un objeto paginado. `PaginaResponseDto<ProductoResponseDto>`
deserializa `contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas` y
`ultima`. `ProductoResponseDto` refleja `id`, `nombre`, `precio`, `stock`,
`estado`, `categoriaId`, `categoriaNombre`, `fechaCreacion` y
`fechaModificacion`.

`ProductoRequestDto` contiene `nombre`, `precio`, `stock`, `estado` y
`categoriaId`. Los mappers convierten entre DTO y dominio sin introducir Ktor ni
`kotlinx.serialization` en la capa de presentación. El repositorio enlazado por
Koin es la implementación REST; el repositorio en memoria se conserva solamente
como alternativa no inyectada.

El cliente usa `ContentNegotiation` con `ignoreUnknownKeys = true`, timeout de
solicitud de 15 segundos y `Logging` en nivel `HEADERS` para registrar la
petición, las cabeceras y el estado sin exponer cuerpos durante la depuración. Los errores 404,
timeout, deserialización y conexión se convierten en mensajes controlados antes
de llegar a la interfaz.

Los errores HTTP se traducen en un único punto a `Validacion`, `NoEncontrado`,
`Conflicto`, `Servidor`, `SinConexion` o `TiempoAgotado`. La cancelación de una
corutina siempre se relanza. El `ViewModel` mantiene por separado la fase del
listado y la operación CRUD, expone errores por campo y recarga el inventario
después de crear, actualizar o eliminar.

### Pruebas de conexión de la Actividad Autónoma 07

`ConexionRestTest` verifica de forma controlada los cinco escenarios exigidos:

1. respuesta HTTP 200 y lista deserializada;
2. respuesta 404 convertida en `ErrorApi.NoEncontrado`;
3. pérdida de conexión comprobada en el emulador con mensaje y reintento;
4. timeout temporal de 1 ms convertido en `ErrorApi.TiempoAgotado`;
5. campo JSON desconocido aceptado con `ignoreUnknownKeys = true` y rechazado
   con una causa `SerializationException` cuando se desactiva.

La configuración normal permanece en 15 000 ms y
`ignoreUnknownKeys = true`; los valores alternativos solo se inyectan en
`MockEngine` durante la prueba. La suite se ejecuta con:

```powershell
.\gradlew.bat :shared:testAndroidHostTest --tests "*ConexionRestTest"
```

## Manejo de errores

La capa de datos convierte los fallos técnicos en `ErrorApi` antes de que
lleguen al `ViewModel`:

| Situación | Error de dominio | Presentación |
| --- | --- | --- |
| HTTP 400 | `Validacion` | Mensajes debajo de `nombre`, `precio` o `stock` |
| HTTP 404 | `NoEncontrado` | Mensaje controlado de recurso no encontrado |
| HTTP 409 | `Conflicto` | Mensaje de la regla de negocio enviado por PharmaSoft |
| HTTP 5xx o respuesta inválida | `Servidor` | Mensaje controlado, sin mostrar excepciones técnicas |
| Backend inaccesible | `SinConexion` | Mensaje de conexión y opción de reintentar |
| Tiempo de espera agotado | `TiempoAgotado` | Mensaje para intentar nuevamente |

Los timeouts de socket (`SocketTimeoutException` de Ktor, equivalente a
`java.net.SocketTimeoutException` en Android/JVM) también se traducen como
`TiempoAgotado`, antes de capturar `IOException`. Los demás fallos de conexión
conservan `SinConexion`. Esta distinción se verificó en el emulador durante una
espera controlada de PharmaSoft, sin modificar el timeout normal ni los datos.

PharmaSoft realiza borrado lógico. Por ese motivo, un segundo `DELETE` sobre el
mismo producto devuelve actualmente HTTP 409 y se representa como
`ErrorApi.Conflicto`; no se documenta como 404 porque ese no es el comportamiento
real del backend. `CancellationException` no se convierte en un error visible:
se relanza para respetar la cancelación estructurada cuando termina el alcance
de la corutina.

Al salir de Inventario, `DisposableEffect` cancela las solicitudes pendientes
mediante el ViewModel compartido. La cancelación no se transforma en un mensaje
de error ni permite una actualización tardía. Al volver se consulta el servidor:
cancelar el cliente no revierte una mutación que el backend ya recibió.
La validación local conserva los controles de obligatoriedad y formato numérico. La
regla de rango del precio se valida en PharmaSoft: un valor cero o negativo se envía,
el HTTP 400 se traduce a `ErrorApi.Validacion` y el mensaje remoto se muestra debajo
del campo `precio`.

La Actividad Autónoma 08 reutiliza las transiciones de
`ProductoViewModelFlujoTest` en `commonTest` y añade la cancelación al salir.
`PharmaSoftErroresRealesTest`, en `androidHostTest`, es integración adicional
contra PharmaSoft y no sustituye el requisito de pruebas comunes. Se activa con
`$env:PHARMASOFT_PRUEBAS_REALES='true'` antes de Gradle. Desde S10 también requiere
`PHARMASOFT_USUARIO` y `PHARMASOFT_CLAVE` en el entorno del proceso, con una cuenta
de desarrollo autorizada; nunca se escriben sus valores en el repositorio.
`PHARMASOFT_PRODUCTO_DUPLICADO_ID` debe identificar un producto temporal cuyo
nombre empiece por `S08_QA_` o `S10_QA_` (por compatibilidad, el valor por defecto
es 41). El fixture debe existir antes de la prueba; la suite no elimina datos.
Retirar las variables al terminar. Comprueba precio 400, PUT inexistente 404, duplicado 409 y timeout de
1 ms en un cliente aislado, sin cambiar el timeout normal de 15 segundos.

## Seguridad de red local

- Android declara `INTERNET` y usa `network_security_config.xml` para autorizar
  HTTP únicamente hacia `10.0.2.2` y `localhost`.
- iOS declara `NSAllowsLocalNetworking` para el entorno local.
- La excepción de tráfico en claro es solo para desarrollo; producción debe usar
  HTTPS.

## Autenticación

PharmaMobile consume la autenticación JWT real de PharmaSoft mediante
`POST /api/v1/auth/login`. La aplicación envía únicamente `username` y
`password`; la contraseña no se persiste. La respuesta se convierte en una
sesión de dominio con el usuario, nombre completo, roles y permisos.

El JWT se conserva cifrado en cada plataforma:

- Android usa una clave AES de Android Keystore y guarda en
  `SharedPreferences` solamente el texto cifrado y su vector de inicialización.
- iOS usa Keychain Services (`SecItemAdd`, `SecItemCopyMatching` y
  `SecItemDelete`). En Windows se verificó la compilación Kotlin/Native de la
  biblioteca (`compileKotlinIosSimulatorArm64`), no el enlace ni la ejecución de
  `iosApp`. El almacenamiento y la aplicación iOS siguen pendientes de macOS/Xcode.

Ktor instala `Auth` con esquema Bearer. `loadTokens` recupera el JWT desde
`AlmacenSeguro` y `sendWithoutRequest` excluye la ruta pública `/auth/login`.
Las solicitudes protegidas reciben automáticamente `Authorization: Bearer
<token>`. El registro de depuración usa nivel `HEADERS` y sanea esta cabecera;
nunca imprime el token ni los cuerpos de login.

Al iniciar la aplicación, un token persistido se valida con
`GET /api/v1/auth/me`. Un HTTP 401 en una ruta protegida borra la sesión y
regresa al acceso; un HTTP 403 conserva la sesión y se presenta como falta de
permiso. `GestorSesionHttp.sinPermiso` notifica el rechazo de cualquier ruta
protegida y `App` presenta el diálogo «Acceso no permitido» sin quitar la sesión.
Descartar el aviso no borra el token; iniciar o invalidar una sesión limpia el
aviso anterior. Cerrar sesión elimina el JWT local y limpia la caché Bearer.

La interfaz aplica los permisos devueltos por PharmaSoft:

| Rol | Acceso visible en PharmaMobile |
| --- | --- |
| `ADMIN` | Catálogo, clientes, ventas, reportes y administración según sus permisos |
| `VENDEDOR` | Lectura de catálogo, clientes y ventas; sin escritura de catálogo |
| `ALMACENERO` | Lectura y escritura de catálogo; sin clientes, ventas ni reportes |

Los usuarios de desarrollo verificados son `admin`, `vendedor` y `almacen`.
Sus contraseñas no se documentan ni se versionan. Para ejecutar Android, iniciar
PharmaSoft en el puerto 8080, seleccionar el Pixel 9a y usar una cuenta de
desarrollo autorizada. Las verificaciones automáticas se ejecutan con:

```powershell
.\gradlew.bat :shared:allTests :androidApp:assembleDebug --rerun-tasks
```

La verificación del flujo real en iOS requiere macOS y Xcode; no se sustituye
con capturas simuladas.

### Diagnóstico real de HTTP 403 en Android

`Sesion403Instrumentation` pertenece exclusivamente a `androidTest`: se instala
en un APK de pruebas separado y no añade accesos prohibidos a la interfaz normal.
Requiere PharmaSoft local y una sesión ALMACENERO iniciada en la app debug.
Consulta `clientes` con el mismo HttpClient de Koin, verifica HTTP 403 y el aviso
visible, comprueba que el token no cambió y obtiene HTTP 200 de `auth/me` y
`productos`. No recibe contraseñas, no modifica permisos ni datos del servidor.

```powershell
.\gradlew.bat :androidApp:assembleDebug :androidApp:assembleDebugAndroidTest
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb install -r androidApp/build/outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk
# Iniciar sesión como ALMACENERO desde la app antes de continuar.
adb shell am instrument -w pe.edu.upeu.pharmamobilee.test/pe.edu.upeu.pharmamobilee.Sesion403Instrumentation
```

El resultado es un escenario instrumentado independiente, no una prueba JUnit
adicional en el total JVM. La captura se guarda dentro de los datos privados de
la app debug; no contiene el JWT. El 16/10/2026 este escenario terminó PASS.
La corrida efectiva de `:shared:testAndroidHostTest --rerun` del mismo día,
con integración habilitada y fixture temporal, registró 18 suites y 62 pruebas:
62 aprobadas, 0 fallidas, 0 errores y 0 omitidas. Sin la activación explícita,
las cuatro pruebas de integración se omiten y no deben contarse como aprobadas.

## Capacidades nativas

La sesión 09 incorpora capacidades específicas de Android e iOS sin introducir
dependencias de plataforma en la presentación compartida:

- `platform/Formato.kt` declara `expect fun formatearSoles(valor: Double)` en
  `commonMain`.
- `Formato.android.kt` implementa el `actual` con `NumberFormat` y la
  configuración regional `es-PE`.
- `Formato.ios.kt` implementa el `actual` con `NSNumberFormatter` y
  `NSLocale("es_PE")`.
- `Producto.toUi()` aplica el formato monetario en la capa de presentación. El
  modelo de dominio conserva `precio` como `Double` y los composables reciben el
  texto ya formateado.
- `domain/platform/Compartidor.kt` define el contrato común. La extensión
  `Producto.comoTextoParaCompartir()` arma una sola vez el mensaje con el patrón
  `Nombre — S/ precio · Stock: cantidad`.
- `CompartidorAndroid` usa `Intent.ACTION_SEND`, MIME `text/plain`,
  `Intent.EXTRA_TEXT` y un selector con `FLAG_ACTIVITY_NEW_TASK`.
- `CompartidorIos` usa `UIActivityViewController` y presenta la hoja desde el
  controlador raíz disponible.
- `PlatformModule.android.kt` y `PlatformModule.ios.kt` registran en Koin la
  implementación correspondiente de `Compartidor`. `ProductoViewModel` recibe
  el contrato por constructor.
- Cada tarjeta del inventario muestra el precio nativo e incluye el botón
  **Compartir**. Compose solo invoca una acción común del ViewModel; no conoce
  `Context`, `Intent` ni UIKit.

El módulo Kotlin se expone a Swift como el framework `Shared`. `iOSApp.swift`
inicializa Koin mediante `KoinIosKt.initKoinIos()` y `ContentView.swift` obtiene
la interfaz Compose con `MainViewControllerKt.MainViewController()`.

Android se compila y prueba desde Windows. La compilación y ejecución real de
`iosMain`, el formato monetario iOS y la hoja de compartir permanecen pendientes
de una máquina macOS con Xcode; no se presentan como evidencias ejecutadas.

## Código específico de plataforma

La Actividad Autónoma 09 distingue las declaraciones `expect/actual` de las
capacidades resueltas mediante interfaz e inyección. El inventario vigente es:

- **Plataforma:** `shared/src/commonMain/.../Platform.kt` declara
  `expect fun getPlatform(): Platform`. Android implementa el `actual` en
  `shared/src/androidMain/.../Platform.android.kt` con `Build.VERSION.SDK_INT`;
  iOS lo implementa en `shared/src/iosMain/.../Platform.ios.kt` con `UIDevice`.
- **Formato monetario:** `shared/src/commonMain/.../platform/Formato.kt` declara
  `expect fun formatearSoles(valor: Double): String`. Android usa
  `NumberFormat` y `Locale.forLanguageTag("es-PE")` en `Formato.android.kt`;
  iOS usa `NSNumberFormatter` y `NSLocale("es_PE")` en `Formato.ios.kt`.
- **Módulo de inyección:** `shared/src/commonMain/.../di/AppModule.kt` declara
  `expect val platformModule: Module`. `PlatformModule.android.kt` registra
  OkHttp, la URL del emulador y `CompartidorAndroid(androidContext())`;
  `PlatformModule.ios.kt` registra Darwin, la URL local y `CompartidorIos()`.
- **Información del dispositivo:**
  `shared/src/commonMain/.../platform/InfoDispositivo.kt` declara
  `expect class InfoDispositivo()` con `sistema` y `version`.
  `InfoDispositivo.android.kt` obtiene la versión mediante
  `Build.VERSION.RELEASE`; `InfoDispositivo.ios.kt` consulta
  `UIDevice.currentDevice.systemName()` y `systemVersion`. La pantalla común
  `presentation/acerca/AcercaScreen.kt` muestra ambos valores dinámicos desde la
  opción **Acerca de** de la navegación.

**Compartir producto no es una declaración expect.**
`domain/platform/Compartidor.kt` es una interfaz común. Android la implementa
con `Intent.ACTION_SEND` y un `Context`; iOS utiliza
`UIActivityViewController`. Koin selecciona la implementación desde el
`platformModule` de cada target.

Para ejecutar Android, iniciar PharmaSoft en el puerto 8080, abrir el proyecto,
seleccionar el Pixel 9a y ejecutar `androidApp`. La opción **Acerca de** debe
mostrar `Android` y la versión devuelta por el emulador. Las pruebas y el APK se
verifican con:

```powershell
$env:PHARMASOFT_PRUEBAS_REALES='true'
.\gradlew.bat :shared:allTests :androidApp:assembleDebug --rerun-tasks
```

Para iOS se requiere macOS con Xcode: iniciar PharmaSoft, abrir
`iosApp/iosApp.xcodeproj`, seleccionar un simulador y verificar Inventario,
Compartir y Acerca de. En Windows se compila `iosMain` hasta el nivel permitido
por Kotlin/Native, pero no se afirma ejecución real de iOS ni se sustituyen sus
capturas.

### Verificación de la Guía Práctica 09

En Windows, la verificación disponible compila el APK, ejecuta las pruebas del
target Android y comprueba automáticamente el contrato `expect/actual`, el texto
compartido, los registros de `Compartidor` en ambos `platformModule` y la ausencia
de importaciones `android.*` o `platform.UIKit` dentro de `presentation`:

```powershell
.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug
```

Para la verificación manual Android, iniciar PharmaSoft, ejecutar el emulador,
abrir Inventario y pulsar **Compartir** en un producto real. El selector nativo
debe mostrar un texto como `Paracetamol 500 mg — S/ 4.50 · Stock: 120`.

La verificación iOS debe realizarse en macOS: iniciar PharmaSoft en el puerto
8080, abrir `iosApp/iosApp.xcodeproj`, seleccionar un simulador, compilar y pulsar
**Compartir**. Se deben conservar capturas auténticas del listado con moneda y de
`UIActivityViewController`. En Windows solo se revisan estáticamente
`Formato.ios.kt`, `CompartidorIos.kt`, `PlatformModule.ios.kt`, `iOSApp.swift` y
`ContentView.swift`; esa revisión no sustituye la compilación ni la ejecución.

## Verificación

```powershell
.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug
```

Resultado de la verificación automática de la Guía 08: `BUILD SUCCESSFUL` para
las pruebas compartidas y la compilación del APK Android. Las pruebas cubren los
cinco métodos HTTP, el DELETE 204 sin cuerpo, el mapeo de errores 400 y 409, la
propagación de `CancellationException`, los mappers y los flujos de creación,
actualización y eliminación del `ViewModel`.

La ejecución del simulador iOS sigue pendiente porque requiere macOS y Xcode.
En macOS, iniciar PharmaSoft en el puerto 8080, abrir
`iosApp/iosApp.xcodeproj`, ejecutar un simulador y comprobar el mismo flujo.

Las evidencias están documentadas en `docs/evidencias_sesion07/README.md`.
La preparación de la Actividad Autónoma N.º 07, el catálogo completo, el
diccionario de DTO y la bitácora pendiente de ejecución están en
`docs/actividad_autonoma_sesion07/README.md`.

## Persistencia offline-first

La Guía Práctica 11 establece a SQLDelight como fuente de verdad del catálogo
de productos. `Producto.sq` define la tabla local y sus consultas, mientras que
`ProductoLocal` es una capa independiente de Ktor y de los DTO; traduce solo
entre filas SQLDelight y el modelo de dominio. Android crea `pharma.db` con
`AndroidSqliteDriver` e iOS dispone del driver `NativeSqliteDriver` mediante
`CreadorDeDriver` y Koin.

`ProductoRepositorioOffline` expone `observar(): Flow<List<Producto>>`. La
pantalla se suscribe a ese flujo y no vuelve a descargar la lista después de
crear, editar o eliminar. Al registrar sin conexión se guarda primero un ID
negativo con `pendiente = 1`, por lo que el producto continúa visible tras
reiniciar la aplicación. Al sincronizar, se suben los pendientes (`POST`) antes
de descargar el catálogo remoto paginado (`GET`), y una transacción sustituye el
ID local por el asignado por PharmaSoft sin borrar pendientes no confirmados.

La comprobación automatizada disponible en Windows incluye tres pruebas comunes
del repositorio (persistencia sin red, emisión local por `Flow` y orden
POST→GET) y una prueba SQLite en memoria para el esquema y la reconciliación de
identificadores:

```powershell
.\gradlew.bat :shared:testAndroidHostTest --tests "*ProductoRepositorioOfflineTest"
.\gradlew.bat :shared:testAndroidHostTest --tests "*ProductoLocalSqlDelightTest"
.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug
```

Para verificar manualmente el ciclo offline en el Pixel 9a, sincronizar una vez
con PharmaSoft activo, cerrar la app, desactivar la red o detener el backend,
abrir Inventario y registrar un producto temporal. Debe verse el aviso de datos
guardados y la etiqueta pendiente. Al restaurar la conexión y pulsar
**Sincronizar**, el producto recibe un ID positivo y puede corroborarse en
Swagger/Postman. La ejecución y capturas iOS requieren macOS con Xcode; en
Windows el driver iOS se conserva en el código, pero no se declara como prueba
ejecutada.
