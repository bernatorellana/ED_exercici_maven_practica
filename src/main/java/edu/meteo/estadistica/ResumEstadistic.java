package edu.meteo.estadistica;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Resultat de l'anàlisi d'una sèrie de mesures diàries.
 */
public record ResumEstadistic(
        int dies,
        LocalDate inici,
        LocalDate fi,
        double mitjanaMaximes,
        double mitjanaMinimes,
        double medianaMaximes,
        double desviacioMaximes,
        double maximaAbsoluta,
        LocalDate dataMaximaAbsoluta,
        double minimaAbsoluta,
        LocalDate dataMinimaAbsoluta,
        double precipitacioTotal,
        int diesDePluja,
        double tendenciaOrdenada,
        double tendenciaPendentPerDia) {

    /** Valor de la recta de tendència (temperatura mitjana) en una data. */
    public double tendencia(LocalDate data) {
        long x = ChronoUnit.DAYS.between(inici, data);
        return tendenciaOrdenada + tendenciaPendentPerDia * x;
    }

    /** Resum en text, una dada per línia. */
    public String text() {
        return String.format(Locale.ROOT,
                "Període: %s - %s (%d dies)%n"
                        + "Mitjana de les màximes: %.1f °C (mediana %.1f °C, desviació %.1f °C)%n"
                        + "Mitjana de les mínimes: %.1f °C%n"
                        + "Màxima absoluta: %.1f °C el %s%n"
                        + "Mínima absoluta: %.1f °C el %s%n"
                        + "Precipitació total: %.1f mm en %d dies de pluja%n"
                        + "Tendència: %+.2f °C per setmana",
                inici, fi, dies,
                mitjanaMaximes, medianaMaximes, desviacioMaximes,
                mitjanaMinimes,
                maximaAbsoluta, dataMaximaAbsoluta,
                minimaAbsoluta, dataMinimaAbsoluta,
                precipitacioTotal, diesDePluja,
                tendenciaPendentPerDia * 7);
    }
}
