package vista;

import controlador.ControladorPedidos;
import dao.DAOException;
import modelo.Repartidor;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * VISTA: registrar repartidores en MySQL (RepartidorDAO.guardar) y listarlos
 * en un JTable (RepartidorDAO.listarTodos). La columna Estado indica si el
 * repartidor está libre o en ruta en la simulación.
 *
 * @author Rodolfo Delgado
 */
public class VentanaRepartidores extends JFrame {

    private static final String[] COLUMNAS = {"ID", "Nombre", "Estado"};

    private final ControladorPedidos controlador;
    private final Runnable observador = this::cargarRepartidores;

    private final JTextField txtNombre = new JTextField(20);
    private final DefaultTableModel modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);
    private final JLabel lblTotal = new JLabel();

    public VentanaRepartidores(ControladorPedidos controlador) {
        this.controlador = controlador;

        setTitle("SpeedFast - Repartidores");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(520, 420);
        setLocationRelativeTo(null);

        crearComponentes();
        cargarRepartidores();

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
        raiz.setBorder(Estilo.margen(10));
        raiz.add(Estilo.encabezado("Repartidores",
                "Se guardan en la tabla repartidor de MySQL"), BorderLayout.NORTH);

        // --- Formulario ---
        JButton btnGuardar = Estilo.boton("Guardar");
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.setBorder(BorderFactory.createTitledBorder("Nuevo repartidor"));
        form.add(new JLabel("Nombre:"));
        form.add(txtNombre);
        form.add(btnGuardar);

        // --- Tabla ---
        tabla.setRowHeight(22);
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setAutoCreateRowSorter(true);
        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(SwingConstants.CENTER);
        tabla.getColumnModel().getColumn(0).setCellRenderer(centrado);
        tabla.getColumnModel().getColumn(2).setCellRenderer(centrado);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(50);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(240);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(form, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        raiz.add(centro, BorderLayout.CENTER);

        JButton btnCerrar = Estilo.boton("Cerrar");
        JPanel sur = new JPanel(new BorderLayout());
        sur.add(lblTotal, BorderLayout.WEST);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.add(btnCerrar);
        sur.add(botones, BorderLayout.EAST);
        raiz.add(sur, BorderLayout.SOUTH);

        setContentPane(raiz);
        getRootPane().setDefaultButton(btnGuardar);   // Enter = Guardar

        btnGuardar.addActionListener(e -> guardarRepartidor());
        btnCerrar.addActionListener(e -> dispose());
    }

    private void guardarRepartidor() {
        try {
            Repartidor r = controlador.registrarRepartidor(txtNombre.getText());
            JOptionPane.showMessageDialog(this,
                    "Repartidor " + r.getNombre() + " guardado con ID " + r.getId() + ".",
                    "Repartidor registrado", JOptionPane.INFORMATION_MESSAGE);
            txtNombre.setText("");
            txtNombre.requestFocus();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Datos inválidos", JOptionPane.WARNING_MESSAGE);
            txtNombre.requestFocus();
            txtNombre.selectAll();
        } catch (DAOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarRepartidores() {
        try {
            modeloTabla.setRowCount(0);
            for (Repartidor r : controlador.getRepartidores()) {
                modeloTabla.addRow(new Object[]{
                        r.getId(),
                        r.getNombre(),
                        r.isDisponible() ? "Disponible" : "En ruta"
                });
            }
            lblTotal.setText("Total de repartidores: " + modeloTabla.getRowCount());
        } catch (DAOException ex) {
            lblTotal.setText("No se pudieron leer los repartidores de la base de datos.");
        }
    }
}
