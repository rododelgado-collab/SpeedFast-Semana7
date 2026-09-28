package modelo;

/**
 * Tipos de pedido que ofrece SpeedFast. Alimenta el JComboBox del formulario,
 * así el usuario solo puede elegir valores válidos.
 *
 * @author Rodolfo Delgado
 */
public enum TipoPedido {
    COMIDA("Comida"),
    ENCOMIENDA("Encomienda"),
    EXPRESS("Express");

    private final String descripcion;

    TipoPedido(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
