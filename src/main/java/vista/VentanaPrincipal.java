package vista;

import controlador.ControladorPedidos;
import dao.DAOException;
import modelo.EstadoPedido;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * VISTA: ventana principal de SpeedFast.
 *
 * Desde aquí se abren las demás ventanas; a todas se les pasa el MISMO
 * ControladorPedidos, que lee y escribe en MySQL a través de los DAO.
 *
 * Distribución (BorderLayout):
 *   NORTH  -> encabezado
 *   WEST   -> botones de navegación (GridLayout)
 *   CENTER -> bitácora de actividad (JTextArea)
 *   SOUTH  -> barra de resumen
 *
 * @author Rodolfo Delgado
 */
public class VentanaPrincipal extends JFrame {

    private final ControladorPedidos controlador;

    private final JTextArea txtActividad = new JTextArea();
    private final JLabel lblResumen = new JLabel();

    // Una sola instancia de cada ventana secundaria (no se abren duplicadas)
    private VentanaRegistroPedido ventanaRegistro;
    private VentanaRepartidores ventanaRepartidores;
    private VentanaListaPedidos ventanaLista;
    private VentanaAsignarRepartidor ventanaAsignar;

    /** Constructor que usa Main: crea el controlador compartido. */
    public VentanaPrincipal() {
        this(new ControladorPedidos());
    }

    public VentanaPrincipal(ControladorPedidos controlador) {
        this.controlador = controlador;
        configurarVentana();
        crearComponentes();

        controlador.agregarObservador(this::actualizarResumen);
        controlador.agregarObservadorBitacora(this::agregarActividad);
        agregarActividad("Sistema SpeedFast iniciado.");
        try {
            controlador.inicializar();   // lee repartidores desde MySQL
        } catch (DAOException e) {
            agregarActividad("[ERROR] " + e.getMessage());
        }
        actualizarResumen();

        setVisible(true);
    }

    private void configurarVentana() {
        setTitle("SpeedFast - Gestión de entregas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(820, 480);
        setMinimumSize(new Dimension(700, 400));
        setLocationRelativeTo(null);
    }

    private void crearComponentes() {
        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.setBorder(Estilo.margen(10));

        raiz.add(Estilo.encabezado("SpeedFast",
                "Registro de pedidos y repartidores en MySQL, listados y simulación de entregas"), BorderLayout.NORTH);

        // --- Menú de botones ---
        JButton btnRegistrar = Estilo.boton("Registrar pedido");
        JButton btnRepartidores = Estilo.boton("Repartidores");
        JButton btnListar = Estilo.boton("Listar pedidos y entregas");
        JButton btnAsignar = Estilo.boton("Asignar repartidor / Iniciar entrega");
        JButton btnSalir = Estilo.boton("Salir");

        JPanel menu = new JPanel(new GridLayout(5, 1, 0, 10));
        menu.setBorder(BorderFactory.createTitledBorder("Acciones"));
        menu.add(btnRegistrar);
        menu.add(btnRepartidores);
        menu.add(btnListar);
        menu.add(btnAsignar);
        menu.add(btnSalir);

        JPanel contenedorMenu = new JPanel(new BorderLayout());
        contenedorMenu.add(menu, BorderLayout.NORTH);   // botones arriba, sin estirarse
        raiz.add(contenedorMenu, BorderLayout.WEST);

        // --- Bitácora ---
        txtActividad.setEditable(false);
        txtActividad.setFont(Estilo.MONO);
        txtActividad.setLineWrap(true);
        txtActividad.setWrapStyleWord(true);
        txtActividad.setMargin(new Insets(6, 6, 6, 6));
        JScrollPane scroll = new JScrollPane(txtActividad);
        scroll.setBorder(BorderFactory.createTitledBorder("Actividad"));
        raiz.add(scroll, BorderLayout.CENTER);

        // --- Resumen ---
        lblResumen.setBorder(Estilo.margen(4));
        raiz.add(lblResumen, BorderLayout.SOUTH);

        setContentPane(raiz);

        // --- Eventos (ActionListener con lambdas) ---
        btnRegistrar.addActionListener(e -> abrirRegistro());
        btnRepartidores.addActionListener(e -> abrirRepartidores());
        btnListar.addActionListener(e -> abrirLista());
        btnAsignar.addActionListener(e -> abrirAsignacion());
        btnSalir.addActionListener(e -> salir());
    }

    // ==================================================================
    // Navegación
    // ==================================================================

    private void abrirRegistro() {
        if (ventanaRegistro == null || !ventanaRegistro.isDisplayable()) {
            ventanaRegistro = new VentanaRegistroPedido(controlador);
        }
        mostrar(ventanaRegistro);
    }

    private void abrirRepartidores() {
        if (ventanaRepartidores == null || !ventanaRepartidores.isDisplayable()) {
            ventanaRepartidores = new VentanaRepartidores(controlador);
        }
        mostrar(ventanaRepartidores);
    }

    private void abrirLista() {
        if (ventanaLista == null || !ventanaLista.isDisplayable()) {
            ventanaLista = new VentanaListaPedidos(controlador);
        }
        mostrar(ventanaLista);
    }

    private void abrirAsignacion() {
        if (ventanaAsignar == null || !ventanaAsignar.isDisplayable()) {
            ventanaAsignar = new VentanaAsignarRepartidor(controlador);
        }
        mostrar(ventanaAsignar);
    }

    private void mostrar(JFrame ventana) {
        ventana.setVisible(true);
        ventana.toFront();
    }

    private void salir() {
        int opcion = JOptionPane.showConfirmDialog(this, "¿Desea cerrar SpeedFast?",
                "Salir", JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    // ==================================================================
    // Actualizaciones que dispara el controlador
    // ==================================================================

    /** Lee los contadores desde MySQL (una consulta con GROUP BY). */
    private void actualizarResumen() {
        try {
            Map<EstadoPedido, Integer> cuenta = controlador.contarPorEstado();
            int total = cuenta.values().stream().mapToInt(Integer::intValue).sum();
            lblResumen.setText("Pedidos: " + total
                    + "   |   Pendientes: " + cuenta.get(EstadoPedido.PENDIENTE)
                    + "   |   En reparto: " + cuenta.get(EstadoPedido.EN_REPARTO)
                    + "   |   Entregados: " + cuenta.get(EstadoPedido.ENTREGADO)
                    + "   |   Repartidores libres: " + controlador.getRepartidoresDisponibles().size());
        } catch (DAOException e) {
            lblResumen.setText("Sin conexión con la base de datos (revise MySQL y db.properties)");
        }
    }

    private void agregarActividad(String linea) {
        txtActividad.append(linea + "\n");
        txtActividad.setCaretPosition(txtActividad.getDocument().getLength());
    }
}
