package edu.meteo.dades;

import java.time.LocalDate;

/**
 * Dades meteorològiques d'un dia.
 *
 * @param data         dia de la mesura
 * @param maxima       temperatura màxima (°C)
 * @param minima       temperatura mínima (°C)
 * @param precipitacio precipitació total del dia (mm)
 */
public record MesuraDiaria(LocalDate data, double maxima, double minima, double precipitacio) {

    /** Temperatura mitjana aproximada del dia. */
    public double mitjana() {
        return (maxima + minima) / 2.0;
    }
}
