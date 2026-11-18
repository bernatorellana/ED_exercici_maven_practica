package edu.meteo.estadistica;

import edu.meteo.dades.MesuraDiaria;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Calcula estadístiques sobre una sèrie de mesures diàries.
 * Utilitza Apache Commons Math.
 */
public class AnalitzadorTemperatures {

    /** Precipitació mínima (mm) perquè un dia es compti com a dia de pluja. */
    public static final double LLINDAR_PLUJA = 1.0;

    public ResumEstadistic resumeix(List<MesuraDiaria> mesures) {
        if (mesures.isEmpty()) {
            throw new IllegalArgumentException("No hi ha cap mesura per analitzar");
        }

        DescriptiveStatistics maximes = new DescriptiveStatistics();
        DescriptiveStatistics minimes = new DescriptiveStatistics();
        SimpleRegression regressio = new SimpleRegression();
        double precipitacio = 0;
        int diesDePluja = 0;

        LocalDate inici = mesures.get(0).data();
        for (MesuraDiaria m : mesures) {
            maximes.addValue(m.maxima());
            minimes.addValue(m.minima());
            // Regressió lineal: x = dies des de l'inici, y = temperatura mitjana del dia
            regressio.addData(ChronoUnit.DAYS.between(inici, m.data()), m.mitjana());
            precipitacio += m.precipitacio();
            if (m.precipitacio() >= LLINDAR_PLUJA) {
                diesDePluja++;
            }
        }

        MesuraDiaria diaMesCalent = mesures.stream().max(Comparator.comparingDouble(MesuraDiaria::maxima)).orElseThrow();
        MesuraDiaria diaMesFred = mesures.stream().min(Comparator.comparingDouble(MesuraDiaria::minima)).orElseThrow();

        return new ResumEstadistic(
                mesures.size(),
                inici,
                mesures.get(mesures.size() - 1).data(),
                maximes.getMean(),
                minimes.getMean(),
                maximes.getPercentile(50),
                maximes.getStandardDeviation(),
                diaMesCalent.maxima(),
                diaMesCalent.data(),
                diaMesFred.minima(),
                diaMesFred.data(),
                precipitacio,
                diesDePluja,
                regressio.getIntercept(),
                regressio.getSlope());
    }
}
