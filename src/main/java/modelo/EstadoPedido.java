package modelo;

/**
 * Ciclo de vida de un pedido en SpeedFast.
 * PENDIENTE -> EN_REPARTO -> ENTREGADO
 *
 * @author Rodolfo Delgado
 */
public enum EstadoPedido {
    PENDIENTE("Pendiente"),
    EN_REPARTO("En reparto"),
    ENTREGADO("Entregado");

    private final String descripcion;

    EstadoPedido(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
