package main;

import dao.ConexionBD;
import vista.VentanaPrincipal;

import javax.swing.*;
import java.sql.SQLException;

/**
 * Punto de entrada de SpeedFast - Semana 7 (Swing + JDBC + MySQL).
 *
 * @author Rodolfo Delgado
 */
public class Main {

    public static void main(String[] args) {
        // Aspecto nativo del sistema operativo; si falla se usa el de Java.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("No se pudo aplicar el Look and Feel del sistema: " + e.getMessage());
        }

        // Se comprueba la conexión con MySQL antes de abrir la interfaz.
        try {
            ConexionBD.probarConexion();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    ConexionBD.describirError(e)
                            + "\n\nLa aplicación se abrirá igualmente; cuando MySQL esté disponible"
                            + "\nvuelva a intentar la operación.",
                    "SpeedFast - Sin conexión a la base de datos", JOptionPane.ERROR_MESSAGE);
        }

        // La interfaz se crea en el Event Dispatch Thread de Swing.
        SwingUtilities.invokeLater(() -> new VentanaPrincipal());
    }
}
