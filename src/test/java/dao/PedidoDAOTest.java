package dao;

import modelo.Entrega;
import modelo.EstadoPedido;
import modelo.Pedido;
import modelo.Repartidor;
import modelo.TipoPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de PedidoDAO (INSERT, SELECT con LEFT JOIN, UPDATE y GROUP BY) contra speedfast_test.
 *
 * @author Rodolfo Delgado
 */
class PedidoDAOTest extends BaseDaoTest {

    @Test
    @DisplayName("guardar deja en el pedido el ID generado por MySQL")
    void guardarAsignaId() {
        Pedido p = Pedido.crear(0, "Av. Providencia 123", TipoPedido.COMIDA);

        pedidoDAO.guardar(p);

        assertTrue(p.getId() > 0);
    }

    @Test
    @DisplayName("listarTodos devuelve lista vacía si no hay pedidos")
    void listarTodosSinDatos() {
        assertTrue(pedidoDAO.listarTodos().isEmpty());
    }

    @Test
    @DisplayName("listarTodos reconstruye la subclase correcta, la dirección y el estado de cada pedido")
    void guardarYListarConservaDatos() {
        guardarPedido("Calle 1", TipoPedido.COMIDA);
        guardarPedido("Calle 2", TipoPedido.ENCOMIENDA);
        guardarPedido("Calle 3", TipoPedido.EXPRESS);

        List<Pedido> lista = pedidoDAO.listarTodos();

        assertEquals(3, lista.size());
        assertEquals("Calle 1", lista.get(0).getDireccionEntrega());
        assertEquals(TipoPedido.COMIDA, lista.get(0).getTipo());
        assertEquals(TipoPedido.ENCOMIENDA, lista.get(1).getTipo());
        assertEquals(TipoPedido.EXPRESS, lista.get(2).getTipo());
        for (Pedido p : lista) {
            assertEquals(EstadoPedido.PENDIENTE, p.getEstado());
            assertNull(p.getRepartidorAsignado(), "Sin entregas no hay repartidor");
        }
    }

    @Test
    @DisplayName("actualizarEstado cambia el estado del pedido indicado y de ningún otro")
    void actualizarEstado() {
        Pedido a = guardarPedido("Calle A", TipoPedido.COMIDA);
        Pedido b = guardarPedido("Calle B", TipoPedido.EXPRESS);

        pedidoDAO.actualizarEstado(a.getId(), EstadoPedido.ENTREGADO);

        List<Pedido> lista = pedidoDAO.listarTodos();
        assertEquals(EstadoPedido.ENTREGADO, buscar(lista, a.getId()).getEstado());
        assertEquals(EstadoPedido.PENDIENTE, buscar(lista, b.getId()).getEstado());
    }

    @Test
    @DisplayName("listarTodos trae el repartidor de la ÚLTIMA entrega del pedido")
    void listarTodosTraeRepartidorDeUltimaEntrega() {
        Pedido pedido = guardarPedido("Calle X", TipoPedido.ENCOMIENDA);
        Repartidor juan = guardarRepartidor("Juan");
        Repartidor maria = guardarRepartidor("María");
        // Primer intento con Juan (interrumpido) y segundo intento con María
        entregaDAO.guardar(new Entrega(0, pedido.getId(), juan.getId(), LocalDate.of(2026, 10, 1), LocalTime.of(9, 0)));
        entregaDAO.guardar(new Entrega(0, pedido.getId(), maria.getId(), LocalDate.of(2026, 10, 1), LocalTime.of(10, 0)));

        Pedido leido = pedidoDAO.listarTodos().get(0);

        assertEquals("María", leido.getRepartidorAsignado());
    }

    @Test
    @DisplayName("reiniciarEntregasInterrumpidas devuelve a PENDIENTE solo los EN_REPARTO")
    void reiniciarEntregasInterrumpidas() {
        Pedido enReparto1 = guardarPedido("Calle 1", TipoPedido.COMIDA);
        Pedido enReparto2 = guardarPedido("Calle 2", TipoPedido.EXPRESS);
        Pedido entregado = guardarPedido("Calle 3", TipoPedido.ENCOMIENDA);
        pedidoDAO.actualizarEstado(enReparto1.getId(), EstadoPedido.EN_REPARTO);
        pedidoDAO.actualizarEstado(enReparto2.getId(), EstadoPedido.EN_REPARTO);
        pedidoDAO.actualizarEstado(entregado.getId(), EstadoPedido.ENTREGADO);

        int reiniciados = pedidoDAO.reiniciarEntregasInterrumpidas();

        assertEquals(2, reiniciados);
        List<Pedido> lista = pedidoDAO.listarTodos();
        assertEquals(EstadoPedido.PENDIENTE, buscar(lista, enReparto1.getId()).getEstado());
        assertEquals(EstadoPedido.PENDIENTE, buscar(lista, enReparto2.getId()).getEstado());
        assertEquals(EstadoPedido.ENTREGADO, buscar(lista, entregado.getId()).getEstado());
    }

    @Test
    @DisplayName("contarPorEstado cuenta cada estado y pone 0 en los que no tienen pedidos")
    void contarPorEstado() {
        Pedido p1 = guardarPedido("Calle 1", TipoPedido.COMIDA);
        guardarPedido("Calle 2", TipoPedido.COMIDA);
        Pedido p3 = guardarPedido("Calle 3", TipoPedido.EXPRESS);
        pedidoDAO.actualizarEstado(p1.getId(), EstadoPedido.ENTREGADO);
        pedidoDAO.actualizarEstado(p3.getId(), EstadoPedido.ENTREGADO);

        Map<EstadoPedido, Integer> cuenta = pedidoDAO.contarPorEstado();

        assertEquals(1, cuenta.get(EstadoPedido.PENDIENTE));
        assertEquals(0, cuenta.get(EstadoPedido.EN_REPARTO));
        assertEquals(2, cuenta.get(EstadoPedido.ENTREGADO));
    }

    @Test
    @DisplayName("una dirección con SQL malicioso se guarda como texto (PreparedStatement evita la inyección)")
    void direccionConSqlMalicioso() {
        String maliciosa = "x'); DROP TABLE pedido; --";

        guardarPedido(maliciosa, TipoPedido.COMIDA);

        List<Pedido> lista = pedidoDAO.listarTodos();   // si la tabla se hubiera borrado, esto fallaría
        assertEquals(1, lista.size());
        assertEquals(maliciosa, lista.get(0).getDireccionEntrega());
    }

    private static Pedido buscar(List<Pedido> pedidos, int id) {
        return pedidos.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró el pedido #" + id));
    }
}
