/*
 * Firma Digital: Servicio
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
package ec.gob.firmadigital.servicio.rest;

import ec.gob.firmadigital.servicio.ServicioValidarCertificadoDigital;
import java.io.StringReader;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

/**
 * REST Web Service
 *
 * @author Christian Espinosa <christian.espinosa@mintel.gob.ec>, Misael Fernández
 */
@Stateless
@Path("/validarcertificadodigital")
public class ServicioValidarCertificadoDigitalRest {

    @EJB
    private ServicioValidarCertificadoDigital servicioValidarCertificadoDigital;

    @GET
    @Path("{json}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String validar(@PathParam("json") String jsonParameter) {

//        Decodificar String base 64
//        byte[] decodedBytes = Base64.getDecoder().decode(jsonParameter);
//        String decodedString = new String(decodedBytes);
//        jsonParameter=decodedString;
        
        if (jsonParameter == null || jsonParameter.isEmpty()) {
            return "Se debe incluir JSON con los parámetros: pkcs12, password";
        }
        
        JsonReader jsonReader = javax.json.Json.createReader(new StringReader(jsonParameter));
        JsonObject json;

        try {
            json = (JsonObject) jsonReader.read();
        } catch (JsonParsingException e) {
            return getClass().getSimpleName() + "::Error al decodificar JSON: \"" + e.getMessage();
        }

        String pkcs12;
        String password;

        try {
            pkcs12 = json.getString("pkcs12");
        } catch (NullPointerException e) {
            return getClass().getSimpleName() + "::Error al decodificar JSON: Se debe incluir \"pkcs12\"";
        }
        try {
            password = json.getString("password");
        } catch (NullPointerException e) {
            return getClass().getSimpleName() + "::Error al decodificar JSON: Se debe incluir \"password\"";
        }

        try {
            return servicioValidarCertificadoDigital.validarCertificadoDigital(pkcs12, password);
        } catch (Exception e) {
            return "Problemas en libreria";
        }
    }

}
