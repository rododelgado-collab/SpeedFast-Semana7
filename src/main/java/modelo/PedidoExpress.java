package modelo;

/** Pedido express: prioridad máxima, tiempo estimado 15 min. @author Rodolfo Delgado */
public class PedidoExpress extends Pedido {

    /** Pedido nuevo (el ID lo asigna MySQL al guardar). */
    public PedidoExpress(String direccionEntrega) {
        super(direccionEntrega, TipoPedido.EXPRESS);
    }

    /** Pedido leído desde la base de datos. */
    public PedidoExpress(int id, String direccionEntrega) {
        super(id, direccionEntrega, TipoPedido.EXPRESS);
    }

    @Override
    public int calcularTiempoEntrega() {
        return 15;
    }
}
