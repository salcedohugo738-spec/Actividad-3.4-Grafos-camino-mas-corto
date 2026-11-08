/*
Salcedo Alvarez Hugo Emmanuel
Morales Escobar Juan Adrian
 */
package Nodos;

import java.util.Locale;

/** Ubicación (vértice) de la red: terminal, almacén, centro de distribución, cliente, etc. */
public class Vertice {

    private final String id;
    private String nombre;
    private String tipo;
    private String estado;
    private double capacidad;

    public Vertice(String id, String nombre, String tipo, String estado, double capacidad) {
        this.id = validarId(id);
        setNombre(nombre);
        setTipo(tipo);
        setEstado(estado);
        setCapacidad(capacidad);
    }

    /** El identificador debe ser alfanumérico (se permiten guion y guion bajo) y no vacío. */
    public static String validarId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El identificador no puede estar vacío.");
        }
        String limpio = id.trim();
        if (!limpio.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("El identificador '" + limpio
                    + "' no es válido: use solo letras, números, guion o guion bajo.");
        }
        return limpio;
    }

    private static String texto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo '" + campo + "' no puede estar vacío.");
        }
        return valor.trim();
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = texto(nombre, "Nombre");
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = texto(tipo, "Tipo");
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = texto(estado, "Estado");
    }

    public double getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(double capacidad) {
        if (Double.isNaN(capacidad) || Double.isInfinite(capacidad) || capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser un número finito mayor a cero.");
        }
        this.capacidad = capacidad;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "[%s] %s | Tipo: %s | Estado: %s | Capacidad: %.2f",
                id, nombre, tipo, estado, capacidad);
    }
}
