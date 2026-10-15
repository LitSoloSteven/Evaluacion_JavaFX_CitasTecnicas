# Auditoría de validaciones — 15/10/2026

Se revisaron los dos controladores, los dos DAO, los modelos, `Validador`, las tres vistas FXML, el arranque, la conexión y las restricciones de `gestion_citas.sql`.

## Corrección aplicada: programación de citas

Antes, la fecha y hora solo se comprobaban contra `null`. Se podía registrar una cita para ayer o para una hora ya pasada del día actual.

Ahora, el formulario y el DAO usan la misma validación:

- Crear una cita o cambiar su horario/cliente exige una fecha y hora no anteriores al minuto actual del equipo.
- Se permite el minuto actual porque la interfaz captura `HH:mm`, sin segundos.
- Una cita histórica puede conservar cliente, fecha y hora para actualizar descripción, servicio o estado. Así se puede finalizar o cancelar una cita vencida.
- El DAO consulta la cita guardada para determinar si conserva el horario. Asignar un ID a una cita nueva no evita la validación de inserción.
- Las consultas y los filtros pueden seguir mostrando fechas pasadas.
- No se modificaron los registros existentes ni el script SQL.

Archivos: `Validador.java`, `CitaTecnicaController.java`, `CitaTecnicaDAO.java`.

## Hallazgos pendientes de verificación del usuario

| Prioridad | Hallazgo y reproducción | Consecuencia | Corrección propuesta |
| --- | --- | --- | --- |
| Alta | `ClienteController` limita el teléfono a 8 dígitos, pero el SQL carga `8888-1111`. Se reprodujo poniendo primero `12345678` en el campo y luego cargando `8888-1111`: el campo conserva `12345678`. | Al seleccionar otro cliente se puede mostrar y después guardar el teléfono del cliente anterior. Si el campo estaba vacío, continúa vacío. | Unificar el formato de teléfonos, normalizar los datos de ejemplo y cargar los campos sin conservar un valor anterior cuando el dato entrante no cumple el filtro. |
| Media | Java acepta `usuario@dominio.c`; el `CHECK` de PostgreSQL lo rechaza con SQLSTATE `23514`. | El formulario acepta el correo y el guardado termina en error de base de datos. | Acordar y compartir las mismas reglas de correo entre Java y SQL, incluidos dominios y longitudes. |
| Media | `ClienteDAO` no valida los datos de contacto. Se guardó `abcdefgh` como teléfono en la base aislada aunque `Validador.esTelefonoValido` lo rechaza. | Una llamada al DAO que no pase por el formulario puede persistir datos que la aplicación considera inválidos. | Añadir una validación central de clientes y aplicarla antes de insertar/actualizar; alinear también las restricciones SQL. |
| Baja | Al intentar eliminar un cliente con citas, PostgreSQL 18.3 devolvió `23001` (`RESTRICT`). `ClienteController.manejarErrorSQL` contempla `23503`, pero no `23001`. | La base protege los datos, pero podría aparecer un error técnico en lugar del aviso de eliminación no permitida si la comprobación previa no detecta la relación, por ejemplo por concurrencia. | Contemplar ambos códigos al comunicar la restricción. |

Estas correcciones de clientes quedan pendientes conforme a la solicitud de mostrar los hallazgos antes de aplicarlas.

## Reglas de negocio que requieren definición

El código permite varias citas con el mismo cliente, fecha y hora, y permite seleccionar clientes inactivos. El README no prohíbe estas operaciones. No se clasificaron como defectos confirmados ni se impusieron nuevas restricciones: antes debe definirse si se permiten citas simultáneas y qué significa un cliente inactivo.

## Pruebas ejecutadas

El proyecto no tenía pruebas. Se añadieron tres clases JUnit y se configuró Surefire 3.5.2 para ejecutar JUnit 5.

Resultado final: **63 pruebas ejecutadas, 0 fallos, 0 errores, 0 omitidas**.

| Clase | Casos | Cobertura |
| --- | ---: | --- |
| `ValidadorTest` | 53 | Horas y contactos válidos/inválidos, campos obligatorios, IDs, longitudes Unicode, estados, correos duplicados, referencias a clientes, fechas pasadas/futuras y conservación de citas históricas. Reloj fijo para los límites de fecha/hora. |
| `PersistenciaValidationTest` | 7 | CRUD de clientes y citas, búsquedas, filtros combinados, correo único sin distinguir mayúsculas, integridad referencial, actualización histórica y reproducción de discrepancias entre Java y SQL. |
| `FormularioValidationTest` | 3 | Carga de las tres vistas y sus controladores; rechazo de fecha pasada, fecha imposible y hora inválida desde el formulario; reproducción del teléfono anterior conservado por el filtro. |

**Los tests llamados `documenta...` reproducen defectos pendientes y comprueban el comportamiento actual. Que pasen no significa que esos defectos estén corregidos.**

Las pruebas de PostgreSQL se ejecutaron en una base nueva con nombre `gestion_citas_auditoria_<identificador>`, cargada con el script del proyecto y eliminada al terminar. Se usaron las credenciales locales sin escribirlas en el repositorio. No se insertaron, actualizaron ni eliminaron datos en `gestion_citas` durante esta auditoría.

También se compiló el proyecto desde cero con JDK 21. El primer intento no pudo iniciar porque `JAVA_HOME` faltaba; se resolvió usando el JDK instalado en `.jdks` solo para los procesos de pruebas.

## Repetir las pruebas

Con `JAVA_HOME` apuntando a un JDK 21, ejecutar `./mvnw.cmd test` en Windows. Por defecto se ejecutan las pruebas unitarias y se omiten las de PostgreSQL/JavaFX para evitar tocar una base real.

Para las pruebas completas:

1. Crear una base desechable en `localhost:5432` con nombre `gestion_citas_auditoria_` seguido solo de letras minúsculas o números.
2. Ejecutar `gestion_citas.sql` dentro de esa base.
3. Establecer `DB_URL`, `DB_USER` y `DB_PASSWORD` para esa base en el proceso de pruebas, además de `CITAS_TEST_DB=true`.
4. Ejecutar `./mvnw.cmd test` en una sesión con JavaFX disponible y eliminar después esa base desechable.

Los informes de Maven quedan en `target/surefire-reports`. Esta revisión cubre los casos indicados; no equivale a probar todas las combinaciones de interacción ni accesos concurrentes.
