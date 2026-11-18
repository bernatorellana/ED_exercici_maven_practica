package edu.meteo.app;

import edu.meteo.dades.MesuraDiaria;
import edu.meteo.dades.Ubicacio;
import edu.meteo.estadistica.ResumEstadistic;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Construeix el gràfic de temperatures. Utilitza JFreeChart.
 */
public class GraficTemperatures {

    public JFreeChart crea(Ubicacio ubicacio, List<MesuraDiaria> mesures, ResumEstadistic resum) {
        TimeSeries maximes = new TimeSeries("Màxima");
        TimeSeries minimes = new TimeSeries("Mínima");
        TimeSeries tendencia = new TimeSeries("Tendència de la mitjana");

        for (MesuraDiaria m : mesures) {
            Day dia = dia(m.data());
            maximes.add(dia, m.maxima());
            minimes.add(dia, m.minima());
            tendencia.add(dia, resum.tendencia(m.data()));
        }

        TimeSeriesCollection series = new TimeSeriesCollection();
        series.addSeries(maximes);
        series.addSeries(minimes);
        series.addSeries(tendencia);

        JFreeChart grafic = ChartFactory.createTimeSeriesChart(
                "Temperatures a " + ubicacio.nom(),   // títol
                "Data",                               // eix X
                "°C",                                 // eix Y
                series,
                true,                                 // llegenda
                true,                                 // tooltips
                false);                               // URLs

        XYPlot plot = grafic.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(0xD9, 0x48, 0x2B));
        renderer.setSeriesPaint(1, new Color(0x2B, 0x6C, 0xD9));
        renderer.setSeriesPaint(2, Color.DARK_GRAY);
        renderer.setSeriesStroke(2, new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[] {6f, 6f}, 0f));
        return grafic;
    }

    public void desaPng(JFreeChart grafic, File fitxer, int amplada, int alcada) throws IOException {
        ChartUtils.saveChartAsPNG(fitxer, grafic, amplada, alcada);
    }

    private static Day dia(LocalDate data) {
        return new Day(data.getDayOfMonth(), data.getMonthValue(), data.getYear());
    }
}
