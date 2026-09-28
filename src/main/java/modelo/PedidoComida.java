package modelo;

/** Pedido de comida: debe llegar caliente, tiempo estimado 30 min. @author Rodolfo Delgado */
public class PedidoComida extends Pedido {

    /** Pedido nuevo (el ID lo asigna MySQL al guardar). */
    public PedidoComida(String direccionEntrega) {
        super(direccionEntrega, TipoPedido.COMIDA);
    }

    /** Pedido leído desde la base de datos. */
    public PedidoComida(int id, String direccionEntrega) {
        super(id, direccionEntrega, TipoPedido.COMIDA);
    }

    @Override
    public int calcularTiempoEntrega() {
        return 30;
    }
}
