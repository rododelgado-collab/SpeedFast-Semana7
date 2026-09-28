package dao;

import modelo.Repartidor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de la tabla repartidor.
 *
 * @author Rodolfo Delgado
 */
public class RepartidorDAO {

    /** INSERT. Deja en el repartidor el ID generado por MySQL. */
    public void guardar(Repartidor repartidor) {
        String sql = "INSERT INTO repartidor (nombre) VALUES (?)";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, repartidor.getNombre());
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    repartidor.setId(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo guardar el repartidor. " + ConexionBD.describirError(e), e);
        }
    }

    /** SELECT de todos los repartidores, recorriendo el ResultSet fila por fila. */
    public List<Repartidor> listarTodos() {
        String sql = "SELECT id, nombre FROM repartidor ORDER BY id";
        List<Repartidor> repartidores = new ArrayList<>();
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                repartidores.add(new Repartidor(rs.getInt("id"), rs.getString("nombre")));
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudieron leer los repartidores. " + ConexionBD.describirError(e), e);
        }
        return repartidores;
    }
}
