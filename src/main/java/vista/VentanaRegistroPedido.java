package vista;

import controlador.ControladorPedidos;
import dao.DAOException;
import modelo.Pedido;
import modelo.TipoPedido;

import javax.swing.*;
import java.awt.*;

/**
 * VISTA: formulario para registrar un pedido nuevo en la base de datos.
 * Campos: Dirección y Tipo (JComboBox). El ID lo asigna MySQL (AUTO_INCREMENT).
 * El botón Guardar valida, guarda el pedido a través del controlador (PedidoDAO)
 * y confirma con JOptionPane.
 *
 * @author Rodolfo Delgado
 */
public class VentanaRegistroPedido extends JFrame {

    private final ControladorPedidos controlador;

    private final JTextField txtDireccion = new JTextField(24);
    private final JComboBox<TipoPedido> cmbTipo = new JComboBox<>(TipoPedido.values());

    public VentanaRegistroPedido(ControladorPedidos controlador) {
        this.controlador = controlador;

        setTitle("SpeedFast - Registrar pedido");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);   // solo cierra esta ventana
        setResizable(false);

        crearComponentes();
        pack();
        setLocationRelativeTo(null);
    }

    private void crearComponentes() {
        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.setBorder(Estilo.margen(12));
        raiz.add(Estilo.encabezado("Registrar pedido", "Se guarda en MySQL; el ID lo asigna la base de datos"),
                BorderLayout.NORTH);

        // Etiquetas alineadas a la derecha y campos a la izquierda
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.EAST;

        agregarFila(form, c, 0, "Dirección de entrega:", txtDireccion);
        agregarFila(form, c, 1, "Tipo de pedido:", cmbTipo);
        raiz.add(form, BorderLayout.CENTER);

        JButton btnGuardar = Estilo.boton("Guardar");
        JButton btnLimpiar = Estilo.boton("Limpiar");
        JButton btnCerrar = Estilo.boton("Cerrar");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnCerrar);
        botones.add(btnLimpiar);
        botones.add(btnGuardar);
        raiz.add(botones, BorderLayout.SOUTH);

        setContentPane(raiz);
        getRootPane().setDefaultButton(btnGuardar);   // Enter = Guardar

        btnGuardar.addActionListener(e -> guardarPedido());
        btnLimpiar.addActionListener(e -> limpiarCampos());
        btnCerrar.addActionListener(e -> dispose());
    }

    private void agregarFila(JPanel form, GridBagConstraints c, int fila, String etiqueta, JComponent campo) {
        c.gridy = fila;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        form.add(new JLabel(etiqueta), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        form.add(campo, c);
    }

    /** Valida (en el controlador), guarda y confirma. */
    private void guardarPedido() {
        try {
            Pedido pedido = controlador.registrarPedido(
                    txtDireccion.getText(),
                    (TipoPedido) cmbTipo.getSelectedItem());

            JOptionPane.showMessageDialog(this,
                    "Pedido #" + pedido.getId() + " guardado en la base de datos.\n"
                            + "Tipo: " + pedido.getTipo()
                            + "  |  Tiempo estimado: " + pedido.calcularTiempoEntrega() + " min",
                    "Pedido registrado", JOptionPane.INFORMATION_MESSAGE);
            limpiarCampos();

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Datos inválidos", JOptionPane.WARNING_MESSAGE);
            txtDireccion.requestFocus();
            txtDireccion.selectAll();

        } catch (DAOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarCampos() {
        txtDireccion.setText("");
        cmbTipo.setSelectedIndex(0);
        txtDireccion.requestFocus();
    }
}
