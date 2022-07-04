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

import ec.gob.firmadigital.servicio.ServicioAppVerificarDocumento;
import io.rubrica.exceptions.InvalidFormatException;
import io.rubrica.model.Document;
import io.rubrica.model.InMemoryDocument;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

/**
 * REST Web Service
 *
 * @author Christian Espinosa <christian.espinosa@mintel.gob.ec>, Misael
 * Fernández
 */
@Path("appverificardocumento")
public class ServicioAppVerificarDocumentoRest {

    @EJB
    private ServicioAppVerificarDocumento servicioAppVerificarDocumento;

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String validarDocumento(@FormParam("documento") String documento) throws Exception {
        if (documento == null || documento.isEmpty()) {
            return "Se debe incluir el parametro documento";
        }
//        byte[] bs = DocumentoUtils.loadFile(documento);
//        FileNameMap MIMETYPES = URLConnection.getFileNameMap();
//        System.out.println("MIMETYPES: " + MIMETYPES.getContentTypeFor(documento));
//        if (MIMETYPES.getContentTypeFor(documento).equals("application/pdf")) {
//            Document document = new InMemoryDocument(bs);
//            InputStream is = document.openStream();
//            Documento documento = Utils.pdfToDocumento(is);
//            System.out.println("JSON:");
//            System.out.println(Json.GenerarJsonDocumento(documento));
//            System.out.println("Documento: " + documento);
//            if (documento.getCertificados() != null) {
//                documento.getCertificados().forEach((certificado) -> {
//                    System.out.println(certificado.toString());
//                });
//            }
//        }
        System.out.println("documento: " + documento);
        byte encodedPkcs12[] = Base64.getDecoder().decode(documento);
        System.out.println("encodedPkcs12: " + encodedPkcs12);
        try {
            InputStream inputStream = new ByteArrayInputStream(encodedPkcs12);
            return servicioAppVerificarDocumento.verificarDocumento(inputStream);
        } catch (Exception e) {
            throw new InvalidFormatException("No se ha podido leer el PDF: "+e.toString(), e);
        }
//        try {
//            Document document = new InMemoryDocument(encodedPkcs12);
//            System.out.println("document: "+document.toString());
//            try (InputStream is = document.openStream()) {
//                return servicioAppVerificarDocumento.verificarDocumento(is);
//            }
//        } catch (Exception e) {
//            throw new InvalidFormatException("No se ha podido leer el PDF: ", e);
//        }
//        return servicioAppVerificarDocumento.verificarDocumento(inputStreamDocumento);
    }
}
