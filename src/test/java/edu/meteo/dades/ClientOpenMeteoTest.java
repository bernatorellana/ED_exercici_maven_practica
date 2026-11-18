package edu.meteo.dades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Prova el client sense connectar-se a Internet: MockWebServer fa de servidor fals
 * que respon el que nosaltres li diem.
 */
class ClientOpenMeteoTest {

    private MockWebServer servidor;
    private ClientOpenMeteo client;

    @BeforeEach
    void arrenca() throws IOException {
        servidor = new MockWebServer();
        servidor.start();
        client = new ClientOpenMeteo(
                servidor.url("/v1/search").toString(),
                servidor.url("/v1/archive").toString());
    }

    @AfterEach
    void atura() throws IOException {
        client.close();
        servidor.close();
    }

    @Test
    void cercaUnaPoblacio() throws Exception {
        servidor.enqueue(new MockResponse.Builder()
                .body(ParserOpenMeteoTest.recurs("geocodificacio-girona.json"))
                .build());

        Ubicacio ubicacio = client.cercaUbicacio("Girona");

        assertEquals("Girona", ubicacio.nom());
        RecordedRequest peticio = servidor.takeRequest();
        assertEquals("/v1/search", peticio.getUrl().encodedPath());
        assertEquals("Girona", peticio.getUrl().queryParameter("name"));
    }

    @Test
    void descarregaLHistoricAmbElsParametresCorrectes() throws Exception {
        servidor.enqueue(new MockResponse.Builder()
                .body(ParserOpenMeteoTest.recurs("historic-barcelona.json"))
                .build());
        Ubicacio barcelona = new Ubicacio("Barcelona", "Catalunya", "Espanya", 41.3888, 2.159);

        List<MesuraDiaria> mesures = client.descarregaHistoric(barcelona,
                LocalDate.of(2026, 8, 14), LocalDate.of(2026, 11, 11));

        assertEquals(90, mesures.size());
        RecordedRequest peticio = servidor.takeRequest();
        assertEquals("41.3888", peticio.getUrl().queryParameter("latitude"));
        assertEquals("2026-08-14", peticio.getUrl().queryParameter("start_date"));
        assertEquals("2026-11-11", peticio.getUrl().queryParameter("end_date"));
    }

    @Test
    void unErrorDelServidorEsConverteixEnIOException() {
        servidor.enqueue(new MockResponse.Builder()
                .code(429)
                .body("{\"error\":true,\"reason\":\"Daily API request limit exceeded.\"}")
                .build());

        IOException error = assertThrows(IOException.class, () -> client.cercaUbicacio("Girona"));
        assertTrue(error.getMessage().contains("429"));
    }

    @Test
    void unaPoblacioInexistentEsConverteixEnIOException() {
        servidor.enqueue(new MockResponse.Builder().body("{\"generationtime_ms\":0.3}").build());

        assertThrows(IOException.class, () -> client.cercaUbicacio("Xyzzyville"));
    }
}
