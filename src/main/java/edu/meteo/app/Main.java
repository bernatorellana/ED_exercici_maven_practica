package edu.meteo.app;

import edu.meteo.dades.ClientOpenMeteo;
import edu.meteo.dades.MesuraDiaria;
import edu.meteo.dades.Ubicacio;
import edu.meteo.estadistica.AnalitzadorTemperatures;
import edu.meteo.estadistica.ResumEstadistic;
import org.jfree.chart.JFreeChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Punt d'entrada de l'aplicació.
 *
 * Ús:
 *   java edu.meteo.app.Main [població] [dies]
 *
 * Per defecte: Barcelona, 90 dies. Descarrega les temperatures dels últims dies,
 * en calcula estadístiques, desa el gràfic en un PNG i el mostra en una finestra.
 * Si no hi ha entorn gràfic (per exemple en un servidor), només desa el PNG.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        String poblacio = args.length > 0 ? args[0] : "Barcelona";
        int dies = args.length > 1 ? Integer.parseInt(args[1]) : 90;

        FinestraMeteo finestra = null;
        if (GraphicsEnvironment.isHeadless()) {
            log.info("No hi ha entorn gràfic: només es desarà el gràfic en un fitxer");
        } else {
            finestra = obreFinestra("Meteo - " + poblacio);
        }

        try (ClientOpenMeteo client = new ClientOpenMeteo()) {
            Ubicacio ubicacio = client.cercaUbicacio(poblacio);
            log.info("Població trobada: {} [{}, {}]", ubicacio.descripcio(), ubicacio.latitud(), ubicacio.longitud());

            // Les dades d'avui encara no són completes: agafem fins ahir
            LocalDate fi = LocalDate.now().minusDays(1);
            LocalDate inici = fi.minusDays(dies - 1L);
            List<MesuraDiaria> mesures = client.descarregaHistoric(ubicacio, inici, fi);

            ResumEstadistic resum = new AnalitzadorTemperatures().resumeix(mesures);
            resum.text().lines().forEach(log::info);

            GraficTemperatures grafics = new GraficTemperatures();
            JFreeChart grafic = grafics.crea(ubicacio, mesures, resum);
            File png = new File("temperatures-" + ubicacio.nom().toLowerCase().replaceAll("[^a-z0-9]+", "_") + ".png");
            grafics.desaPng(grafic, png, 1000, 500);
            log.info("Gràfic desat a {}", png.getAbsolutePath());

            if (finestra != null) {
                finestra.mostraGrafic(grafic);
            }
        } catch (IOException e) {
            log.error("No s'han pogut obtenir les dades: {}", e.getMessage());
            if (finestra == null) {
                System.exit(1);
            }
        }
    }

    private static FinestraMeteo obreFinestra(String titol) throws Exception {
        FinestraMeteo[] resultat = new FinestraMeteo[1];
        SwingUtilities.invokeAndWait(() -> {
            resultat[0] = new FinestraMeteo(titol);
            AppenderAreaText.connecta(resultat[0]::afegeixMissatge);
            resultat[0].setVisible(true);
        });
        return resultat[0];
    }
}
