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

package ec.gob.firmadigital.servicio.rest;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import ec.gob.firmadigital.servicio.ServicioDocumento;
import ec.gob.firmadigital.servicio.SistemaTransversalException;
import ec.gob.firmadigital.servicio.token.TokenExpiradoException;
import ec.gob.firmadigital.servicio.token.TokenInvalidoException;
import ec.gob.firmadigital.servicio.util.Base64InvalidoException;

/**
 * Servicio REST para almacenar, actualizar y obtener documentos desde los
 * sistemas transversales y comunicarse con aplicación api.firmadigital.gob.ec
 * 
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Stateless
@Path("/documentos")
public class ServicioDocumentoRest {

    @EJB
    private ServicioDocumento servicioDocumento;

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
    @Produces(MediaType.TEXT_PLAIN)
    public Response crearDocumentos(String jsonParameter) {
        JsonReader jsonReader = Json.createReader(new StringReader(jsonParameter));
        JsonObject json;

        try {
            json = (JsonObject) jsonReader.read();
        } catch (JsonParsingException e) {
            return Response.status(Status.BAD_REQUEST).entity("Error al decodificar JSON: \"" + e.getMessage() + "\"")
                    .build();
        }

        String cedula;
        String sistema;

        try {
            cedula = json.getString("cedula");
        } catch (NullPointerException e) {
            return Response.status(Status.BAD_REQUEST).entity("Error al decodificar JSON: Se debe incluir \"cedula\"")
                    .build();
        }

        try {
            sistema = json.getString("sistema");
        } catch (NullPointerException e) {
            return Response.status(Status.BAD_REQUEST).entity("Error al decodificar JSON: Se debe incluir \"sistema\"")
                    .build();
        }

        JsonArray array = json.getJsonArray("documentos");

        if (array == null) {
            return Response.status(Status.BAD_REQUEST)
                    .entity("Error al decodificar JSON: Se debe incluir \"documentos\"").build();
        }

        // Documentos a devolver
        Map<String, String> documentos = new HashMap<>();

        for (JsonObject documentoJson : array.getValuesAs(JsonObject.class)) {
            String nombre = documentoJson.getString("nombre");
            String documento = documentoJson.getString("documento");
            documentos.put(nombre, documento);
        }

        try {
            String token = servicioDocumento.crearDocumentos(cedula, sistema, documentos);
            return Response.status(Status.CREATED).entity(token).build();
        } catch (Base64InvalidoException e) {
            return Response.status(Status.BAD_REQUEST).entity("Error al decodificar Base64").build();
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerDocumentos(@PathParam("token") String token) {
        Map<Long, String> documentos;

        try {
            documentos = servicioDocumento.obtenerDocumentos(token);
        } catch (TokenInvalidoException e) {
            return Response.status(Status.BAD_REQUEST).entity("Token invalido").build();
        } catch (TokenExpiradoException e) {
            return Response.status(Status.BAD_REQUEST).entity("Token expirado").build();
        }

        JsonArrayBuilder array = Json.createArrayBuilder();

        for (Long id : documentos.keySet()) {
            String documento = documentos.get(id);
            array.add(Json.createObjectBuilder().add("id", id).add("documento", documento));
        }

        String json = Json.createObjectBuilder().add("documentos", array).build().toString();
        return Response.ok(json).build();
    }

    @PUT
    @Path("{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response actualizarDocumentos(@PathParam("token") String token, JsonObject json) {
        Map<Long, String> documentos = new HashMap<>();
        List<JsonObject> array = json.getJsonArray("documentos").getValuesAs(JsonObject.class);

        for (JsonObject documentoJson : array) {
            Integer id = documentoJson.getInt("id");
            String documento = documentoJson.getString("documento");
            documentos.put(id.longValue(), documento);
        }

        try {
            servicioDocumento.actualizarDocumentos(token, documentos);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (TokenInvalidoException e) {
            return Response.status(Status.BAD_REQUEST).entity("Token invalido").build();
        } catch (TokenExpiradoException e) {
            return Response.status(Status.BAD_REQUEST).entity("Token expirado").build();
        } catch (Base64InvalidoException e) {
            return Response.status(Status.BAD_REQUEST).entity("Base 64 invalido").build();
        } catch (SistemaTransversalException e) {
            return Response.status(Status.BAD_REQUEST)
                    .entity("Error al guardar documento en sistema transversal: " + e.getMessage()).build();
        }
    }
}