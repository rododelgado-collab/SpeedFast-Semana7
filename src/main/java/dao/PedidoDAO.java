package dao;

import modelo.EstadoPedido;
import modelo.Pedido;
import modelo.TipoPedido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * DAO de la tabla pedido: es la única clase que conoce el SQL de los pedidos.
 * Todas las consultas usan PreparedStatement (parámetros con ?) y try-with-resources.
 *
 * @author Rodolfo Delgado
 */
public class PedidoDAO {

    /** INSERT. Deja en el pedido el ID que generó MySQL (AUTO_INCREMENT). */
    public void guardar(Pedido pedido) {
        String sql = "INSERT INTO pedido (direccion, tipo, estado) VALUES (?, ?, ?)";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, pedido.getDireccionEntrega());
            ps.setString(2, pedido.getTipo().name());
            ps.setString(3, pedido.getEstado().name());
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    pedido.setId(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo guardar el pedido. " + ConexionBD.describirError(e), e);
        }
    }

    /**
     * SELECT de todos los pedidos. El LEFT JOIN trae el repartidor de la última
     * entrega de cada pedido (null si todavía no tiene ninguna).
     */
    public List<Pedido> listarTodos() {
        String sql = "SELECT p.id, p.direccion, p.tipo, p.estado, r.nombre AS repartidor "
                + "FROM pedido p "
                + "LEFT JOIN entrega e ON e.id = (SELECT MAX(e2.id) FROM entrega e2 WHERE e2.id_pedido = p.id) "
                + "LEFT JOIN repartidor r ON r.id = e.id_repartidor "
                + "ORDER BY p.id";
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Pedido p = Pedido.crear(
                        rs.getInt("id"),
                        rs.getString("direccion"),
                        TipoPedido.valueOf(rs.getString("tipo")));
                p.restaurarDesdeBD(EstadoPedido.valueOf(rs.getString("estado")), rs.getString("repartidor"));
                pedidos.add(p);
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudieron leer los pedidos. " + ConexionBD.describirError(e), e);
        }
        return pedidos;
    }

    /** UPDATE del estado de un pedido. */
    public void actualizarEstado(int idPedido, EstadoPedido nuevoEstado) {
        String sql = "UPDATE pedido SET estado = ? WHERE id = ?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nuevoEstado.name());
            ps.setInt(2, idPedido);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DAOException("No se pudo actualizar el estado del pedido #" + idPedido + ". "
                    + ConexionBD.describirError(e), e);
        }
    }

    /**
     * Los pedidos EN_REPARTO se simulan con hilos: si la aplicación se cerró a la mitad,
     * la entrega quedó interrumpida. Al iniciar, esos pedidos vuelven a PENDIENTE.
     *
     * @return cuántos pedidos se reiniciaron
     */
    public int reiniciarEntregasInterrumpidas() {
        String sql = "UPDATE pedido SET estado = ? WHERE estado = ?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, EstadoPedido.PENDIENTE.name());
            ps.setString(2, EstadoPedido.EN_REPARTO.name());
            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new DAOException("No se pudieron reiniciar las entregas interrumpidas. "
                    + ConexionBD.describirError(e), e);
        }
    }

    /** SELECT con GROUP BY: cuántos pedidos hay en cada estado (para el resumen de la ventana principal). */
    public Map<EstadoPedido, Integer> contarPorEstado() {
        String sql = "SELECT estado, COUNT(*) AS total FROM pedido GROUP BY estado";
        Map<EstadoPedido, Integer> cuenta = new EnumMap<>(EstadoPedido.class);
        for (EstadoPedido e : EstadoPedido.values()) {
            cuenta.put(e, 0);
        }
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cuenta.put(EstadoPedido.valueOf(rs.getString("estado")), rs.getInt("total"));
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo contar los pedidos. " + ConexionBD.describirError(e), e);
        }
        return cuenta;
    }
}
