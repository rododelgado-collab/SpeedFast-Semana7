package vista;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Estilo visual común para que todas las ventanas se vean coherentes
 * (mismos márgenes, colores y tipografías).
 *
 * @author Rodolfo Delgado
 */
public final class Estilo {

    public static final Color PRIMARIO = new Color(0, 102, 204);
    public static final Color FONDO_ENCABEZADO = new Color(235, 243, 252);
    public static final Font TITULO = new Font("SansSerif", Font.BOLD, 18);
    public static final Font SUBTITULO = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private Estilo() {
    }

    public static Border margen(int px) {
        return BorderFactory.createEmptyBorder(px, px, px, px);
    }

    /** Franja superior con título y subtítulo. */
    public static JPanel encabezado(String titulo, String subtitulo) {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setBackground(FONDO_ENCABEZADO);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARIO), margen(10)));
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(TITULO);
        lblTitulo.setForeground(PRIMARIO);
        JLabel lblSub = new JLabel(subtitulo);
        lblSub.setFont(SUBTITULO);
        panel.add(lblTitulo);
        panel.add(lblSub);
        return panel;
    }

    public static JButton boton(String texto) {
        JButton b = new JButton(texto);
        b.setFocusPainted(false);
        return b;
    }
}
