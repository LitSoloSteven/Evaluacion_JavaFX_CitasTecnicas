# Sistema de Gestión de Citas Técnicas

**Evaluación práctica sumativa — Programación de Aplicaciones de Escritorio**  
**Universidad Americana (UAM) | Ingeniería de Sistemas**

## Descripción del proyecto

Sistema de escritorio desarrollado con **JavaFX**, **PostgreSQL** y **JDBC**, orientado a la gestión de clientes y citas de mantenimiento y reparación de equipos informáticos de **TecnoAsistencia Nicaragua S.A.**

El proyecto tiene como propósito implementar una solución que permita registrar, consultar, modificar y eliminar clientes y citas técnicas, así como realizar búsquedas, aplicar filtros y validar la información ingresada. La aplicación utiliza una base de datos relacional para garantizar la persistencia de los registros y preservar la relación entre clientes y citas.

## Objetivo

Desarrollar una aplicación de escritorio funcional que integre interfaces JavaFX, acceso a datos mediante JDBC y una base de datos PostgreSQL, implementando operaciones CRUD con validaciones, manejo de errores y consistencia de los datos.

## Funcionalidades requeridas

- **Gestión de clientes:** registrar, listar, seleccionar, actualizar y eliminar clientes.
- **Gestión de citas técnicas:** registrar, listar, seleccionar, actualizar y eliminar citas vinculadas a clientes existentes.
- **Visualización:** mostrar los registros en tablas JavaFX (`TableView`) y seleccionar clientes mediante `ComboBox`.
- **Búsqueda de clientes:** buscar por coincidencias parciales de nombres, apellidos o correo electrónico, sin distinguir mayúsculas y minúsculas.
- **Filtros de citas:** filtrar por fecha y estado, con opción para restablecer la lista completa.
- **Validación de datos:** verificar campos obligatorios, formato del correo, correos duplicados, fecha, hora, cliente asociado y estados permitidos.
- **Control de eliminaciones:** solicitar confirmación y evitar la eliminación de clientes que tengan citas asociadas.
- **Mensajes al usuario:** informar operaciones exitosas, entradas inválidas y errores de base de datos.
- **Persistencia:** conservar los datos en PostgreSQL al cerrar y volver a abrir la aplicación.

Los estados admitidos para una cita son **Pendiente**, **En proceso**, **Finalizada** y **Cancelada**.

## Tecnologías utilizadas

- **Java:** lógica de la aplicación.
- **JavaFX y FXML:** interfaces gráficas y controladores.
- **PostgreSQL:** almacenamiento relacional de datos.
- **JDBC:** conexión y ejecución de consultas SQL mediante `Connection`, `Statement`, `PreparedStatement` y `ResultSet`.
- **IntelliJ IDEA:** entorno de desarrollo y ejecución.
- **Git y GitHub:** control de versiones y trabajo colaborativo.

## Integrantes y distribución de responsabilidades

| Integrante | Usuario de GitHub | Responsabilidades |
|---|---|---|
| **Carlos Rodriguez** | `@cerf007` | Diseño y creación de la base de datos y script SQL; conexión centralizada mediante `ConexionBD.java`; modelo `Cliente.java`; implementación de `ClienteDAO.java`, CRUD y búsqueda de clientes; gestión de errores JDBC. |
| **Kenneth Zamora** | `@Proexample12` | Modelo `CitaTecnica.java`; implementación de `CitaTecnicaDAO.java` y CRUD de citas; consultas `JOIN` entre clientes y citas; consultas de filtrado por estado y fecha; integridad de relaciones. |
| **Steven Cuadra** | `@LitSoloSteven` | Diseño de las vistas `ClienteView.fxml` y `CitaTecnicaView.fxml`; implementación de `ClienteController.java`; configuración de formularios, `TableView`, búsqueda visual, navegación y actualización de registros en pantalla. |
| **Kate Ramirez** | `@kateV1013` | Implementación de `CitaTecnicaController.java`; integración del CRUD de citas con la interfaz; selección de clientes y filtros visuales; implementación de `Validador.java` y `AlertUtil.java`; manejo de validaciones, confirmaciones y alertas. |

