package controlador;

import dao.DAOException;
import dao.EntregaDAO;
import dao.PedidoDAO;
import dao.RepartidorDAO;
import modelo.Entrega;
import modelo.EstadoPedido;
import modelo.Pedido;
import modelo.Repartidor;
import modelo.TipoPedido;
import tareas.TareaEntrega;

import javax.swing.SwingUtilities;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * CONTROLADOR (MVC): intermediario entre las ventanas, el modelo y los DAO.
 *
 * Semana 7: ya no guarda los datos en listas en memoria. Los pedidos, repartidores y
 * entregas viven en MySQL y se leen/escriben con PedidoDAO, RepartidorDAO y EntregaDAO.
 * Solo se mantiene en memoria la lista de repartidores, porque cada uno lleva
 * el estado momentáneo "disponible / en ruta" de la simulación con hilos.
 *
 * Todas las ventanas reciben ESTA MISMA instancia y se suscriben como observadores
 * para refrescar sus tablas cuando algo cambia.
 *
 * @author Rodolfo Delgado
 */
public class ControladorPedidos {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final PedidoDAO pedidoDAO = new PedidoDAO();
    private final RepartidorDAO repartidorDAO = new RepartidorDAO();
    private final EntregaDAO entregaDAO = new EntregaDAO();

    // Repartidores cargados desde la BD (guardan si están libres o en ruta)
    private final List<Repartidor> repartidores = new CopyOnWriteArrayList<>();
    private boolean inicializado = false;

    // Ventanas suscritas. CopyOnWrite: se pueden agregar/quitar mientras se notifica.
    private final List<Runnable> observadoresCambios = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> observadoresBitacora = new CopyOnWriteArrayList<>();

    // ==================================================================
    // Inicio: lectura de datos desde la base de datos
    // ==================================================================

    /**
     * Prepara los datos: devuelve a PENDIENTE las entregas que quedaron a medias si la
     * aplicación se cerró, y carga los repartidores. Si falla, se puede reintentar.
     *
     * @throws DAOException si no hay conexión con la base de datos
     */
    public synchronized void inicializar() {
        if (inicializado) {
            return;
        }
        int reiniciados = pedidoDAO.reiniciarEntregasInterrumpidas();
        repartidores.clear();
        repartidores.addAll(repartidorDAO.listarTodos());
        inicializado = true;

        registrarEvento("Conectado a MySQL (speedfast_db): " + repartidores.size() + " repartidor(es) cargado(s).");
        if (reiniciados > 0) {
            registrarEvento(reiniciados + " entrega(s) quedó/quedaron interrumpida(s) y volvió/volvieron a PENDIENTE.");
        }
        notificarCambios();
    }

    private void asegurarInicializado() {
        if (!inicializado) {
            inicializar();
        }
    }

    // ==================================================================
    // Registro de pedidos y repartidores (INSERT)
    // ==================================================================

    /**
     * Valida los datos del formulario, crea el pedido del tipo elegido y lo guarda en MySQL.
     * El ID lo genera la base de datos (AUTO_INCREMENT).
     *
     * @throws IllegalArgumentException con un mensaje listo para mostrar al usuario
     * @throws DAOException             si falla la base de datos
     */
    public Pedido registrarPedido(String direccion, TipoPedido tipo) {
        if (direccion == null || direccion.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar la dirección de entrega.");
        }
        if (direccion.trim().length() < 5) {
            throw new IllegalArgumentException("La dirección debe tener al menos 5 caracteres.");
        }
        if (direccion.trim().length() > 150) {
            throw new IllegalArgumentException("La dirección no puede superar los 150 caracteres.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Debe seleccionar un tipo de pedido.");
        }

        Pedido pedido = Pedido.crear(0, direccion, tipo);   // Polimorfismo: crea la subclase según el tipo
        pedidoDAO.guardar(pedido);
        registrarEvento("Pedido " + pedido + " guardado en la base de datos (" + pedido.getEstado() + ").");
        notificarCambios();
        return pedido;
    }

    /**
     * Valida el nombre y guarda un repartidor nuevo en MySQL.
     *
     * @throws IllegalArgumentException con un mensaje listo para mostrar al usuario
     * @throws DAOException             si falla la base de datos
     */
    public Repartidor registrarRepartidor(String nombre) {
        asegurarInicializado();
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar el nombre del repartidor.");
        }
        String limpio = nombre.trim();
        if (limpio.length() < 2) {
            throw new IllegalArgumentException("El nombre debe tener al menos 2 caracteres.");
        }
        if (limpio.length() > 100) {
            throw new IllegalArgumentException("El nombre no puede superar los 100 caracteres.");
        }
        for (Repartidor r : repartidores) {
            if (r.getNombre().equalsIgnoreCase(limpio)) {
                throw new IllegalArgumentException("Ya existe un repartidor llamado " + r.getNombre() + ".");
            }
        }

