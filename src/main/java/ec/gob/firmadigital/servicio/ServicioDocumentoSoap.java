/*
 * Firma Digital: Servicio
 * Copyright 2017 Secretaría Nacional de la Administración Pública
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

import javax.ejb.EJB;
import javax.jws.WebMethod;
import javax.jws.WebService;

/**
 * Servicio REST para almacenar, actualizar y obtener documentos desde los
 * sistemas transversales y la aplicación en api.firmadigital.gob.ec
 * 
 * FIXME: Proteger este metodo con usuario/password para cada sistema
 * transversal
 * 
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@WebService
public class ServicioDocumentoSoap {

    @EJB
    private ServicioDocumento servicioDocumento;

    /**
     * Almacena un documento desde un Sistema Transversal.
     * 
     * @param cedula
     * @param archivoBase64
     * @return el token para poder buscar el documento
     */
    @WebMethod
    public String crearDocumento(String cedula, String sistema, String nombre, String archivoBase64) {
        try {
            return servicioDocumento.crearDocumento(cedula, sistema, nombre, archivoBase64);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error al decodificar Base64");
        }
    }

    /**
     * Obtiene un documento mediante un token.
     * 
     * @param token
     * @return el documento en Base64
     */
    @WebMethod
    public String obtenerDocumento(String token) {
        try {
            return servicioDocumento.obtenerDocumento(token);
        } catch (TokenInvalidoException e) {
            throw new RuntimeException("Token invalido");
        } catch (TokenExpiradoException e) {
            throw new RuntimeException("Token expirado");
        }
    }

    /**
     * 
     * @param token
     * @param archivoBase64
     * @return
     */
    @WebMethod
    public void actualizarDocumento(String token, String archivoBase64) {
        try {
            servicioDocumento.actualizarDocumento(token, archivoBase64);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error al decodificar Base64");
        } catch (TokenInvalidoException e) {
            throw new RuntimeException("Token invalido");
        } catch (TokenExpiradoException e) {
            throw new RuntimeException("Token expirado");
        }
    }
}