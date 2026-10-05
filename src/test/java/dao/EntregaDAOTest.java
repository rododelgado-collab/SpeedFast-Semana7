package dao;

import modelo.Entrega;
import modelo.Pedido;
import modelo.Repartidor;
import modelo.TipoPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de EntregaDAO (INSERT con claves foráneas y SELECT con JOIN) contra speedfast_test.
 *
 * @author Rodolfo Delgado
 */
class EntregaDAOTest extends BaseDaoTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 5);
    private static final LocalTime HORA = LocalTime.of(14, 30, 15);

    @Test
    @DisplayName("guardar registra la entrega y le asigna el ID generado por MySQL")
    void guardarAsignaId() {
        Pedido pedido = guardarPedido("Calle 1", TipoPedido.COMIDA);
        Repartidor repartidor = guardarRepartidor("Juan");
        Entrega entrega = new Entrega(0, pedido.getId(), repartidor.getId(), FECHA, HORA);

        entregaDAO.guardar(entrega);

        assertTrue(entrega.getId() > 0);
    }

    @Test
    @DisplayName("listarTodas devuelve lista vacía si no hay entregas")
    void listarTodasSinDatos() {
        assertTrue(entregaDAO.listarTodas().isEmpty());
    }

    @Test
    @DisplayName("listarTodas trae fecha, hora, dirección del pedido y nombre del repartidor (JOIN)")
    void listarTodasTraeDatosDelJoin() {
        Pedido pedido = guardarPedido("Av. Apoquindo 4500", TipoPedido.EXPRESS);
        Repartidor repartidor = guardarRepartidor("María");
        entregaDAO.guardar(new Entrega(0, pedido.getId(), repartidor.getId(), FECHA, HORA));

        List<Entrega> lista = entregaDAO.listarTodas();

        assertEquals(1, lista.size());
        Entrega e = lista.get(0);
        assertEquals(pedido.getId(), e.getIdPedido());
        assertEquals(repartidor.getId(), e.getIdRepartidor());
        assertEquals(FECHA, e.getFecha());
        assertEquals(HORA, e.getHora());
        assertEquals("Av. Apoquindo 4500", e.getDireccionPedido());
        assertEquals("María", e.getNombreRepartidor());
    }

    @Test
    @DisplayName("listarTodas muestra primero la entrega más reciente")
    void listarTodasOrdenaPorMasReciente() {
        Pedido pedido = guardarPedido("Calle 1", TipoPedido.ENCOMIENDA);
        Repartidor juan = guardarRepartidor("Juan");
        Repartidor maria = guardarRepartidor("María");
        entregaDAO.guardar(new Entrega(0, pedido.getId(), juan.getId(), FECHA, LocalTime.of(9, 0)));
        entregaDAO.guardar(new Entrega(0, pedido.getId(), maria.getId(), FECHA, LocalTime.of(10, 0)));

        List<Entrega> lista = entregaDAO.listarTodas();

        assertEquals(2, lista.size());
        assertEquals("María", lista.get(0).getNombreRepartidor());
        assertEquals("Juan", lista.get(1).getNombreRepartidor());
    }

    @Test
    @DisplayName("una entrega con un pedido que no existe viola la clave foránea y lanza DAOException")
    void pedidoInexistenteViolaClaveForanea() {
        Repartidor repartidor = guardarRepartidor("Juan");
        Entrega entrega = new Entrega(0, 9999, repartidor.getId(), FECHA, HORA);

        assertThrows(DAOException.class, () -> entregaDAO.guardar(entrega));
        assertTrue(entregaDAO.listarTodas().isEmpty());
    }

    @Test
    @DisplayName("una entrega con un repartidor que no existe viola la clave foránea y lanza DAOException")
    void repartidorInexistenteViolaClaveForanea() {
        Pedido pedido = guardarPedido("Calle 1", TipoPedido.COMIDA);
        Entrega entrega = new Entrega(0, pedido.getId(), 9999, FECHA, HORA);

        assertThrows(DAOException.class, () -> entregaDAO.guardar(entrega));
    }
}
