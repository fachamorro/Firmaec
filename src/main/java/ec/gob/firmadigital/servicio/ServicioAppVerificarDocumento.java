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
package ec.gob.firmadigital.servicio;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ec.gob.firmadigital.servicio.util.Base64Util;
import javax.ejb.Stateless;

import io.rubrica.certificate.to.Documento;
import io.rubrica.exceptions.SignatureVerificationException;
import io.rubrica.utils.FileUtils;
import io.rubrica.utils.Json;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.validation.constraints.NotNull;

/**
 *
 * @author Christian Espinosa <christian.espinosa@mintel.gob.ec>, Misael
 * Fernández
 */
@Stateless
public class ServicioAppVerificarDocumento {

    private final int BUFFER_SIZE = 8192;
    
    public String verificarDocumento(@NotNull String base64Documento) {
        String retorno = null;
        Documento documento = null;
        try {
            byte[] byteDocumento = Base64Util.decode(base64Documento);

//        byte[] buffer = new byte[BUFFER_SIZE];
//        int count;
//        String jsonRespuesta;
//        // Leer la respuesta del sw
//        try (InputStream in = con.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//            while ((count = in.read(buffer)) != -1) {
//                out.write(buffer, 0, count);
//            }
//            jsonRespuesta = out.toString();
//        }
//        System.out.println("documento: " + documento);
//        byte encodedPkcs12[] = Base64.getDecoder().decode(documento);
//        System.out.println("encodedPkcs12: " + encodedPkcs12);
//        InputStream inputStream = new ByteArrayInputStream(encodedPkcs12);
//        System.out.println("inputStream: " + inputStream);
//
//        byte[] buffer = new byte[BUFFER_SIZE];
//        int count;
//        String base64;
//        try (InputStream in = inputStream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//            while ((count = in.read(buffer)) != -1) {
//                out.write(buffer, 0, count);
//            }
//            base64 = out.toString();
//            System.out.println("out.toString(): " + out.toString());
//        }
//        byte[] byteDocumento = base64.getBytes();
//        InputStream inputStreamDocumento = new ByteArrayInputStream(byteDocumento);
//        documento = io.rubrica.utils.Utils.pdfToDocumento(inputStreamDocumento);

            documento = io.rubrica.utils.Utils.pdfToDocumento(FileUtils.byteArrayConvertToFile(byteDocumento));
        } catch (java.lang.UnsupportedOperationException uoe) {
            retorno = "No es posible procesar el documento desde dispositivo móvil\nIntentar en FirmaEC de Escritorio";
        } catch (com.itextpdf.io.IOException ioe) {
            retorno = "El archivo no es PDF";
        } catch (SignatureVerificationException sve) {
            retorno = sve.toString();
        } catch (Exception ex) {
            retorno = ex.toString();
        }

        Gson gson = new Gson();
        JsonObject jsonDoc = new JsonObject();
        jsonDoc.addProperty("error", retorno);
        JsonArray arrayCer = new JsonArray();
        if (documento != null) {
            jsonDoc.addProperty("documentoValida", "por resolver");
            arrayCer.add(Json.generarJsonDocumento(documento));
            jsonDoc.add("certificado", arrayCer);
        }
        return gson.toJson(jsonDoc);
    }

}
