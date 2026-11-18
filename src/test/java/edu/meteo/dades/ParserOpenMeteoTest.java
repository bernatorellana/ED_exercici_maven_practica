package edu.meteo.dades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ParserOpenMeteoTest {

    private final ParserOpenMeteo parser = new ParserOpenMeteo();

    /** Llegeix un fitxer de src/test/resources. */
    static String recurs(String nom) throws IOException {
        try (InputStream in = ParserOpenMeteoTest.class.getResourceAsStream("/" + nom)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void llegeixLHistoricDescarregat() throws IOException {
        List<MesuraDiaria> mesures = parser.parseHistoric(recurs("historic-barcelona.json"));

        assertEquals(90, mesures.size());
        MesuraDiaria primera = mesures.get(0);
        assertEquals(LocalDate.of(2026, 8, 14), primera.data());
        assertTrue(primera.maxima() > primera.minima());
    }

    @Test
    void descartaElsDiesSenseDades() throws IOException {
        String json = """
                {"daily": {
                  "time": ["2026-11-16", "2026-11-17"],
                  "temperature_2m_max": [20.1, null],
                  "temperature_2m_min": [12.3, null],
                  "precipitation_sum": [0.0, null]
                }}""";

        List<MesuraDiaria> mesures = parser.parseHistoric(json);

        assertEquals(List.of(new MesuraDiaria(LocalDate.of(2026, 11, 16), 20.1, 12.3, 0.0)), mesures);
    }

    @Test
    void llegeixElResultatDeLaGeocodificacio() throws IOException {
        Optional<Ubicacio> ubicacio = parser.parseUbicacio(recurs("geocodificacio-girona.json"));

        assertTrue(ubicacio.isPresent());
        assertEquals("Girona", ubicacio.get().nom());
        assertEquals(41.98, ubicacio.get().latitud(), 0.01);
        assertEquals(2.82, ubicacio.get().longitud(), 0.01);
    }

    @Test
    void unaCercaSenseResultatsRetornaBuit() throws IOException {
        assertTrue(parser.parseUbicacio("{\"generationtime_ms\": 0.5}").isEmpty());
    }
}
