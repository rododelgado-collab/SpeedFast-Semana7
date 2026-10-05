package dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Centraliza la conexión JDBC con MySQL usando DriverManager.
 *
 * Los tres datos de acceso (db.url, db.user y db.password) se leen del archivo
 * db.properties (raíz del proyecto), así se puede cambiar el servidor, el usuario, la base
 * o la contraseña sin tocar el código fuente. Ese archivo NO se sube a GitHub (.gitignore);
 * en el repositorio solo está la plantilla db.properties.example.
 *
 * Si el archivo no existe, o le falta alguna clave, se usan los valores por defecto:
 * jdbc:mysql://localhost:3306/speedfast_db, usuario root y sin contraseña.
 *
 * El nombre del archivo puede cambiarse con la propiedad del sistema -Ddb.config=archivo.
 * Las pruebas unitarias la usan para apuntar a db-test.properties (base speedfast_test),
 * de modo que nunca toquen los datos reales.
 *
 * @author Rodolfo Delgado
 */
public class ConexionBD {

    private static final String URL_POR_DEFECTO = "jdbc:mysql://localhost:3306/speedfast_db";
    private static final String USUARIO_POR_DEFECTO = "root";
    private static final String CLAVE_POR_DEFECTO = "";
    private static final String ARCHIVO_CONFIG_POR_DEFECTO = "db.properties";
    private static final String PROPIEDAD_ARCHIVO = "db.config";

    // Parámetros del conector para MySQL 8 en un equipo local
    private static final String PARAMETROS = "allowPublicKeyRetrieval=true&serverTimezone=America/Santiago";

    private static final Properties CONFIG = cargarConfiguracion();

    private ConexionBD() {
    }

    private static Properties cargarConfiguracion() {
        Properties p = new Properties();
        String archivo = System.getProperty(PROPIEDAD_ARCHIVO, ARCHIVO_CONFIG_POR_DEFECTO);
        try (FileInputStream in = new FileInputStream(archivo)) {
            p.load(in);
        } catch (IOException e) {
            // Sin archivo se usan los valores por defecto
        }
        return p;
    }

    /**
     * Abre una conexión nueva. Quien la pide es responsable de cerrarla
     * (los DAO lo hacen con try-with-resources).
     *
     * @throws SQLException si el driver no está, el servidor está apagado o las credenciales fallan
     */
    public static Connection conectar() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");   // carga el conector (mysql-connector-j)
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontró el driver de MySQL. Revise la dependencia "
                    + "mysql-connector-j en el pom.xml y recargue Maven.", e);
        }
        String url = CONFIG.getProperty("db.url", URL_POR_DEFECTO);
        if (!url.contains("?")) {
            url += "?" + PARAMETROS;
        }
        String usuario = CONFIG.getProperty("db.user", USUARIO_POR_DEFECTO);
        String clave = CONFIG.getProperty("db.password", CLAVE_POR_DEFECTO);
        return DriverManager.getConnection(url, usuario, clave);
    }

    /**
     * Comprueba que la base de datos responde. Usa try-catch-finally: la conexión
     * se cierra en el finally aunque algo falle.
     *
     * @throws SQLException con el error original si no se pudo conectar
     */
    public static void probarConexion() throws SQLException {
        Connection conn = null;
        try {
            conn = conectar();
            if (!conn.isValid(3)) {
                throw new SQLException("La base de datos no respondió a tiempo.");
            }
            System.out.println("[ConexionBD] Conexión exitosa a " + conn.getCatalog());
        } catch (SQLException e) {
            System.err.println("[ConexionBD] " + describirError(e));
            throw e;
        } finally {
            cerrar(conn);
        }
    }

    /** Cierra una conexión sin lanzar excepciones (para usar en bloques finally). */
    public static void cerrar(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("[ConexionBD] No se pudo cerrar la conexión: " + e.getMessage());
            }
        }
    }

    /** Traduce el error de MySQL a un mensaje entendible. */
    public static String describirError(SQLException e) {
        String detalle = " (código " + e.getErrorCode() + ": " + e.getMessage() + ")";
        if (e.getMessage() != null && e.getMessage().startsWith("No se encontró el driver")) {
            return e.getMessage();
        }
        switch (e.getErrorCode()) {
            case 1045:
                return "Usuario o contraseña de MySQL incorrectos. Revise db.properties" + detalle;
            case 1049:
                return "No existe la base speedfast_db. Ejecute bd/script_estructura.sql en MySQL" + detalle;
            case 1146:
                return "Falta una tabla. Ejecute bd/script_estructura.sql en MySQL" + detalle;
            default:
                // SQLState 08xxx = error de conexión (servidor apagado, puerto incorrecto, etc.)
                String estado = e.getSQLState();
                if (e.getErrorCode() <= 0 || (estado != null && estado.startsWith("08"))) {
                    return "No se pudo conectar con MySQL. ¿Está iniciado el servicio en localhost:3306?" + detalle;
                }
                return "Error de base de datos" + detalle;
        }
    }
}
