package vista;

import controlador.ControladorPedidos;
import dao.DAOException;
import modelo.Pedido;
import modelo.Repartidor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * VISTA: asignar un repartidor disponible a un pedido pendiente e iniciar la
 * entrega. Los pedidos pendientes se leen de MySQL y al iniciar se registra la
 * entrega en la tabla entrega. La entrega se simula en un hilo (tareas.TareaEntrega), así la
 * interfaz sigue respondiendo mientras el repartidor "va en camino".
 *
 * @author Rodolfo Delgado
 */
public class VentanaAsignarRepartidor extends JFrame {

    private final ControladorPedidos controlador;
    private final Runnable observador = this::cargarCombos;

    private final JComboBox<Pedido> cmbPedidos = new JComboBox<>();
    private final JComboBox<Repartidor> cmbRepartidores = new JComboBox<>();
    private final JButton btnIniciar = Estilo.boton("Iniciar entrega");
    private final JLabel lblEstado = new JLabel(" ");

    public VentanaAsignarRepartidor(ControladorPedidos controlador) {
        this.controlador = controlador;

        setTitle("SpeedFast - Asignar repartidor");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);

        crearComponentes();
        cargarCombos();
        pack();
        setLocationRelativeTo(null);

        controlador.agregarObservador(observador);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                controlador.quitarObservador(observador);
            }
        });
    }

    private void crearComponentes() {
        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.setBorder(Estilo.margen(12));
        raiz.add(Estilo.encabezado("Asignar repartidor",
                "Solo se muestran pedidos pendientes y repartidores libres"), BorderLayout.NORTH);

        cmbPedidos.setPreferredSize(new Dimension(320, cmbPedidos.getPreferredSize().height));

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 10));
        form.add(new JLabel("Pedido pendiente:", SwingConstants.RIGHT));
        form.add(cmbPedidos);
        form.add(new JLabel("Repartidor disponible:", SwingConstants.RIGHT));
        form.add(cmbRepartidores);
        raiz.add(form, BorderLayout.CENTER);

        JButton btnCerrar = Estilo.boton("Cerrar");
        JPanel sur = new JPanel(new BorderLayout());
        lblEstado.setForeground(Color.DARK_GRAY);
        sur.add(lblEstado, BorderLayout.WEST);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.add(btnCerrar);
        botones.add(btnIniciar);
        sur.add(botones, BorderLayout.EAST);
        raiz.add(sur, BorderLayout.SOUTH);

        setContentPane(raiz);

        btnIniciar.addActionListener(e -> iniciarEntrega());
        btnCerrar.addActionListener(e -> dispose());
    }

    /** Recarga los combos (pedidos pendientes desde MySQL) manteniendo la selección si sigue siendo válida. */
    private void cargarCombos() {
        Object pedidoSel = cmbPedidos.getSelectedItem();
        Object repSel = cmbRepartidores.getSelectedItem();

        try {
            cmbPedidos.removeAllItems();
            for (Pedido p : controlador.getPedidosPendientes()) {
                cmbPedidos.addItem(p);
            }
            cmbRepartidores.removeAllItems();
            for (Repartidor r : controlador.getRepartidoresDisponibles()) {
                cmbRepartidores.addItem(r);
            }
        } catch (DAOException e) {
            btnIniciar.setEnabled(false);
            lblEstado.setText("Sin conexión con la base de datos.");
            return;
        }
        if (pedidoSel != null) cmbPedidos.setSelectedItem(pedidoSel);
        if (repSel != null) cmbRepartidores.setSelectedItem(repSel);

        boolean hayPedidos = cmbPedidos.getItemCount() > 0;
        boolean hayRepartidores = cmbRepartidores.getItemCount() > 0;
        btnIniciar.setEnabled(hayPedidos && hayRepartidores);

        if (!hayPedidos) {
            lblEstado.setText("No hay pedidos pendientes.");
        } else if (!hayRepartidores) {
            lblEstado.setText("No hay repartidores libres (registre uno o espere una entrega).");
        } else {
            lblEstado.setText(cmbPedidos.getItemCount() + " pendiente(s), "
                    + cmbRepartidores.getItemCount() + " repartidor(es) libre(s).");
        }
    }

    private void iniciarEntrega() {
        Pedido pedido = (Pedido) cmbPedidos.getSelectedItem();
        Repartidor repartidor = (Repartidor) cmbRepartidores.getSelectedItem();
        try {
            controlador.iniciarEntrega(pedido, repartidor);
            JOptionPane.showMessageDialog(this,
                    repartidor.getNombre() + " inició la entrega del pedido #" + pedido.getId() + ".\n"
                            + "Tiempo estimado: " + pedido.calcularTiempoEntrega() + " min.",
                    "Entrega iniciada", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "No se pudo iniciar", JOptionPane.WARNING_MESSAGE);
        } catch (DAOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }
}
