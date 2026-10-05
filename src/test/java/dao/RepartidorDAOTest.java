package dao;

import modelo.Repartidor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de RepartidorDAO (INSERT y SELECT) contra la base speedfast_test.
 *
 * @author Rodolfo Delgado
 */
class RepartidorDAOTest extends BaseDaoTest {

    @Test
    @DisplayName("guardar deja en el repartidor el ID generado por MySQL")
    void guardarAsignaId() {
        Repartidor r = new Repartidor("Juan");
        assertEquals(0, r.getId(), "Antes de guardar el ID vale 0");

        repartidorDAO.guardar(r);

        assertTrue(r.getId() > 0, "MySQL debe generar un ID (AUTO_INCREMENT)");
    }

    @Test
    @DisplayName("listarTodos devuelve lista vacía si no hay repartidores")
    void listarTodosSinDatos() {
        assertTrue(repartidorDAO.listarTodos().isEmpty());
    }

    @Test
    @DisplayName("listarTodos devuelve lo guardado, ordenado por ID")
    void listarTodosDevuelveLoGuardado() {
        Repartidor juan = guardarRepartidor("Juan");
        Repartidor maria = guardarRepartidor("María");

        List<Repartidor> lista = repartidorDAO.listarTodos();

        assertEquals(2, lista.size());
        assertEquals(juan.getId(), lista.get(0).getId());
        assertEquals("Juan", lista.get(0).getNombre());
        assertEquals(maria.getId(), lista.get(1).getId());
        assertEquals("María", lista.get(1).getNombre());
    }

    @Test
    @DisplayName("los nombres con tildes y eñes se guardan sin deformarse")
    void nombresConCaracteresEspeciales() {
        guardarRepartidor("Ñandú Pérez");

        assertEquals("Ñandú Pérez", repartidorDAO.listarTodos().get(0).getNombre());
    }

    @Test
    @DisplayName("un nombre más largo que la columna (100) lanza DAOException")
    void nombreDemasiadoLargo() {
        Repartidor r = new Repartidor("A".repeat(101));

        assertThrows(DAOException.class, () -> repartidorDAO.guardar(r));
        assertTrue(repartidorDAO.listarTodos().isEmpty(), "No debe quedar nada guardado");
    }
}
