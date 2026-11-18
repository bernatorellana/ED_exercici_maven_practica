package edu.meteo.dades;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Descarrega dades de l'API pública d'Open-Meteo (https://open-meteo.com/).
 * Utilitza OkHttp per fer les peticions HTTP.
 */
public class ClientOpenMeteo implements AutoCloseable {

    public static final String URL_GEOCODIFICACIO = "https://geocoding-api.open-meteo.com/v1/search";
    public static final String URL_HISTORIC = "https://archive-api.open-meteo.com/v1/archive";

    private static final Logger log = LoggerFactory.getLogger(ClientOpenMeteo.class);

    private final OkHttpClient http;
    private final HttpUrl urlGeocodificacio;
    private final HttpUrl urlHistoric;
    private final ParserOpenMeteo parser = new ParserOpenMeteo();

    /** Client que fa servir les adreces reals d'Open-Meteo. */
    public ClientOpenMeteo() {
        this(URL_GEOCODIFICACIO, URL_HISTORIC);
    }

    /** Client amb adreces configurables: els tests el fan servir amb un servidor fals. */
    public ClientOpenMeteo(String urlGeocodificacio, String urlHistoric) {
        this.http = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(30))
                .build();
        this.urlGeocodificacio = HttpUrl.get(urlGeocodificacio);
        this.urlHistoric = HttpUrl.get(urlHistoric);
    }

    /** Busca les coordenades d'una població pel seu nom. */
    public Ubicacio cercaUbicacio(String nom) throws IOException {
        HttpUrl url = urlGeocodificacio.newBuilder()
                .addQueryParameter("name", nom)
                .addQueryParameter("count", "1")
                .addQueryParameter("language", "ca")
                .build();
        return parser.parseUbicacio(descarrega(url))
                .orElseThrow(() -> new IOException("No s'ha trobat cap població anomenada '" + nom + "'"));
    }

    /** Descarrega les temperatures i la precipitació diàries entre dues dates (incloses). */
    public List<MesuraDiaria> descarregaHistoric(Ubicacio ubicacio, LocalDate inici, LocalDate fi) throws IOException {
        HttpUrl url = urlHistoric.newBuilder()
                .addQueryParameter("latitude", String.valueOf(ubicacio.latitud()))
                .addQueryParameter("longitude", String.valueOf(ubicacio.longitud()))
                .addQueryParameter("start_date", inici.toString())
                .addQueryParameter("end_date", fi.toString())
                .addEncodedQueryParameter("daily", "temperature_2m_max,temperature_2m_min,precipitation_sum")
                .addQueryParameter("timezone", "auto")
                .build();
        List<MesuraDiaria> mesures = parser.parseHistoric(descarrega(url));
        log.info("S'han descarregat {} dies de dades", mesures.size());
        return mesures;
    }

    private String descarrega(HttpUrl url) throws IOException {
        log.info("Descarregant {}", url);
        Request peticio = new Request.Builder().url(url).build();
        try (Response resposta = http.newCall(peticio).execute()) {
            String cos = resposta.body().string();
            log.debug("Resposta {} amb {} caràcters", resposta.code(), cos.length());
            if (!resposta.isSuccessful()) {
                throw new IOException("El servidor ha respost amb l'error " + resposta.code() + ": " + cos);
            }
            return cos;
        }
    }

    /** Allibera els fils i les connexions que OkHttp manté obertes. */
    @Override
    public void close() {
        http.dispatcher().executorService().shutdown();
        http.connectionPool().evictAll();
    }
}
