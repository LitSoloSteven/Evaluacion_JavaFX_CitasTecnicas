package ni.edu.uam.evaluacionpractica.utils;


import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public final class ConexionBD {

    // --- Fallbacks (solo si no hay .env ni variables de entorno) -------
    private static final String DEFAULT_URL      = "jdbc:postgresql://localhost:5432/gestion_citas";
    private static final String DEFAULT_USER     = "postgres";
    private static final String DEFAULT_PASSWORD = "admin";
    private static final String DRIVER_CLASS     = "org.postgresql.Driver";

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        // Carga perezosa del .env — ignoreIfMissing evita excepción si no existe.
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()          // si no hay .env, seguimos con env vars / defaults
                .ignoreIfMalformed()        // tolera líneas raras sin romper
                .load();

        URL      = resolver("DB_URL",      dotenv, DEFAULT_URL);
        USER     = resolver("DB_USER",     dotenv, DEFAULT_USER);
        PASSWORD = resolver("DB_PASSWORD", dotenv, DEFAULT_PASSWORD);

        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                    "No se encontró el driver JDBC de PostgreSQL. " +
                            "Verifica la dependencia org.postgresql:postgresql en el pom.xml."
            );
        }

        System.out.printf("[ConexionBD] Configurado → %s (usuario: %s)%n", URL, USER);
    }

    private static volatile Connection conexion;

    private ConexionBD() {
        // clase utilitaria: no instanciable
    }

    /**
     * Devuelve la conexión activa. Si no existe o está cerrada, la recrea.
     * <p>
     * <b>Importante:</b> no usar dentro de un {@code try-with-resources},
     * porque se cerraría el singleton compartido.
     */
    public static Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            synchronized (ConexionBD.class) {
                if (conexion == null || conexion.isClosed()) {
                    conexion = DriverManager.getConnection(URL, USER, PASSWORD);
                    conexion.setAutoCommit(true);
                }
            }
        }
        return conexion;
    }

    /** Abre una conexión independiente que el DAO debe cerrar con try-with-resources. */
    public static Connection abrirConexion() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** Cierra la conexión global. Llamar desde el {@code stop()} de la App. */
    public static void cerrar() {
        if (conexion != null) {
            try {
                if (!conexion.isClosed()) {
                    conexion.close();
                }
            } catch (SQLException e) {
                System.err.println("[ConexionBD] Error al cerrar la conexión: " + e.getMessage());
            } finally {
                conexion = null;
            }
        }
    }

    /** Prueba rápida de conectividad (útil para arrancar la app). */
    public static boolean probarConexion() {
        try {
            Connection cn = getConexion();
            return cn != null && !cn.isClosed();
        } catch (SQLException e) {
            System.err.println("[ConexionBD] Falló la prueba de conexión: " + e.getMessage());
            return false;
        }
    }

    /**
     * Resuelve un valor en este orden:
     * 1. variable de entorno del sistema,
     * 2. entrada del .env,
     * 3. valor por defecto.
     */
    private static String resolver(String key, Dotenv dotenv, String fallback) {
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromDotenv = dotenv.get(key);
        if (fromDotenv != null && !fromDotenv.isBlank()) {
            return fromDotenv;
        }
        return fallback;
    }
}
