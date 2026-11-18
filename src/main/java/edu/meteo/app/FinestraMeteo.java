package edu.meteo.app;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Finestra principal: el gràfic a dalt i els missatges del programa a sota.
 */
public class FinestraMeteo extends JFrame {

    private final JTextArea missatges = new JTextArea(8, 80);

    public FinestraMeteo(String titol) {
        super(titol);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel carregant = new JLabel("Descarregant dades...", SwingConstants.CENTER);
        carregant.setPreferredSize(new Dimension(900, 450));
        add(carregant, BorderLayout.CENTER);

        missatges.setEditable(false);
        missatges.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(missatges);
        scroll.setBorder(BorderFactory.createTitledBorder("Missatges"));
        add(scroll, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    /** Substitueix el missatge "Descarregant..." pel gràfic. Es pot cridar des de qualsevol fil. */
    public void mostraGrafic(JFreeChart grafic) {
        SwingUtilities.invokeLater(() -> {
            BorderLayout layout = (BorderLayout) getContentPane().getLayout();
            remove(layout.getLayoutComponent(BorderLayout.CENTER));
            ChartPanel panell = new ChartPanel(grafic);
            panell.setPreferredSize(new Dimension(900, 450));
            add(panell, BorderLayout.CENTER);
            revalidate();
            repaint();
        });
    }

    /** Afegeix una línia a la capsa de missatges. Es pot cridar des de qualsevol fil. */
    public void afegeixMissatge(String linia) {
        SwingUtilities.invokeLater(() -> {
            missatges.append(linia);
            missatges.setCaretPosition(missatges.getDocument().getLength());
        });
    }
}
