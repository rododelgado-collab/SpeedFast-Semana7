package modelo;

/** Encomienda: paquete sin urgencia, tiempo estimado 60 min. @author Rodolfo Delgado */
public class PedidoEncomienda extends Pedido {

    /** Pedido nuevo (el ID lo asigna MySQL al guardar). */
    public PedidoEncomienda(String direccionEntrega) {
        super(direccionEntrega, TipoPedido.ENCOMIENDA);
    }

    /** Pedido leído desde la base de datos. */
    public PedidoEncomienda(int id, String direccionEntrega) {
        super(id, direccionEntrega, TipoPedido.ENCOMIENDA);
    }

    @Override
    public int calcularTiempoEntrega() {
        return 60;
    }
}
