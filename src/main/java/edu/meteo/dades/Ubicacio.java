package edu.meteo.dades;

/**
 * Un lloc del món amb les seves coordenades.
 *
 * @param nom      nom de la població
 * @param regio    regió o comunitat (pot ser null)
 * @param pais     país (pot ser null)
 * @param latitud  latitud en graus
 * @param longitud longitud en graus
 */
public record Ubicacio(String nom, String regio, String pais, double latitud, double longitud) {

    /** Per exemple: "Girona (Catalunya, Espanya)". */
    public String descripcio() {
        StringBuilder sb = new StringBuilder(nom);
        if (regio != null || pais != null) {
            sb.append(" (");
            if (regio != null) {
                sb.append(regio);
            }
            if (regio != null && pais != null) {
                sb.append(", ");
            }
            if (pais != null) {
                sb.append(pais);
            }
            sb.append(")");
        }
        return sb.toString();
    }
}
