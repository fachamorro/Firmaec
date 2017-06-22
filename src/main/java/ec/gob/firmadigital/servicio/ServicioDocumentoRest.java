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

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Servicio REST para almacenar, actualizar y obtener documentos desde los
 * sistemas transversales y la aplicación en api.firmadigital.gob.ec
 * 
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Stateless
@Path("/documentos")
public class ServicioDocumentoRest {

    @EJB
    private ServicioDocumento servicioDocumento;

    /**
     * Almacena un documento desde un Sistema Transversal.
     * 
     * @param cedula
     * @param archivoBase64
     * @return el token para poder buscar el documento
     */
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String crearDocumento(@QueryParam("cedula") String cedula, @QueryParam("sistema") String sistema,
            @QueryParam("nombre") String nombre, String archivoBase64) {
        try {
            return servicioDocumento.crearDocumento(cedula, sistema, nombre, archivoBase64);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Error al decodificar Base64");
        }
    }

    /**
     * Almacena varios documentos desde un Sistema Transversal.
     * 
     * Ejemplo:
     * 
     * { "cedula":"12345678", "sistema":"quipux", "documentos":[
     * {"nombre":"Archivo1.pdf", "base64":"abc"} ] }
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String crearDocumentos(JsonObject json) {
        Map<String, String> documentos = new HashMap<>();

        String cedula = json.getString("cedula");
        String sistema = json.getString("sistema");
        List<JsonObject> documentosJson = json.getJsonArray("documentos").getValuesAs(JsonObject.class);

        for (JsonObject documentoJson : documentosJson) {
            String nombre = documentoJson.getString("nombre");
            String documento = documentoJson.getString("documento");
            documentos.put(nombre, documento);
        }

        try {
            return servicioDocumento.crearDocumentos(cedula, sistema, documentos);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Error al decodificar Base64");
        }
    }

    /**
     * Obtiene un documento mediante un token.
     * 
     * @param token
     * @return el documento en Base64
     */
    @GET
    @Path("{token}")
    @Produces(MediaType.TEXT_PLAIN)
    public String obtenerDocumento(@PathParam("token") String token) {
        try {
            return servicioDocumento.obtenerDocumento(token);
        } catch (TokenInvalidoException e) {
            throw new NotFoundException("Token invalido");
        } catch (TokenExpiradoException e) {
            throw new NotFoundException("Token expirado");
        }
    }

    /**
     * Obtiene un documento mediante un token.
     * 
     * @param token
     * @return el documento en Base64
     */
    @GET
    @Path("{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject obtenerDocumentos(@PathParam("token") String token) {
        Map<Long, String> documentos;

        try {
            documentos = servicioDocumento.obtenerDocumentos(token);
        } catch (TokenInvalidoException e) {
            throw new NotFoundException("Token invalido");
        } catch (TokenExpiradoException e) {
            throw new NotFoundException("Token expirado");
        }

        // Para construir un array de documentos
        JsonObjectBuilder builder = Json.createObjectBuilder();
        Set<Long> keySet = documentos.keySet();

        for (Long id : keySet) {
            String documento = documentos.get(id);
            builder.add("documentos", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder().add("id", id).add("documento", documento)));
        }

        return builder.build();
    }

    /**
     * 
     * @param token
     * @param archivoBase64
     * @return
     */
    @PUT
    @Path("{token}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response actualizarDocumento(@PathParam("token") String token, String archivoBase64) {
        try {
            servicioDocumento.actualizarDocumento(token, archivoBase64);
            return Response.status(Status.OK).build();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Error al decodificar Base64");
        } catch (TokenInvalidoException | TokenExpiradoException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    /**
     * 
     * @param token
     * @param archivoBase64
     * @return
     */
    @PUT
    @Path("{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    //DAMN
    public Response actualizarDocumentos(@PathParam("token") String token, JsonObject json) {
        System.out.println("JSON from SDR="+ json);
        
        
        Map<Long, String> documentos = new HashMap<>();
        List<JsonObject> documentosJson = json.getJsonArray("documentos").getValuesAs(JsonObject.class);

        for (JsonObject documentoJson : documentosJson) {
            Integer id = documentoJson.getInt("id");
            String documento = documentoJson.getString("documento");
            documentos.put((long) id, documento);
        }

        try {
            servicioDocumento.actualizarDocumentos(token, documentos);
            return Response.status(Status.OK).build();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Error al decodificar Base64");
        } catch (TokenInvalidoException | TokenExpiradoException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }
}