        Repartidor repartidor = new Repartidor(limpio);
        repartidorDAO.guardar(repartidor);
        repartidores.add(repartidor);
        registrarEvento("Repartidor " + repartidor.getNombre() + " (id " + repartidor.getId()
                + ") guardado en la base de datos.");
        notificarCambios();
        return repartidor;
    }

    // ==================================================================
    // Asignación e inicio de entregas
    // ==================================================================

    /**
     * Asigna el repartidor al pedido, deja constancia en MySQL (estado EN_REPARTO y una
     * fila en la tabla entrega) y lanza la entrega en un hilo aparte, para que la ventana
     * no se congele mientras se simula el recorrido.
     */
    public void iniciarEntrega(Pedido pedido, Repartidor repartidor) {
        if (pedido == null) {
            throw new IllegalArgumentException("Seleccione un pedido pendiente.");
        }
        if (repartidor == null) {
            throw new IllegalArgumentException("Seleccione un repartidor disponible.");
        }
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new IllegalArgumentException("El pedido #" + pedido.getId() + " ya está " + pedido.getEstado() + ".");
        }
        if (!repartidor.ocupar()) {
            throw new IllegalArgumentException(repartidor.getNombre() + " ya está realizando una entrega.");
        }

        try {
            pedido.iniciarReparto(repartidor.getNombre());
            pedidoDAO.actualizarEstado(pedido.getId(), EstadoPedido.EN_REPARTO);
            entregaDAO.guardar(new Entrega(pedido, repartidor));
        } catch (DAOException | IllegalStateException e) {
            // Si algo falló se deshace todo para no dejar datos a medias
            pedido.volverAPendiente();
            repartidor.liberar();
            try {
                pedidoDAO.actualizarEstado(pedido.getId(), EstadoPedido.PENDIENTE);
            } catch (DAOException ignorada) {
                // Sin conexión no se puede corregir; al reiniciar la app se normaliza
            }
            throw e;
        }

        registrarEvento("[" + repartidor.getNombre() + "] Retira pedido #" + pedido.getId()
                + " -> " + pedido.getDireccionEntrega() + " (" + pedido.getTipo()
                + ", " + pedido.calcularTiempoEntrega() + " min estimados)");
        notificarCambios();

        Thread hilo = new Thread(new TareaEntrega(pedido, repartidor, this),
                "Repartidor-" + repartidor.getNombre());
        hilo.setDaemon(true);   // no impide cerrar la aplicación
        hilo.start();
    }

    /** Lo llama TareaEntrega (desde su hilo) al terminar el recorrido. */
    public void finalizarEntrega(Pedido pedido, Repartidor repartidor) {
        try {
            pedido.marcarEntregado();
            pedidoDAO.actualizarEstado(pedido.getId(), EstadoPedido.ENTREGADO);
            registrarEvento("[" + repartidor.getNombre() + "] Pedido #" + pedido.getId() + " ENTREGADO. "
                    + repartidor.getNombre() + " queda disponible.");
        } catch (DAOException e) {
            registrarEvento("[ERROR] Pedido #" + pedido.getId() + " entregado, pero no se pudo guardar en la BD: "
                    + e.getMessage());
        } finally {
            repartidor.liberar();   // el repartidor siempre queda libre
            notificarCambios();
        }
    }

    // ==================================================================
    // Consultas (SELECT)
    // ==================================================================

    /** Todos los pedidos guardados en MySQL, con su repartidor. */
    public List<Pedido> getPedidos() {
        return pedidoDAO.listarTodos();
    }

    public List<Pedido> getPedidosPendientes() {
        List<Pedido> pendientes = new ArrayList<>();
        for (Pedido p : pedidoDAO.listarTodos()) {
            if (p.getEstado() == EstadoPedido.PENDIENTE) {
                pendientes.add(p);
            }
        }
        return pendientes;
    }

    public List<Repartidor> getRepartidores() {
        asegurarInicializado();
        return new ArrayList<>(repartidores);
    }

    public List<Repartidor> getRepartidoresDisponibles() {
        asegurarInicializado();
        List<Repartidor> libres = new ArrayList<>();
        for (Repartidor r : repartidores) {
            if (r.isDisponible()) {
                libres.add(r);
            }
        }
        return libres;
    }

    /** Historial de entregas (JOIN entre entrega, pedido y repartidor). */
    public List<Entrega> getEntregas() {
        return entregaDAO.listarTodas();
    }

    public Map<EstadoPedido, Integer> contarPorEstado() {
        return pedidoDAO.contarPorEstado();
    }

    // ==================================================================
    // Observadores: las ventanas se suscriben para refrescarse solas
    // ==================================================================

    public void agregarObservador(Runnable observador)  { observadoresCambios.add(observador); }
    public void quitarObservador(Runnable observador)   { observadoresCambios.remove(observador); }
    public void agregarObservadorBitacora(Consumer<String> o) { observadoresBitacora.add(o); }

    /** Swing no es thread-safe: se avisa siempre dentro del Event Dispatch Thread. */
    private void notificarCambios() {
        SwingUtilities.invokeLater(() -> {
            for (Runnable o : observadoresCambios) {
                o.run();
            }
        });
    }

    private void registrarEvento(String mensaje) {
        String linea = LocalTime.now().format(HORA) + "  " + mensaje;
        SwingUtilities.invokeLater(() -> {
            for (Consumer<String> o : observadoresBitacora) {
                o.accept(linea);
            }
        });
    }
}