La integración y las pruebas generales de funcionamiento son responsabilidad compartida de los cuatro integrantes. Cada integrante debe conocer su módulo y el flujo general de conexión y persistencia de datos mediante JDBC.

## Organización del proyecto

Estructura prevista de los archivos principales:

```text
src/main/
├── java/
│   └── .../
│       ├── controllers/
│       │   ├── ClienteController.java
│       │   └── CitaTecnicaController.java
│       ├── dao/
│       │   ├── ClienteDAO.java
│       │   └── CitaTecnicaDAO.java
│       ├── models/
│       │   ├── Cliente.java
│       │   └── CitaTecnica.java
│       ├── utils/
│       │   ├── ConexionBD.java
│       │   └── AlertUtil.java
│       └── validators/
│           └── Validador.java
└── resources/
    ├── ClienteView.fxml
    └── CitaTecnicaView.fxml

gestion_citas.sql
README.md
```

> La estructura mostrada es referencial y debe ajustarse a las rutas y nombres definitivos del proyecto.

## Base de datos

**Nombre:** `gestion_citas`

El sistema utiliza dos tablas relacionadas:

- **`cliente`:** ID, nombres, apellidos, teléfono, correo electrónico único y estado activo.
- **`cita_tecnica`:** ID, ID del cliente, fecha, hora, tipo de servicio, descripción y estado.

La relación es **uno a muchos**: un cliente puede tener varias citas técnicas, pero cada cita pertenece a un único cliente. No se permite eliminar físicamente un cliente mientras tenga citas relacionadas.

## Requisitos para ejecutar

- JDK compatible con la versión utilizada por el proyecto.
- IntelliJ IDEA.
- PostgreSQL instalado y en ejecución.
- Dependencias de JavaFX y controlador JDBC de PostgreSQL configuradas en el proyecto.

## Instalación y ejecución

1. Clonar o descargar este repositorio y abrirlo en **IntelliJ IDEA**.
2. Crear en PostgreSQL una base de datos llamada **`gestion_citas`**.
3. Ejecutar el script **`gestion_citas.sql`** para generar las tablas y relaciones.
4. Configurar en el entorno local la dirección, el puerto, el usuario y la contraseña de PostgreSQL que utilizará `ConexionBD.java`.
5. Verificar que las dependencias de JavaFX y JDBC estén disponibles y ejecutar la clase principal de la aplicación desde IntelliJ IDEA.
6. Comprobar la conexión y realizar una operación de registro y consulta para verificar la persistencia.

**Seguridad:** no subir contraseñas reales ni archivos de configuración local que contengan credenciales al repositorio. Las credenciales deben mantenerse fuera del control de versiones.

## Pruebas de funcionamiento

Para comprobar la solución se deben realizar las siguientes pruebas:

- Registrar un cliente y verificar su aparición en la tabla.
- Intentar registrar un correo duplicado y comprobar que se rechace.
- Modificar un cliente y confirmar la persistencia del cambio.
- Crear una cita asociada a un cliente y visualizar sus datos.
- Buscar un cliente utilizando parte de su apellido.
- Filtrar citas por fecha y estado y limpiar los filtros.
- Cambiar el estado de una cita a **Finalizada**.
- Intentar eliminar un cliente con citas asociadas y verificar que la operación se impida.
- Eliminar primero la cita y posteriormente el cliente, previa confirmación.
- Reiniciar la aplicación y verificar que se mantengan los registros guardados.

## Evidencias de funcionamiento

**Pendiente:** incorporar capturas reales de las pantallas de clientes, citas técnicas, búsquedas, filtros y validaciones una vez que las funcionalidades estén implementadas y verificadas.

---

**Asignatura:** Programación de Aplicaciones de Escritorio  
**Docente:** José A. Durán García  
**Período de evaluación:** 12 al 15 de octubre de 2026
