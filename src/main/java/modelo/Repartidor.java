package modelo;

/**
 * Repartidor de SpeedFast. Solo puede llevar un pedido a la vez:
 * ocupar() y liberar() son synchronized porque los llama el hilo de la
 * interfaz y el hilo de la entrega.
 *
 * Semana 7: ahora tiene un ID (clave de la tabla repartidor). La disponibilidad
 * NO se guarda en la base de datos: es el estado momentáneo de la simulación.
 *
 * @author Rodolfo Delgado
 */
public class Repartidor {

    private int id;
    private final String nombre;
    private boolean disponible = true;

    /** Repartidor nuevo, todavía sin guardar (id = 0). */
    public Repartidor(String nombre) {
        this(0, nombre);
    }

    /** Repartidor que ya existe en la base de datos. */
    public Repartidor(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId()        { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() {
        return nombre;
    }

    public synchronized boolean isDisponible() {
        return disponible;
    }

    /** @return true si quedó asignado; false si ya estaba ocupado. */
    public synchronized boolean ocupar() {
        if (!disponible) {
            return false;
        }
        disponible = false;
        return true;
    }

    public synchronized void liberar() {
        disponible = true;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
