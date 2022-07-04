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
import javax.ejb.Stateless;

import io.rubrica.certificate.to.Documento;
import io.rubrica.exceptions.SignatureVerificationException;
import io.rubrica.utils.Json;
import java.io.InputStream;

/**
 *
 * @author Christian Espinosa <christian.espinosa@mintel.gob.ec>, Misael
 * Fernández
 */
@Stateless
public class ServicioAppVerificarDocumento {

    public String verificarDocumento(InputStream inputStreamDocumento) {
        String retorno = null;
        Documento documento = null;
        try {
            documento = io.rubrica.utils.Utils.pdfToDocumento(inputStreamDocumento);
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
