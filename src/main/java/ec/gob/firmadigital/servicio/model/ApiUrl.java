package ec.gob.firmadigital.servicio.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;

/**
 * Representa el URL de un servidor API provisto por un tercero.
 *
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Entity
@NamedQuery(name = "ApiUrl.findByUrl", query = "SELECT a FROM ApiUrl a WHERE a.url LIKE :url")
public class ApiUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String url;

    public ApiUrl() {
    }

    public ApiUrl(String nombre, String url) {
        this.nombre = nombre;
        this.url = url;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "ApiUrl[id=" + id + ", nombre=" + nombre + ", url=" + url + "]";
    }
}