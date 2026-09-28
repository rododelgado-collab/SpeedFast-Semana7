package modelo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Una entrega relaciona un pedido con un repartidor (tabla entrega).
 * Un repartidor puede hacer muchas entregas y un pedido puede tener varias
 * (por ejemplo, si un intento se interrumpe y se vuelve a asignar).
 *
 * @author Rodolfo Delgado
 */
public class Entrega {

    private int id;
    private final int idPedido;
    private final int idRepartidor;
    private final LocalDate fecha;
    private final LocalTime hora;

    // Solo para mostrar en tablas (los rellena EntregaDAO.listarTodos con un JOIN)
    private String direccionPedido;
    private String nombreRepartidor;

    /** Entrega nueva que ocurre "ahora". */
    public Entrega(Pedido pedido, Repartidor repartidor) {
        this(0, pedido.getId(), repartidor.getId(), LocalDate.now(), LocalTime.now().withNano(0));
        this.direccionPedido = pedido.getDireccionEntrega();
        this.nombreRepartidor = repartidor.getNombre();
    }

    public Entrega(int id, int idPedido, int idRepartidor, LocalDate fecha, LocalTime hora) {
        this.id = id;
        this.idPedido = idPedido;
        this.idRepartidor = idRepartidor;
        this.fecha = fecha;
        this.hora = hora;
    }

    public int getId()                   { return id; }
    public void setId(int id)            { this.id = id; }
    public int getIdPedido()             { return idPedido; }
    public int getIdRepartidor()         { return idRepartidor; }
    public LocalDate getFecha()          { return fecha; }
    public LocalTime getHora()           { return hora; }
    public String getDireccionPedido()   { return direccionPedido; }
    public String getNombreRepartidor()  { return nombreRepartidor; }

    public void setDatosVisibles(String direccionPedido, String nombreRepartidor) {
        this.direccionPedido = direccionPedido;
        this.nombreRepartidor = nombreRepartidor;
    }
}
