package dao;

/**
 * Error de acceso a datos. Los DAO capturan la SQLException (try-catch) y la
 * relanzan con un mensaje claro para el usuario; las ventanas lo muestran con
 * un JOptionPane en vez de dejar un stack trace en la consola.
 *
 * @author Rodolfo Delgado
 */
public class DAOException extends RuntimeException {

    public DAOException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
