/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/WebServices/GenericResource.java to edit this template
 */
package ec.gob.firmadigital.servicio;

import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

/**
 * REST Web Service
 *
 * @author barckl3y
 */
@Stateless
@Path("appfirmardocumento")
public class ServicioAppFirmarDocumento {


    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String firmarDocumento(@FormParam("documentoPdf") String documentoPdf,
            @FormParam("pkcs12") String pkcs12,@FormParam("password") String password) throws Exception {

        if (documentoPdf == null || documentoPdf.isEmpty()) {
            return "Se debe incluir el parametro documentoPdf";
        }
        
        if (pkcs12 == null || pkcs12.isEmpty()) {
            return "Se debe incluir el parametro pkcs12";
        }

        if (password == null || password.isEmpty()) {
            return "Se debe incluir el parametro password";
        }        
        
        return "Se ha firmado correctamente campeón";

//        return servicioAppFirmarDocumentoTransversal.firmarTransversal(pkcs12, password, sistema, operacion, url, versionFirmaEC, formatoDocumento, tokenJwt, llx, lly, pagina, tipoEstampado, razon, pre, des);
    }

}
