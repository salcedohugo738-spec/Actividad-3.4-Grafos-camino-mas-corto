/*
Salcedo Alvarez Hugo Emmanuel
Morales Escobar Juan Adrian
 */
package back_end;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Resultado de ejecutar un algoritmo de camino más corto entre un origen y un destino. */
public class ResultadoRuta {

    public final String algoritmo;
    public final String origen;
    public final String destino;

    public boolean alcanzable;
    public boolean cicloNegativo;
    public List<String> camino = new ArrayList<>();
    public double costo = Double.POSITIVE_INFINITY;

    /** Aristas evaluadas (se calculó dist[u] + peso y se comparó con dist[v]). */
    public int evaluaciones;
    /** Evaluaciones en las que se aceptó un camino mejor (relajación). */
    public int aceptaciones;
    /** Evaluaciones en las que la alternativa se descartó por costar igual o más. */
    public int descartes;
    /** Aristas omitidas por no haberse alcanzado aún su nodo de partida (solo Bellman-Ford). */
    public int omitidas;
    /** Nodos extraídos de la cola (Dijkstra) o iteraciones sobre todas las aristas (Bellman-Ford). */
    public int pasos;
    public long nanos;

    public ResultadoRuta(String algoritmo, String origen, String destino) {
        this.algoritmo = algoritmo;
        this.origen = origen;
        this.destino = destino;
    }

    public static String fmt(double valor) {
        return Double.isInfinite(valor) ? "∞" : String.format(Locale.US, "%.2f", valor);
    }

    public String getCaminoTexto() {
        return String.join(" -> ", camino);
    }

    /** Texto para el área de resultado final. */
    public String resumen() {
        StringBuilder sb = new StringBuilder();
        sb.append("Algoritmo: ").append(algoritmo).append("\n");
        sb.append("Origen: ").append(origen).append("   Destino: ").append(destino).append("\n");
        if (cicloNegativo) {
            sb.append("Se detectó un ciclo de peso negativo: no existe ruta mínima definida.\n");
        } else if (!alcanzable) {
            sb.append("Sin camino: '").append(destino).append("' es inaccesible desde '").append(origen).append("'.\n");
        } else {
            sb.append("Ruta más corta: ").append(getCaminoTexto()).append("\n");
            sb.append("Costo total: ").append(fmt(costo)).append("\n");
        }
        return sb.toString();
    }
}
