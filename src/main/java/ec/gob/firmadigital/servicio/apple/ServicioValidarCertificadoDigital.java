/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/WebServices/GenericResource.java to edit this template
 */
package ec.gob.firmadigital.servicio.apple;

import com.google.gson.Gson;
import ec.gob.firmadigital.servicio.util.CertificadoP12;
import ec.gob.firmadigital.servicio.util.ValidadorCertificadoDigital;
import io.rubrica.certificate.to.Certificado;
import io.rubrica.utils.Json;
import java.io.StringReader;
import java.util.Base64;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author Christian Espinosa <christian.espinosa@mintel.gob.ec>
 */
@Path("validarCertificadoDigital")
public class ServicioValidarCertificadoDigital {
    
    // Servicio REST interno
    private static final String REST_SERVICE_URL = "http://localhost:8080/servicio/validarCertificadoDigital";
    //    private static final String REST_SERVICE_URL = "https://wsfederada.firmadigital.gob.ec/servicio/validarCertificadoDigital";


    @Context
    private UriInfo context;

    /**
     * Creates a new instance of ServicioValidarCertificadoDigital
     */
    public ServicioValidarCertificadoDigital() {
    }

    /**
     * Retrieves representation of an instance of ec.gob.firmadigital.servicio.apple.ServicioValidarCertificadoDigital
     * @param jsonParameter
     * @return an instance of java.lang.String
     */
    @GET
    @Path("{json}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON) 
    public Response validar(@PathParam("json") String jsonParameter) {
        try {
            if (jsonParameter == null || jsonParameter.isEmpty()) {
                JsonObject errorResponse = javax.json.Json.createObjectBuilder().add("Error:", "Se debe incluir JSON con los parámetros: certificadoBase64, contrasena").build();
                return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
            }
            
            byte[] decodedBytes = Base64.getDecoder().decode(jsonParameter);
            String decodedString = new String(decodedBytes);
            jsonParameter=decodedString;
            
            //String certificadoBase64="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";
            //String contrasena="123456";
            String certificadoBase64;
            String contrasena;
            
            try {
                certificadoBase64=jsonParameter.replace("{", "").replace("}", "").replace("\"", "").split(",")[0].split(":")[1];
            } catch (NullPointerException e) {
                JsonObject errorResponse = javax.json.Json.createObjectBuilder().add("Error:", "Error al decodificar JSON , Se debe incluir certificadoBase64" ).build();
                return Response.ok(errorResponse, MediaType.APPLICATION_JSON).build();
            }
            
            try {
                contrasena=jsonParameter.replace("{", "").replace("}", "").replace("\"", "").split(",")[1].split(":")[1];
            } catch (NullPointerException e) {
                JsonObject errorResponse = javax.json.Json.createObjectBuilder().add("Error:", "Error al decodificar JSON , Se debe incluir contrasena" ).build();
                return Response.ok(errorResponse, MediaType.APPLICATION_JSON).build();
            }
            
             System.out.println("DATOS");
            System.out.println(certificadoBase64);
            System.out.println(contrasena);
            

            ValidadorCertificadoDigital validador = new ValidadorCertificadoDigital();
            Certificado certificado = validador.validarCertificado(certificadoBase64,contrasena);
            if (certificado != null) {
                 String respuesta=Json.generarJsonCertificado(certificado);
                 return Response.ok(respuesta, MediaType.APPLICATION_JSON).build();
            }

        } catch (Exception e) {
            JsonObject errorResponse = javax.json.Json.createObjectBuilder().add("Excepcion:", e.toString()).build();
            return Response.ok(errorResponse, MediaType.APPLICATION_JSON).build();
        }
        
        return null;
    }

}
