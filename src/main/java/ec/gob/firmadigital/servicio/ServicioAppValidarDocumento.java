/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/WebServices/GenericResource.java to edit this template
 */
package ec.gob.firmadigital.servicio;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.core.MediaType;

/**
 * REST Web Service
 *
 * @author barckl3y
 */
@Path("appvalidardocumento")
public class ServicioAppValidarDocumento {

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String validarDocumento(@FormParam("documentoPdf") String documentoPdf) throws Exception {

        if (documentoPdf == null || documentoPdf.isEmpty()) {
            return "Se debe incluir el parametro documentoPdf";
        }
         
        
        return "Se ha validado correctamente campeón";

//        return servicioAppFirmarDocumentoTransversal.firmarTransversal(pkcs12, password, sistema, operacion, url, versionFirmaEC, formatoDocumento, tokenJwt, llx, lly, pagina, tipoEstampado, razon, pre, des);
    }
}
