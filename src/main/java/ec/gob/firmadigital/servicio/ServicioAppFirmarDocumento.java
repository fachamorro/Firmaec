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

import com.itextpdf.kernel.pdf.PdfReader;
import ec.gob.firmadigital.servicio.util.FirmaDigitalPdf;
import ec.gob.firmadigital.servicio.util.Propiedades;
import io.rubrica.certificate.to.Documento;
import io.rubrica.exceptions.SignatureVerificationException;
import io.rubrica.keystore.Alias;
import io.rubrica.keystore.FileKeyStoreProvider;
import io.rubrica.keystore.KeyStoreProvider;
import io.rubrica.keystore.KeyStoreUtilities;
import io.rubrica.sign.SignInfo;
import io.rubrica.sign.Signer;
import io.rubrica.sign.pdf.PDFSignerItext;
import io.rubrica.utils.Json;
import io.rubrica.utils.TiempoUtils;
import static io.rubrica.utils.Utils.pdfToDocumento;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.validation.constraints.NotNull;

/**
 *
 * @author Christian Espinosa <christian.espinosa@mintel.gob.ec>, Misael
 * Fernández
 */
@Stateless
public class ServicioAppFirmarDocumento {

    private String versionFirmaEC = null;
    private String formatoDocumento = null;
    private String llx = null;
    private String lly = null;
    private String tipoEstampado = null;
    private String pagina = null;

    public String firmarDocumento(@NotNull String pkcs12, @NotNull String password,
            @NotNull String documentoBase64, String versionFirmaEC, String formatoDocumento,
            String llx, String lly, String pagina, String tipoEstampado, String razon) throws KeyStoreException {
        // Parametros opcionales
        this.versionFirmaEC = versionFirmaEC;
        this.formatoDocumento = formatoDocumento;
        this.llx = llx;
        this.lly = lly;
        this.tipoEstampado = tipoEstampado;
        this.pagina = pagina;
        Documento documento = null;
        String retorno = null;

        KeyStore keyStore;
        try {
            keyStore = getKeyStore(pkcs12, password);
        } catch (java.security.KeyStoreException kse) {
            retorno = "La contraseña es inválida.";
            return retorno;
        }
        String alias = getAlias(keyStore);
        byte[] byteDocumentoSigned = null;
        try {
            byteDocumentoSigned = firmarDocumentos(documentoBase64, keyStore, password, alias);
        } catch (java.security.UnrecoverableKeyException e) {
            retorno = "Certificado Corrupto";
        } catch (Exception ex) {
            retorno = ex.getMessage();
            Logger.getLogger(ServicioAppFirmarDocumento.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (byteDocumentoSigned != null) {

            try {
                //Verificar Documento
                InputStream inputStreamDocumento = new ByteArrayInputStream(byteDocumentoSigned);
                PdfReader pdfReader = new PdfReader(inputStreamDocumento);
                Signer signer = new PDFSignerItext();
                java.util.List<SignInfo> signInfos;
                signInfos = signer.getSigners(byteDocumentoSigned);
                documento = pdfToDocumento(pdfReader, signInfos);
            } catch (java.lang.UnsupportedOperationException uoe) {
                retorno = "No es posible procesar el documento desde dispositivo móvil\nIntentar en FirmaEC de Escritorio";
            } catch (com.itextpdf.io.IOException ioe) {
                retorno = "El archivo no es PDF";
            } catch (SignatureVerificationException sve) {
                retorno = sve.toString();
            } catch (Exception ex) {
                retorno = ex.toString();
            }
        }
        if (documento == null) {
            documento = new Documento(false, false, new ArrayList<>(), retorno);
        }
        return Json.generarJsonDocumentoFirmado(byteDocumentoSigned, documento);
    }

    private byte[] firmarDocumentos(String documentoBase64, KeyStore keyStore, String keyStorePassword, String alias)
            throws Exception {
        byte[] byteDocumentoSign = null;
        // Firmar!
//            if ("xml".equalsIgnoreCase(formato)) {
//                FirmaDigitalXml firmador = new FirmaDigitalXml();
//                // FIXME
//                documentoFirmado = firmador.firmar(keyStore, documento, clave);
//            }
        if ("pdf".equalsIgnoreCase(formatoDocumento)) {
            byte[] byteDocumento = java.util.Base64.getDecoder().decode(documentoBase64);
            FirmaDigitalPdf firmador = new FirmaDigitalPdf();

            String fechaHora = TiempoUtils.getFechaHoraServidor(null);
            Properties properties = Propiedades.propiedades(versionFirmaEC, llx, lly, pagina, tipoEstampado, null, fechaHora);
            byteDocumentoSign = firmador.firmar(keyStore, alias, byteDocumento, keyStorePassword.toString().toCharArray(), properties, null);
        }
        return byteDocumentoSign;
    }

    private KeyStore getKeyStore(String pkcs12, String password) throws KeyStoreException {
        byte encodedPkcs12[] = Base64.getDecoder().decode(pkcs12);
        InputStream inputStreamPkcs12 = new ByteArrayInputStream(encodedPkcs12);

        KeyStoreProvider ksp = new FileKeyStoreProvider(inputStreamPkcs12);
        return ksp.getKeystore(password.toCharArray());
    }

    private String getAlias(KeyStore keyStore) {
        List<Alias> signingAliases = KeyStoreUtilities.getSigningAliases(keyStore);
        return signingAliases.get(0).getAlias();
    }
}
