/*
Salcedo Alvarez Hugo Emmanuel
Morales Escobar Juan Adrian
 */
package back_end;

import Nodos.Arista;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Algoritmos de camino más corto sobre un GrafoPonderado:
 *  - Dijkstra (pesos no negativos, cola de prioridad)
 *  - Bellman-Ford (admite pesos negativos y detecta ciclos negativos)
 * Ambos reportan paso a paso (inicialización, relajación/aceptación y descarte)
 * a través del consumidor de traza recibido.
 */
public final class OptimizadorRutas {

    private static final double INF = Double.POSITIVE_INFINITY;

    private OptimizadorRutas() {
    }

    private record Entrada(String id, double costo, int orden) {
    }

    private static String f(double v) {
        return ResultadoRuta.fmt(v);
    }

    private static String validar(GrafoPonderado g, String id, String rol) {
        if (g == null || g.isEmpty()) {
            throw new IllegalStateException("El grafo está vacío.");
        }
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El nodo " + rol + " no puede estar vacío.");
        }
        String limpio = id.trim();
        if (g.buscarVertice(limpio) == null) {
            throw new IllegalArgumentException("El nodo " + rol + " '" + limpio + "' no existe en el grafo.");
        }
        return limpio;
    }

    /** Reconstruye "A -> B -> C" siguiendo los predecesores desde el nodo hasta el origen. */
    private static List<String> rutaHasta(Map<String, String> pred, String origen, String nodo) {
        List<String> ruta = new ArrayList<>();
        String actual = nodo;
        int limite = pred.size() + 2;
        while (actual != null && limite-- > 0) {
            ruta.add(actual);
            if (actual.equals(origen)) {
                break;
            }
            actual = pred.get(actual);
        }
        Collections.reverse(ruta);
        return ruta;
    }

    private static String txt(List<String> ruta) {
        return "[" + String.join(" -> ", ruta) + "]";
    }

    private static void inicializar(GrafoPonderado g, String o, Map<String, Double> dist, Consumer<String> t) {
        t.accept("FASE DE INICIALIZACIÓN");
        for (String id : g.getIds()) {
            dist.put(id, id.equals(o) ? 0.0 : INF);
            t.accept("  dist[" + id + "] = " + f(dist.get(id)) + (id.equals(o) ? "   (nodo origen)" : ""));
        }
        t.accept("");
    }

    private static void finalizar(ResultadoRuta r, Map<String, Double> dist, Map<String, String> pred, Consumer<String> t) {
        t.accept("");
        t.accept("RESULTADO FINAL (" + r.algoritmo + ")");
        double d = dist.get(r.destino);
        if (r.cicloNegativo) {
            r.alcanzable = false;
            t.accept("  Hay un ciclo de peso negativo: no existe una ruta mínima bien definida.");
        } else if (Double.isInfinite(d)) {
            r.alcanzable = false;
            t.accept("  El nodo '" + r.destino + "' es INACCESIBLE desde '" + r.origen + "': no existe ningún camino.");
        } else {
            r.alcanzable = true;
            r.costo = d;
            r.camino = rutaHasta(pred, r.origen, r.destino);
            t.accept("  Ruta más corta : " + String.join(" -> ", r.camino));
            t.accept("  Costo total    : " + f(d));
        }
        t.accept("  Evaluaciones: " + r.evaluaciones + " | Aceptadas: " + r.aceptaciones + " | Descartadas: " + r.descartes
                + (r.omitidas > 0 ? " | Omitidas: " + r.omitidas : "") + " | Pasos: " + r.pasos);
    }

    // ----------------------------------------------------------------- Dijkstra

    public static ResultadoRuta dijkstra(GrafoPonderado g, String origen, String destino, Consumer<String> traza) {
        Consumer<String> t = (traza == null) ? s -> { } : traza;
        String o = validar(g, origen, "origen");
        String d = validar(g, destino, "destino");
        for (Arista a : g.getAristas()) {
            if (a.peso() < 0) {
                throw new IllegalArgumentException("Dijkstra no admite pesos negativos (ruta " + a.origen() + " -> "
                        + a.destino() + " = " + f(a.peso()) + "). Use Bellman-Ford.");
            }
        }
        ResultadoRuta r = new ResultadoRuta("Dijkstra", o, d);
        long inicio = System.nanoTime();

        t.accept("========== ALGORITMO DE DIJKSTRA: " + o + " -> " + d + " ==========");
        Map<String, Double> dist = new LinkedHashMap<>();
        Map<String, String> pred = new HashMap<>();
        Set<String> fijados = new HashSet<>();
        inicializar(g, o, dist, t);

        PriorityQueue<Entrada> cola = new PriorityQueue<>((x, y) -> {
            int c = Double.compare(x.costo(), y.costo());
            return c != 0 ? c : Integer.compare(x.orden(), y.orden());
        });
        int orden = 0;
        cola.add(new Entrada(o, 0.0, orden++));

        while (!cola.isEmpty()) {
            Entrada e = cola.poll();
            String u = e.id();
            if (fijados.contains(u)) {
                t.accept("  (entrada obsoleta de '" + u + "' con costo " + f(e.costo()) + " ignorada: ya está fijado)");
                continue;
            }
            fijados.add(u);
            r.pasos++;
            t.accept("PASO " + r.pasos + ": se extrae '" + u + "' de la cola de prioridad (costo acumulado = " + f(dist.get(u))
                    + ") -> nodo fijado");
            if (u.equals(d)) {
                t.accept("  Se extrajo el destino: su costo ya es definitivo, el algoritmo se detiene.");
                break;
            }
            Map<String, Double> vecinos = g.getVecinos(u);
            if (vecinos.isEmpty()) {
                t.accept("  '" + u + "' no tiene rutas de salida.");
            }
            for (Map.Entry<String, Double> ent : vecinos.entrySet()) {
                String v = ent.getKey();
                double w = ent.getValue();
                double nuevo = dist.get(u) + w;
                double actual = dist.get(v);
                String arista = u + " -> " + v + " (peso " + f(w) + ")";
                r.evaluaciones++;
                if (fijados.contains(v)) {
                    r.descartes++;
                    t.accept("  DESCARTADA " + arista + ": '" + v + "' ya está fijado con costo " + f(actual)
                            + "; la alternativa costaría " + f(nuevo));
                } else if (nuevo < actual) {
                    r.aceptaciones++;
                    t.accept("  ACEPTADA   " + arista + ": dist[" + u + "] + peso = " + f(dist.get(u)) + " + " + f(w) + " = "
                            + f(nuevo) + " < dist[" + v + "] = " + f(actual) + " -> dist[" + v + "] = " + f(nuevo)
                            + ", predecesor = " + u);
                    if (!Double.isInfinite(actual)) {
                        List<String> alterna = new ArrayList<>(rutaHasta(pred, o, v));
                        t.accept("             ruta anterior descartada: " + txt(alterna) + " (costo " + f(actual) + ")");
                    }
                    dist.put(v, nuevo);
                    pred.put(v, u);
                    cola.add(new Entrada(v, nuevo, orden++));
                } else {
                    r.descartes++;
                    List<String> alterna = new ArrayList<>(rutaHasta(pred, o, u));
                    alterna.add(v);
                    t.accept("  DESCARTADA " + arista + ": " + f(dist.get(u)) + " + " + f(w) + " = " + f(nuevo)
                            + " >= dist[" + v + "] = " + f(actual) + " -> se conserva " + txt(rutaHasta(pred, o, v))
                            + "; alternativa descartada " + txt(alterna));
                }
            }
        }
        finalizar(r, dist, pred, t);
        r.nanos = System.nanoTime() - inicio;
        return r;
    }

    // ------------------------------------------------------------- Bellman-Ford

    public static ResultadoRuta bellmanFord(GrafoPonderado g, String origen, String destino, Consumer<String> traza) {
        Consumer<String> t = (traza == null) ? s -> { } : traza;
        String o = validar(g, origen, "origen");
        String d = validar(g, destino, "destino");
        ResultadoRuta r = new ResultadoRuta("Bellman-Ford", o, d);
        long inicio = System.nanoTime();

        t.accept("========== ALGORITMO DE BELLMAN-FORD: " + o + " -> " + d + " ==========");
        Map<String, Double> dist = new LinkedHashMap<>();
        Map<String, String> pred = new HashMap<>();
        inicializar(g, o, dist, t);

        List<Arista> arcos = g.getAristas();
        int n = g.getNumVertices();
        t.accept("Se repetirán hasta V-1 = " + (n - 1) + " iteraciones, revisando las " + arcos.size() + " rutas dirigidas en cada una.");
        for (int i = 1; i <= n - 1; i++) {
            r.pasos++;
            t.accept("");
            t.accept("ITERACIÓN " + i + " de " + (n - 1));
            boolean cambio = false;
            for (Arista a : arcos) {
                String u = a.origen();
                String v = a.destino();
                double w = a.peso();
                String arista = u + " -> " + v + " (peso " + f(w) + ")";
                if (Double.isInfinite(dist.get(u))) {
                    r.omitidas++;
                    t.accept("  OMITIDA    " + arista + ": dist[" + u + "] = ∞, '" + u + "' aún no es alcanzable");
                    continue;
                }
                double nuevo = dist.get(u) + w;
                double actual = dist.get(v);
                r.evaluaciones++;
                if (nuevo < actual) {
                    r.aceptaciones++;
                    cambio = true;
                    t.accept("  ACEPTADA   " + arista + ": dist[" + u + "] + peso = " + f(dist.get(u)) + " + " + f(w) + " = "
                            + f(nuevo) + " < dist[" + v + "] = " + f(actual) + " -> dist[" + v + "] = " + f(nuevo)
                            + ", predecesor = " + u);
                    if (!Double.isInfinite(actual)) {
                        t.accept("             ruta anterior descartada: " + txt(rutaHasta(pred, o, v)) + " (costo " + f(actual) + ")");
                    }
                    dist.put(v, nuevo);
                    pred.put(v, u);
                } else {
                    r.descartes++;
                    List<String> alterna = new ArrayList<>(rutaHasta(pred, o, u));
                    alterna.add(v);
                    t.accept("  DESCARTADA " + arista + ": " + f(dist.get(u)) + " + " + f(w) + " = " + f(nuevo)
                            + " >= dist[" + v + "] = " + f(actual) + " -> se conserva " + txt(rutaHasta(pred, o, v))
                            + "; alternativa descartada " + txt(alterna));
                }
            }
            if (!cambio) {
                t.accept("  Ninguna distancia cambió en esta iteración: el algoritmo converge y se detiene antes de tiempo.");
                break;
            }
        }

        t.accept("");
        t.accept("VERIFICACIÓN DE CICLOS NEGATIVOS");
        for (Arista a : arcos) {
            double du = dist.get(a.origen());
            if (!Double.isInfinite(du) && du + a.peso() < dist.get(a.destino())) {
                r.cicloNegativo = true;
                t.accept("  CICLO NEGATIVO detectado: la ruta " + a.origen() + " -> " + a.destino() + " todavía mejora el costo de '"
                        + a.destino() + "' tras V-1 iteraciones.");
                break;
            }
        }
        if (!r.cicloNegativo) {
            t.accept("  Ninguna ruta mejora tras V-1 iteraciones: no hay ciclos de peso negativo.");
        }
        finalizar(r, dist, pred, t);
        r.nanos = System.nanoTime() - inicio;
        return r;
    }

    // ---------------------------------------------------------------- comparativa

    public static String comparativa(ResultadoRuta a, ResultadoRuta b) {
        StringBuilder sb = new StringBuilder();
        sb.append("========== COMPARATIVA DE DESEMPEÑO: ").append(a.origen).append(" -> ").append(a.destino).append(" ==========\n");
        sb.append(String.format("%-14s %-10s %-12s %-10s %-11s %-9s %-6s %s%n",
                "Algoritmo", "Costo", "Evaluaciones", "Aceptadas", "Descartadas", "Omitidas", "Pasos", "Tiempo(µs)"));
        for (ResultadoRuta r : new ResultadoRuta[]{a, b}) {
            sb.append(String.format("%-14s %-10s %-12d %-10d %-11d %-9d %-6d %d%n", r.algoritmo,
                    r.alcanzable ? ResultadoRuta.fmt(r.costo) : "sin camino", r.evaluaciones, r.aceptaciones,
                    r.descartes, r.omitidas, r.pasos, r.nanos / 1000));
        }
        sb.append("\n");
        if (!a.alcanzable && !b.alcanzable) {
            sb.append("Ambos algoritmos coinciden: '").append(a.destino).append("' es inaccesible desde '").append(a.origen).append("'.\n");
        } else if (a.alcanzable && b.alcanzable && Math.abs(a.costo - b.costo) < 1e-9) {
            sb.append("Costos iguales: SÍ (").append(ResultadoRuta.fmt(a.costo)).append("). Ambos algoritmos llegan al mismo costo óptimo.\n");
            sb.append("Ruta ").append(a.algoritmo).append(": ").append(a.getCaminoTexto()).append("\n");
            sb.append("Ruta ").append(b.algoritmo).append(": ").append(b.getCaminoTexto()).append("\n");
        } else {
            sb.append("Costos iguales: NO. Los algoritmos difieren (revise la traza).\n");
        }
        return sb.toString();
    }
}
