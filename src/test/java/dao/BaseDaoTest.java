package dao;

import modelo.Pedido;
import modelo.Repartidor;
import modelo.TipoPedido;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base de las pruebas de los DAO. Se conecta a la base de PRUEBAS (speedfast_test, configurada en
 * db-test.properties) y deja las tres tablas vacías antes de cada test, así cada prueba parte de
 * un estado conocido y no depende del orden en que se ejecuten.
 *
 * Seguridad: como los tests borran datos, solo se ejecutan si la base conectada termina en "_test".
 * Si no hay base de pruebas configurada, los tests se OMITEN (no fallan) y el mensaje explica
 * cómo prepararla; así "mvn package" sigue compilando en equipos que no tienen esa base.
 *
 * @author Rodolfo Delgado
 */
abstract class BaseDaoTest {

    // Si se ejecuta desde IntelliJ (sin Maven) nadie definió db.config: se usa db-test.properties.
    // Con "mvn test" ya lo define el pom.xml (surefire) y este valor no se pisa.
    static {
        if (System.getProperty("db.config") == null) {
            System.setProperty("db.config", "db-test.properties");
        }
    }

    protected final PedidoDAO pedidoDAO = new PedidoDAO();
    protected final RepartidorDAO repartidorDAO = new RepartidorDAO();
    protected final EntregaDAO entregaDAO = new EntregaDAO();

    @BeforeAll
    static void verificarBaseDePruebas() {
        String problema = null;
        try (Connection conn = ConexionBD.conectar()) {
            String base = conn.getCatalog();
            if (base == null || !base.endsWith("_test")) {
                problema = "La base conectada es '" + base + "', no una base de pruebas (*_test). "
                        + "Copie db-test.properties.example como db-test.properties.";
            } else {
                // Comprueba que existan las 3 tablas
                try (Statement st = conn.createStatement()) {
                    st.executeQuery("SELECT 1 FROM repartidor LIMIT 1").close();
                    st.executeQuery("SELECT 1 FROM pedido LIMIT 1").close();
                    st.executeQuery("SELECT 1 FROM entrega LIMIT 1").close();
                }
            }
        } catch (SQLException e) {
            problema = "No se pudo usar la base de pruebas: " + ConexionBD.describirError(e)
                    + ". Ejecute bd/script_estructura_test.sql y revise db-test.properties.";
        }
        if (problema != null) {
            System.err.println("[Pruebas DAO omitidas] " + problema);
        }
        Assumptions.assumeTrue(problema == null, problema);
    }

    @BeforeEach
    void vaciarTablas() throws SQLException {
        // Orden: primero la tabla con claves foráneas (entrega), después las que ella referencia
        try (Connection conn = ConexionBD.conectar();
             Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM entrega");
            st.executeUpdate("DELETE FROM pedido");
            st.executeUpdate("DELETE FROM repartidor");
        }
    }

    /** Guarda un repartidor con el DAO y lo devuelve con su ID asignado. */
    protected Repartidor guardarRepartidor(String nombre) {
        Repartidor r = new Repartidor(nombre);
        repartidorDAO.guardar(r);
        return r;
    }

    /** Guarda un pedido PENDIENTE con el DAO y lo devuelve con su ID asignado. */
    protected Pedido guardarPedido(String direccion, TipoPedido tipo) {
        Pedido p = Pedido.crear(0, direccion, tipo);
        pedidoDAO.guardar(p);
        return p;
    }
}
