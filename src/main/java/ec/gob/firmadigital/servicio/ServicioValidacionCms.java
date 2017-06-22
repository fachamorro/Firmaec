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

import java.util.Base64;
import java.util.List;

import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import ec.gob.firmadigital.servicio.util.VerificadorCMS;
import io.rubrica.core.SignatureVerificationException;

@Stateless
@Path("/validacioncms")
public class ServicioValidacionCms {

    @POST
    public JsonObject verificarCms(String archivoBase64) throws SignatureVerificationException {
        byte[] pdf = Base64.getDecoder().decode(archivoBase64);

        VerificadorCMS verificador = new VerificadorCMS();
        byte[] pdfOriginal = verificador.verify(pdf);
        String documento = Base64.getEncoder().encodeToString(pdfOriginal);

        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        jsonBuilder.add("archivo", documento);

        // Para construir un array de firmantes
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        // FIXME
        List<DatosUsuario> listaDatosUsuario = verificador.listaDatosUsuario;

        for (DatosUsuario datosUsuario : listaDatosUsuario) {
            JsonObjectBuilder json = Json.createObjectBuilder();
            json.add("nombre", datosUsuario.getNombre());
            json.add("apellido", datosUsuario.getApellido());
            json.add("cargo", datosUsuario.getCargo());
            json.add("cedula", datosUsuario.getCedula());
            json.add("institucion", datosUsuario.getInstitucion());
            arrayBuilder.add(json);
        }

        jsonBuilder.add("firmantes", arrayBuilder.build());
        return jsonBuilder.build();
    }
}