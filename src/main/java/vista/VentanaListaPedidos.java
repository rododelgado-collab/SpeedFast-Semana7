package vista;

import controlador.ControladorPedidos;
import dao.DAOException;
import modelo.Entrega;
import modelo.Pedido;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.format.DateTimeFormatter;

/**
 * VISTA: consulta de los datos guardados en MySQL, en dos JTable dentro de pestañas:
 *   - Pedidos:  SELECT de PedidoDAO (con el repartidor de su última entrega).
 *   - Entregas: SELECT con JOIN de EntregaDAO (historial pedido - repartidor - fecha - hora).
 *
 * Cada vez que se abre, se refresca o el controlador avisa un cambio, las tablas
 * se vacían y se vuelven a llenar leyendo la base de datos.
 *
 * @author Rodolfo Delgado
 */
public class VentanaListaPedidos extends JFrame {

    private static final String[] COLUMNAS_PEDIDOS =
            {"ID", "Dirección", "Tipo", "Min. estimados", "Estado", "Repartidor"};
    private static final String[] COLUMNAS_ENTREGAS =
            {"ID entrega", "ID pedido", "Dirección", "Repartidor", "Fecha", "Hora"};
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final ControladorPedidos controlador;
    private final Runnable observador = this::cargarDatos;

    private final DefaultTableModel modeloPedidos = crearModeloSoloLectura(COLUMNAS_PEDIDOS);
    private final DefaultTableModel modeloEntregas = crearModeloSoloLectura(COLUMNAS_ENTREGAS);
    private final JTable tablaPedidos = new JTable(modeloPedidos);
    private final JTable tablaEntregas = new JTable(modeloEntregas);
    private final JLabel lblTotal = new JLabel();

    public VentanaListaPedidos(ControladorPedidos controlador) {
        this.controlador = controlador;

        setTitle("SpeedFast - Pedidos y entregas");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(820, 440);
        setLocationRelativeTo(null);

        crearComponentes();
        cargarDatos();

        // Suscribirse a los cambios y desuscribirse al cerrar
        controlador.agregarObservador(observador);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                controlador.quitarObservador(observador);
            }
        });
    }

    /** Celdas de solo lectura: los datos se cambian desde el controlador, no a mano. */
    private static DefaultTableModel crearModeloSoloLectura(String[] columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    private void crearComponentes() {
        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.setBorder(Estilo.margen(10));
        raiz.add(Estilo.encabezado("Datos guardados en MySQL",
                "Pedidos y entregas leídos desde la base de datos speedfast_db"), BorderLayout.NORTH);

        configurarTabla(tablaPedidos, new int[]{0, 2, 3, 4, 5});
        tablaPedidos.getColumnModel().getColumn(0).setPreferredWidth(50);
        tablaPedidos.getColumnModel().getColumn(1).setPreferredWidth(240);
        configurarTabla(tablaEntregas, new int[]{0, 1, 3, 4, 5});
        tablaEntregas.getColumnModel().getColumn(2).setPreferredWidth(240);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Pedidos", new JScrollPane(tablaPedidos));
        pestanas.addTab("Entregas", new JScrollPane(tablaEntregas));
        raiz.add(pestanas, BorderLayout.CENTER);

        JButton btnRefrescar = Estilo.boton("Refrescar");
        JButton btnCerrar = Estilo.boton("Cerrar");
        JPanel sur = new JPanel(new BorderLayout());
        sur.add(lblTotal, BorderLayout.WEST);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.add(btnRefrescar);
        botones.add(btnCerrar);
        sur.add(botones, BorderLayout.EAST);
        raiz.add(sur, BorderLayout.SOUTH);

        setContentPane(raiz);

        btnRefrescar.addActionListener(e -> cargarDatos());
        btnCerrar.addActionListener(e -> dispose());
    }

    private void configurarTabla(JTable tabla, int[] columnasCentradas) {
        tabla.setRowHeight(22);
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setAutoCreateRowSorter(true);   // ordenar al hacer clic en la cabecera

        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(SwingConstants.CENTER);
        for (int col : columnasCentradas) {
            tabla.getColumnModel().getColumn(col).setCellRenderer(centrado);
        }
    }

    /** Vacía los modelos y los vuelve a llenar con lo que devuelven los DAO. */
    private void cargarDatos() {
        try {
            modeloPedidos.setRowCount(0);
            for (Pedido p : controlador.getPedidos()) {
                modeloPedidos.addRow(new Object[]{
                        p.getId(),
                        p.getDireccionEntrega(),
                        p.getTipo(),
                        p.calcularTiempoEntrega(),
                        p.getEstado(),
                        p.getRepartidorAsignado() == null ? "-" : p.getRepartidorAsignado()
                });
            }
            modeloEntregas.setRowCount(0);
            for (Entrega e : controlador.getEntregas()) {
                modeloEntregas.addRow(new Object[]{
                        e.getId(),
                        e.getIdPedido(),
                        e.getDireccionPedido(),
                        e.getNombreRepartidor(),
                        e.getFecha().format(FECHA),
                        e.getHora()
                });
            }
            lblTotal.setText("Pedidos: " + modeloPedidos.getRowCount()
                    + "   |   Entregas registradas: " + modeloEntregas.getRowCount());
        } catch (DAOException ex) {
            lblTotal.setText("No se pudieron leer los datos de la base de datos.");
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }
}
