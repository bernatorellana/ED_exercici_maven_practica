package edu.meteo.dades;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Converteix les respostes JSON de l'API d'Open-Meteo en objectes Java.
 * Utilitza Jackson.
 */
public class ParserOpenMeteo {

    private static final Logger log = LoggerFactory.getLogger(ParserOpenMeteo.class);

    private final ObjectMapper mapper = new ObjectMapper()
            // Sense aquest mòdul, Jackson no sap convertir "2026-08-14" en un LocalDate
            .registerModule(new JavaTimeModule())
            // El JSON porta molts camps que no ens interessen: els ignorem
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // ---- Estructura del JSON de l'API de geocodificació ----
    // {"results":[{"name":"Girona","latitude":41.98,"longitude":2.82,"country":"Espanya","admin1":"Catalunya",...}]}

    record RespostaGeocodificacio(List<Resultat> results) {
    }

    record Resultat(String name, String admin1, String country, double latitude, double longitude) {
    }

    // ---- Estructura del JSON de l'API d'històric ----
    // {"daily":{"time":["2026-08-14",...],"temperature_2m_max":[29.8,...],...}}

    record RespostaHistoric(Diari daily) {
    }

    record Diari(
            List<LocalDate> time,
            @JsonProperty("temperature_2m_max") List<Double> maximes,
            @JsonProperty("temperature_2m_min") List<Double> minimes,
            @JsonProperty("precipitation_sum") List<Double> precipitacions) {
    }

    /** Retorna la primera ubicació trobada, o buit si la cerca no ha donat cap resultat. */
    public Optional<Ubicacio> parseUbicacio(String json) throws JsonProcessingException {
        RespostaGeocodificacio resposta = mapper.readValue(json, RespostaGeocodificacio.class);
        if (resposta.results() == null || resposta.results().isEmpty()) {
            return Optional.empty();
        }
        Resultat r = resposta.results().get(0);
        return Optional.of(new Ubicacio(r.name(), r.admin1(), r.country(), r.latitude(), r.longitude()));
    }

    /**
     * Retorna les mesures diàries. Els dies on falta alguna dada (l'API hi posa null,
     * per exemple als dies més recents) es descarten.
     */
    public List<MesuraDiaria> parseHistoric(String json) throws JsonProcessingException {
        Diari diari = mapper.readValue(json, RespostaHistoric.class).daily();
        if (diari == null || diari.time() == null) {
            throw new IllegalArgumentException("La resposta no conté dades diàries");
        }

        List<MesuraDiaria> mesures = new ArrayList<>();
        int descartats = 0;
        for (int i = 0; i < diari.time().size(); i++) {
            Double max = diari.maximes().get(i);
            Double min = diari.minimes().get(i);
            Double pluja = diari.precipitacions().get(i);
            if (max == null || min == null || pluja == null) {
                descartats++;
            } else {
                mesures.add(new MesuraDiaria(diari.time().get(i), max, min, pluja));
            }
        }
        if (descartats > 0) {
            log.warn("S'han descartat {} dies sense dades completes", descartats);
        }
        return mesures;
    }
}
