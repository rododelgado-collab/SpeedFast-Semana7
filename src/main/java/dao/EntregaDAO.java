package dao;

import modelo.Entrega;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de la tabla entrega: registra la relación entre un pedido y un repartidor.
 *
 * @author Rodolfo Delgado
 */
public class EntregaDAO {

    /** INSERT de la entrega (id_pedido y id_repartidor son claves foráneas). */
    public void guardar(Entrega entrega) {
        String sql = "INSERT INTO entrega (id_pedido, id_repartidor, fecha, hora) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, entrega.getIdPedido());
            ps.setInt(2, entrega.getIdRepartidor());
            ps.setDate(3, Date.valueOf(entrega.getFecha()));
            ps.setTime(4, Time.valueOf(entrega.getHora()));
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    entrega.setId(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo registrar la entrega. " + ConexionBD.describirError(e), e);
        }
    }

    /** SELECT con JOIN a pedido y repartidor, para mostrar el historial en un JTable. */
    public List<Entrega> listarTodas() {
        String sql = "SELECT e.id, e.id_pedido, e.id_repartidor, e.fecha, e.hora, "
                + "p.direccion, r.nombre "
                + "FROM entrega e "
                + "JOIN pedido p ON p.id = e.id_pedido "
                + "JOIN repartidor r ON r.id = e.id_repartidor "
                + "ORDER BY e.id DESC";
        List<Entrega> entregas = new ArrayList<>();
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Entrega e = new Entrega(
                        rs.getInt("id"),
                        rs.getInt("id_pedido"),
                        rs.getInt("id_repartidor"),
                        rs.getDate("fecha").toLocalDate(),
                        rs.getTime("hora").toLocalTime());
                e.setDatosVisibles(rs.getString("direccion"), rs.getString("nombre"));
                entregas.add(e);
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo leer el historial de entregas. " + ConexionBD.describirError(e), e);
        }
        return entregas;
    }
}
