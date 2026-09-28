package modelo;

/**
 * Clase ABSTRACTA base de los pedidos de SpeedFast (viene de las semanas 2 a 6).
 *
 * Cada subclase define su tiempo estimado de entrega (polimorfismo).
 * Novedad de la semana 7: el ID ya no lo elige el usuario, lo genera MySQL
 * (AUTO_INCREMENT) cuando PedidoDAO guarda el pedido. Mientras no se guarde, el ID vale 0.
 *
 * El estado y el repartidor son volatile y los cambios de estado son synchronized,
 * porque la entrega se simula en un hilo distinto al de la interfaz gráfica.
 *
 * @author Rodolfo Delgado
 */
public abstract class Pedido {

    private int id;
    private final String direccionEntrega;
    private final TipoPedido tipo;
    private volatile EstadoPedido estado = EstadoPedido.PENDIENTE;
    private volatile String repartidorAsignado;

    /** Pedido nuevo, todavía sin guardar en la base de datos (id = 0). */
    protected Pedido(String direccionEntrega, TipoPedido tipo) {
        this(0, direccionEntrega, tipo);
    }

    /** Pedido que ya existe en la base de datos (se usa al leer con ResultSet). */
    protected Pedido(int id, String direccionEntrega, TipoPedido tipo) {
        if (id < 0) {
            throw new IllegalArgumentException("El ID no puede ser negativo.");
        }
        if (direccionEntrega == null || direccionEntrega.trim().isEmpty()) {
            throw new IllegalArgumentException("La dirección no puede estar vacía.");
        }
        this.id = id;
        this.direccionEntrega = direccionEntrega.trim();
        this.tipo = tipo;
    }

    /** Minutos estimados de entrega: cada tipo de pedido lo resuelve distinto. */
    public abstract int calcularTiempoEntrega();

    /** Crea la subclase correcta según el tipo (lo usan el controlador y los DAO). */
    public static Pedido crear(int id, String direccion, TipoPedido tipo) {
        switch (tipo) {
            case COMIDA:     return new PedidoComida(id, direccion);
            case ENCOMIENDA: return new PedidoEncomienda(id, direccion);
            case EXPRESS:    return new PedidoExpress(id, direccion);
            default: throw new IllegalArgumentException("Tipo no soportado: " + tipo);
        }
    }

    /** PENDIENTE -> EN_REPARTO. */
    public synchronized void iniciarReparto(String nombreRepartidor) {
        if (estado != EstadoPedido.PENDIENTE) {
            throw new IllegalStateException("El pedido #" + id + " ya está " + estado + ".");
        }
        this.repartidorAsignado = nombreRepartidor;
        this.estado = EstadoPedido.EN_REPARTO;
    }

    /** EN_REPARTO -> ENTREGADO. */
    public synchronized void marcarEntregado() {
        if (estado != EstadoPedido.EN_REPARTO) {
            throw new IllegalStateException("El pedido #" + id + " no está en reparto.");
        }
        this.estado = EstadoPedido.ENTREGADO;
    }

    /** Vuelve a PENDIENTE (si no se pudo registrar la entrega en la base de datos). */
    public synchronized void volverAPendiente() {
        this.estado = EstadoPedido.PENDIENTE;
        this.repartidorAsignado = null;
    }

    /** Fija estado y repartidor tal como están guardados en la base de datos. */
    public synchronized void restaurarDesdeBD(EstadoPedido estado, String repartidor) {
        this.estado = estado;
        this.repartidorAsignado = repartidor;
    }

    /** Lo llama PedidoDAO con la clave que generó MySQL. */
    public void setId(int id) {
        this.id = id;
    }

    public int getId()                    { return id; }
    public String getDireccionEntrega()   { return direccionEntrega; }
    public TipoPedido getTipo()           { return tipo; }
    public EstadoPedido getEstado()       { return estado; }
    public String getRepartidorAsignado() { return repartidorAsignado; }

    /** Texto usado por los JComboBox. */
    @Override
    public String toString() {
        return "#" + id + " - " + tipo + " - " + direccionEntrega;
    }
}
