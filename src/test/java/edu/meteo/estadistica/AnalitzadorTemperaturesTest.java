package edu.meteo.estadistica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import edu.meteo.dades.MesuraDiaria;
import org.junit.jupiter.api.Test;

class AnalitzadorTemperaturesTest {

    private final AnalitzadorTemperatures analitzador = new AnalitzadorTemperatures();

    private static final LocalDate DIA1 = LocalDate.of(2026, 11, 1);

    /** Tres dies que s'escalfen exactament 1 °C cada dia. */
    private final List<MesuraDiaria> mesures = List.of(
            new MesuraDiaria(DIA1, 20, 10, 0.0),
            new MesuraDiaria(DIA1.plusDays(1), 21, 11, 5.0),
            new MesuraDiaria(DIA1.plusDays(2), 22, 12, 0.5));

    @Test
    void calculaLesMitjanesIElsExtrems() {
        ResumEstadistic resum = analitzador.resumeix(mesures);

        assertEquals(3, resum.dies());
        assertEquals(21.0, resum.mitjanaMaximes(), 1e-9);
        assertEquals(11.0, resum.mitjanaMinimes(), 1e-9);
        assertEquals(22.0, resum.maximaAbsoluta(), 1e-9);
        assertEquals(DIA1.plusDays(2), resum.dataMaximaAbsoluta());
        assertEquals(10.0, resum.minimaAbsoluta(), 1e-9);
        assertEquals(DIA1, resum.dataMinimaAbsoluta());
    }

    @Test
    void comptaLaPlujaNomesSiSuperaElLlindar() {
        ResumEstadistic resum = analitzador.resumeix(mesures);

        assertEquals(5.5, resum.precipitacioTotal(), 1e-9);
        assertEquals(1, resum.diesDePluja());
    }

    @Test
    void laTendenciaEsUnGrauPerDia() {
        ResumEstadistic resum = analitzador.resumeix(mesures);

        assertEquals(1.0, resum.tendenciaPendentPerDia(), 1e-9);
        assertEquals(15.0, resum.tendencia(DIA1), 1e-9);
        assertEquals(17.0, resum.tendencia(DIA1.plusDays(2)), 1e-9);
    }

    @Test
    void senseMesuresLlancaExcepcio() {
        assertThrows(IllegalArgumentException.class, () -> analitzador.resumeix(List.of()));
    }
}
