/*
Salcedo Alvarez Hugo Emmanuel
Morales Escobar Juan Adrian
 */
package back_end;

import Nodos.Arista;
import Nodos.Vertice;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Grafo ponderado implementado con lista de adyacencia.
 * Internamente todas las conexiones son arcos dirigidos; una ruta no dirigida
 * se guarda como dos arcos (origen->destino y destino->origen) con el mismo peso.
 * Los recorridos visitan los vecinos en el orden en que se crearon las rutas.
 */
public class GrafoPonderado {

    public enum Conectividad {
        FUERTEMENTE_CONEXO("Fuertemente conectado: desde cualquier ubicación se puede llegar a todas las demás."),
        DEBILMENTE_CONEXO("Débilmente conectado: ignorando el sentido de las rutas todas las ubicaciones se alcanzan, "
                + "pero no desde cualquiera se llega a todas."),
        DESCONECTADO("Desconectado: existen ubicaciones que no se pueden alcanzar ni ignorando el sentido de las rutas.");

        private final String descripcion;

        Conectividad(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    private final Map<String, Vertice> vertices = new LinkedHashMap<>();
    private final Map<String, Map<String, Double>> adyacencia = new LinkedHashMap<>();

    // ------------------------------------------------------------ validaciones

    /** El peso debe ser numérico, finito y positivo. */
    public static double validarPeso(double peso) {
        if (Double.isNaN(peso) || Double.isInfinite(peso) || peso <= 0) {
            throw new IllegalArgumentException("El peso debe ser un número finito mayor a cero.");
        }
        return peso;
    }

    private void exigirNoVacio() {
        if (isEmpty()) {
            throw new IllegalStateException("El grafo está vacío.");
        }
    }

    private String exigirExistente(String id, String rol) {
        String limpio = Vertice.validarId(id);
        if (!vertices.containsKey(limpio)) {
            throw new IllegalArgumentException("No existe " + rol + " con identificador '" + limpio + "'.");
        }
        return limpio;
    }

    // ---------------------------------------------------------------- vértices

    public void agregarVertice(Vertice v) {
        if (v == null) {
            throw new IllegalArgumentException("La ubicación no puede ser nula.");
        }
        if (vertices.containsKey(v.getId())) {
            throw new IllegalArgumentException("Ya existe una ubicación con el identificador '" + v.getId() + "'.");
        }
        vertices.put(v.getId(), v);
        adyacencia.put(v.getId(), new LinkedHashMap<>());
    }

    public void actualizarVertice(String id, String nombre, String tipo, String estado, double capacidad) {
        exigirNoVacio();
        Vertice v = vertices.get(exigirExistente(id, "una ubicación"));
        // se validan todos los datos antes de modificar para no dejar el vértice a medias
        Vertice prueba = new Vertice(v.getId(), nombre, tipo, estado, capacidad);
        v.setNombre(prueba.getNombre());
        v.setTipo(prueba.getTipo());
        v.setEstado(prueba.getEstado());
        v.setCapacidad(prueba.getCapacidad());
    }

    /** Devuelve la ubicación o null si no existe (o si el grafo está vacío). */
    public Vertice buscarVertice(String id) {
        String limpio = Vertice.validarId(id);
        return vertices.get(limpio);
    }

    /** Elimina la ubicación y, en cascada, todas las rutas que salen o llegan a ella. Devuelve cuántas rutas (arcos) se quitaron. */
    public int eliminarVertice(String id) {
        exigirNoVacio();
        String limpio = exigirExistente(id, "una ubicación");
        int quitadas = adyacencia.get(limpio).size();
        adyacencia.remove(limpio);
        for (Map<String, Double> vecinos : adyacencia.values()) {
            if (vecinos.remove(limpio) != null) {
                quitadas++;
            }
        }
        vertices.remove(limpio);
        return quitadas;
    }

    // ----------------------------------------------------------------- aristas

    /**
     * Crea (o actualiza el peso de) la ruta entre dos ubicaciones existentes.
     * @return true si la ruta ya existía y solo se actualizó su peso
     */
    public boolean agregarArista(String origen, String destino, double peso, boolean dirigida) {
        exigirNoVacio();
        String o = exigirExistente(origen, "una ubicación de origen");
        String d = exigirExistente(destino, "una ubicación de destino");
        if (o.equals(d)) {
            throw new IllegalArgumentException("El origen y el destino no pueden ser la misma ubicación.");
        }
        validarPeso(peso);
        boolean existia = adyacencia.get(o).containsKey(d);
        adyacencia.get(o).put(d, peso);
        if (!dirigida) {
            existia = existia || adyacencia.get(d).containsKey(o);
            adyacencia.get(d).put(o, peso);
        }
        return existia;
    }

    /** Quita la ruta origen->destino (y destino->origen si no es dirigida). Devuelve cuántos arcos se quitaron. */
    public int eliminarArista(String origen, String destino, boolean dirigida) {
        exigirNoVacio();
        String o = exigirExistente(origen, "una ubicación de origen");
        String d = exigirExistente(destino, "una ubicación de destino");
        int quitadas = 0;
        if (adyacencia.get(o).remove(d) != null) {
            quitadas++;
        }
        if (!dirigida && adyacencia.get(d).remove(o) != null) {
            quitadas++;
        }
        if (quitadas == 0) {
            throw new IllegalArgumentException("No existe una ruta entre '" + o + "' y '" + d + "'"
                    + (dirigida ? " en ese sentido." : "."));
        }
        return quitadas;
    }

    // -------------------------------------------------------------- recorridos

    public List<String> bfs(String inicio) {
        exigirNoVacio();
        String raiz = exigirExistente(inicio, "una ubicación inicial");
        List<String> orden = new ArrayList<>();
        Set<String> visitados = new LinkedHashSet<>();
        Deque<String> cola = new ArrayDeque<>();
        visitados.add(raiz);
        cola.add(raiz);
        while (!cola.isEmpty()) {
            String actual = cola.poll();
            orden.add(actual);
            for (String vecino : adyacencia.get(actual).keySet()) {
                if (visitados.add(vecino)) {
                    cola.add(vecino);
                }
            }
        }
        return orden;
    }

    public List<String> dfs(String inicio) {
        exigirNoVacio();
        String raiz = exigirExistente(inicio, "una ubicación inicial");
        List<String> orden = new ArrayList<>();
        dfs(raiz, new LinkedHashSet<>(), orden);
        return orden;
    }

    private void dfs(String actual, Set<String> visitados, List<String> orden) {
        visitados.add(actual);
        orden.add(actual);
        for (String vecino : adyacencia.get(actual).keySet()) {
            if (!visitados.contains(vecino)) {
                dfs(vecino, visitados, orden);
            }
        }
    }

    // ------------------------------------------------- matriz / lista / conectividad

    public List<String> getIds() {
        return new ArrayList<>(vertices.keySet());
    }

    /** Matriz de pesos en el orden de getIds(); 0 significa que no hay ruta. */
    public double[][] matrizAdyacencia() {
        exigirNoVacio();
        List<String> ids = getIds();
        double[][] m = new double[ids.size()][ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            for (int j = 0; j < ids.size(); j++) {
                Double peso = adyacencia.get(ids.get(i)).get(ids.get(j));
                m[i][j] = (peso == null) ? 0 : peso;
            }
        }
        return m;
    }

    public String matrizComoTexto() {
        double[][] m = matrizAdyacencia();
        List<String> ids = getIds();
        int ancho = 9;
        for (String id : ids) {
            ancho = Math.max(ancho, id.length() + 2);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-" + ancho + "s", "")).append("  ");
        for (String id : ids) {
            sb.append(String.format("%" + ancho + "s", id));
        }
        sb.append("\n");
        for (int i = 0; i < ids.size(); i++) {
            sb.append(String.format("%-" + ancho + "s", ids.get(i))).append("  ");
            for (int j = 0; j < ids.size(); j++) {
                String celda = (m[i][j] == 0) ? "-" : String.format(Locale.US, "%.2f", m[i][j]);
                sb.append(String.format("%" + ancho + "s", celda));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public String listaAdyacenciaComoTexto() {
        exigirNoVacio();
        StringBuilder sb = new StringBuilder();
        for (String id : vertices.keySet()) {
            sb.append(id).append(" -> ");
            Map<String, Double> vecinos = adyacencia.get(id);
            if (vecinos.isEmpty()) {
                sb.append("(sin rutas de salida)");
            } else {
                boolean primero = true;
                for (Map.Entry<String, Double> e : vecinos.entrySet()) {
                    if (!primero) {
                        sb.append(", ");
                    }
                    sb.append(e.getKey()).append("(").append(String.format(Locale.US, "%.2f", e.getValue())).append(")");
                    primero = false;
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public Conectividad evaluarConectividad() {
        exigirNoVacio();
        String primero = vertices.keySet().iterator().next();
        boolean fuerte = alcanzados(primero, false).size() == vertices.size()
                && alcanzados(primero, true).size() == vertices.size();
        if (fuerte) {
            return Conectividad.FUERTEMENTE_CONEXO;
        }
        // débil: se ignora el sentido (se recorre hacia adelante y hacia atrás a la vez)
        Set<String> visitados = new LinkedHashSet<>();
        Deque<String> pila = new ArrayDeque<>();
        visitados.add(primero);
        pila.push(primero);
        while (!pila.isEmpty()) {
            String actual = pila.pop();
            for (String v : adyacencia.get(actual).keySet()) {
                if (visitados.add(v)) {
                    pila.push(v);
                }
            }
            for (Map.Entry<String, Map<String, Double>> e : adyacencia.entrySet()) {
                if (e.getValue().containsKey(actual) && visitados.add(e.getKey())) {
                    pila.push(e.getKey());
                }
            }
        }
        return visitados.size() == vertices.size() ? Conectividad.DEBILMENTE_CONEXO : Conectividad.DESCONECTADO;
    }

    /** Ubicaciones alcanzables desde "inicio" siguiendo (o invirtiendo) el sentido de las rutas. */
    private Set<String> alcanzados(String inicio, boolean invertido) {
        Set<String> visitados = new LinkedHashSet<>();
        Deque<String> pila = new ArrayDeque<>();
        visitados.add(inicio);
        pila.push(inicio);
        while (!pila.isEmpty()) {
            String actual = pila.pop();
            if (!invertido) {
                for (String v : adyacencia.get(actual).keySet()) {
                    if (visitados.add(v)) {
                        pila.push(v);
                    }
                }
            } else {
                for (Map.Entry<String, Map<String, Double>> e : adyacencia.entrySet()) {
                    if (e.getValue().containsKey(actual) && visitados.add(e.getKey())) {
                        pila.push(e.getKey());
                    }
                }
            }
        }
        return visitados;
    }

    // --------------------------------------------------------------- consultas

    /** Rutas de salida de una ubicación (vecino -> peso), en el orden en que se crearon. */
    public Map<String, Double> getVecinos(String id) {
        return Collections.unmodifiableMap(adyacencia.get(exigirExistente(id, "una ubicación")));
    }

    public int gradoSalida(String id) {
        return adyacencia.get(exigirExistente(id, "una ubicación")).size();
    }

    public int gradoEntrada(String id) {
        String limpio = exigirExistente(id, "una ubicación");
        int n = 0;
        for (Map<String, Double> vecinos : adyacencia.values()) {
            if (vecinos.containsKey(limpio)) {
                n++;
            }
        }
        return n;
    }

    public List<Vertice> getVertices() {
        return new ArrayList<>(vertices.values());
    }

    /** Todos los arcos dirigidos (una ruta no dirigida aparece como dos arcos). */
    public List<Arista> getAristas() {
        List<Arista> lista = new ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> e : adyacencia.entrySet()) {
            for (Map.Entry<String, Double> v : e.getValue().entrySet()) {
                lista.add(new Arista(e.getKey(), v.getKey(), v.getValue()));
            }
        }
        return lista;
    }

    public int getNumVertices() {
        return vertices.size();
    }

    public int getNumArcos() {
        int n = 0;
        for (Map<String, Double> vecinos : adyacencia.values()) {
            n += vecinos.size();
        }
        return n;
    }

    public boolean isEmpty() {
        return vertices.isEmpty();
    }

    public void destruir() {
        vertices.clear();
        adyacencia.clear();
    }
}
