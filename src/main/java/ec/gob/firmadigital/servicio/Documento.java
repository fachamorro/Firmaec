/*
 * Firma Digital: Servicio
 * Copyright (C) 2017 Secretaría Nacional de la Administración Pública
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ec.gob.firmadigital.servicio;

import java.io.Serializable;
import java.util.Base64;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

/**
 * Representa un documento almacenado en la base de datos.
 * 
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Entity
public class Documento implements Serializable {

    private static final long serialVersionUID = -8995191478984459392L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /** Contenido del documento, representado en Base64 */
    private String contenido;

    public Documento() {
    }

    public Documento(String contenido) {
        this.contenido = contenido;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    @Transient
    public byte[] getArchivo() {
        return Base64.getDecoder().decode(contenido);
    }
}