package edu.meteo.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import javax.imageio.ImageIO;

import edu.meteo.dades.MesuraDiaria;
import edu.meteo.dades.Ubicacio;
import edu.meteo.estadistica.AnalitzadorTemperatures;
import edu.meteo.estadistica.ResumEstadistic;
import org.jfree.chart.JFreeChart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GraficTemperaturesTest {

    @Test
    void desaElGraficEnUnPngDeLaMidaIndicada(@TempDir Path carpeta) throws Exception {
        LocalDate dia = LocalDate.of(2026, 11, 1);
        List<MesuraDiaria> mesures = List.of(
                new MesuraDiaria(dia, 20, 10, 0),
                new MesuraDiaria(dia.plusDays(1), 22, 12, 3));
        ResumEstadistic resum = new AnalitzadorTemperatures().resumeix(mesures);
        Ubicacio lloc = new Ubicacio("Prova", null, null, 0, 0);

        GraficTemperatures grafics = new GraficTemperatures();
        JFreeChart grafic = grafics.crea(lloc, mesures, resum);
        File png = carpeta.resolve("grafic.png").toFile();
        grafics.desaPng(grafic, png, 640, 320);

        BufferedImage imatge = ImageIO.read(png);
        assertEquals(640, imatge.getWidth());
        assertEquals(320, imatge.getHeight());
        assertEquals(3, grafic.getXYPlot().getDataset().getSeriesCount());
    }
}
