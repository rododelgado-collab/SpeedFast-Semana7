package tareas;

import controlador.ControladorPedidos;
import modelo.Pedido;
import modelo.Repartidor;

/**
 * Hilo que simula el recorrido de un repartidor (viene de las semanas 4 y 5; en la semana 7 el resultado se guarda en MySQL).
 *
 * Escala: 1 minuto estimado = 100 ms, para que la simulación dure unos segundos
 * (Express 1,5 s, Comida 3 s, Encomienda 6 s).
 *
 * @author Rodolfo Delgado
 */
public class TareaEntrega implements Runnable {

    private static final int MS_POR_MINUTO = 100;

    private final Pedido pedido;
    private final Repartidor repartidor;
    private final ControladorPedidos controlador;

    public TareaEntrega(Pedido pedido, Repartidor repartidor, ControladorPedidos controlador) {
        this.pedido = pedido;
        this.repartidor = repartidor;
        this.controlador = controlador;
    }

    @Override
    public void run() {
        try {
            Thread.sleep((long) pedido.calcularTiempoEntrega() * MS_POR_MINUTO);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        controlador.finalizarEntrega(pedido, repartidor);
    }
}
