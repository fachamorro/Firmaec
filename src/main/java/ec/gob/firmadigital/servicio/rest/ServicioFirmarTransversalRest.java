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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import ec.gob.firmadigital.servicio.ServicioFirmarTransversal;
import java.io.StringReader;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

/**
 * REST Web Service
 *
 * @author Christian Espinosa <christian.espinosa@mintel.gob.ec>, Misael
 * Fernández
 */
@Stateless
@Path("/firmartransversal")
public class ServicioFirmarTransversalRest {

    @EJB
    private ServicioFirmarTransversal servicioFirmarTransversal;

    @GET
    @Path("{json}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String validar(@PathParam("json") String jsonParameter) throws Exception {
        if (jsonParameter == null || jsonParameter.isEmpty()) {
            return "Se debe incluir JSON con los parámetros: pkcs12, password";
        }
        JsonObject jsonObject;
        try {
            jsonObject = new JsonParser().parse(jsonParameter).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return getClass().getSimpleName() + "::Error al decodificar JSON: \"" + e.getMessage();
        }

        String pkcs12 = null;
        String password = null;
        String sistema = null;
        String operacion = null;
        String url = null;
        String versionFirmaEC = null;
        String formatoDocumento = null;
        String tokenJwt = null;
        String llx = null;
        String lly = null;
        String pagina = null;
        String tipoEstampado = null;
        String razon = null;
        boolean pre = false;
        boolean des = false;

        try {
            pkcs12 = jsonObject.get("pkcs12").getAsString();
        } catch (NullPointerException npe) {
            return "Error al decodificar JSON: Se debe incluir \"pkcs12\"";
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"pkcs12\"";
        }
        try {
            password = jsonObject.get("password").getAsString();
        } catch (NullPointerException npe) {
            return "Error al decodificar JSON: Se debe incluir \"password\"";
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"password\"";
        }
        try {
            sistema = jsonObject.get("sistema").getAsString();
        } catch (NullPointerException npe) {
            return "Error al decodificar JSON: Se debe incluir \"sistema\"";
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"sistema\"";
        }
        try {
            operacion = jsonObject.get("operacion").getAsString();
        } catch (NullPointerException npe) {
            return "Error al decodificar JSON: Se debe incluir \"operacion\"";
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"operacion\"";
        }
        try {
            if (jsonObject.get("url") != null) {
                url = jsonObject.get("url").getAsString();
            }
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"url\"";
        }
        try {
            versionFirmaEC = jsonObject.get("versionFirmaEC").getAsString();
        } catch (NullPointerException npe) {
            return "Error al decodificar JSON: Se debe incluir \"versionFirmaEC\"";
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"versionFirmaEC\"";
        }
        try {
            formatoDocumento = jsonObject.get("formatoDocumento").getAsString();
        } catch (NullPointerException npe) {
            formatoDocumento = "pdf";
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"formatoDocumento\"";
        }
        try {
            tokenJwt = jsonObject.get("tokenJwt").getAsString();
        } catch (NullPointerException npe) {
            return "Error al decodificar JSON: Se debe incluir \"tokenJwt\"";
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"tokenJwt\"";
        }
        try {
            if (jsonObject.get("llx") != null) {
                llx = jsonObject.get("llx").getAsString();
            }
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"llx\"";
        }
        try {
            if (jsonObject.get("lly") != null) {
                lly = jsonObject.get("lly").getAsString();
            }
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"jwt\"";
        }
        try {
            if (jsonObject.get("pagina") != null) {
                pagina = jsonObject.get("pagina").getAsString();
            }
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"pagina\"";
        }
        try {
            if (jsonObject.get("tipoEstampado") != null) {
                tipoEstampado = jsonObject.get("tipoEstampado").getAsString();
            }
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"tipoEstampado\"";
        }

        try {
            if (jsonObject.get("pre") != null) {
                pre = jsonObject.get("pre").getAsBoolean();
            }
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"pre\"";
        }
        try {
            if (jsonObject.get("des") != null) {
                des = jsonObject.get("des").getAsBoolean();
            }
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"des\"";
        }
        return servicioFirmarTransversal.firmarTransversal(pkcs12, password, sistema, operacion, url, versionFirmaEC, formatoDocumento, tokenJwt, llx, lly, pagina, tipoEstampado, razon, pre, des);
    }

}
