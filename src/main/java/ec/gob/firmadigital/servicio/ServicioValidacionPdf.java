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

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.List;

import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import io.rubrica.certificate.ec.bce.CertificadoBancoCentral;
import io.rubrica.certificate.ec.bce.CertificadoBancoCentralFactory;
import io.rubrica.certificate.ec.securitydata.CertificadoSecurityData;
import io.rubrica.certificate.ec.securitydata.CertificadoSecurityDataFactory;
import io.rubrica.ocsp.OcspValidationException;
import io.rubrica.sign.Falla;
import io.rubrica.sign.Verificacion;
import io.rubrica.sign.pdf.Firma;
import io.rubrica.sign.pdf.VerificadorFirmaPdf;

@Stateless
@Path("/validacionpdf")
public class ServicioValidacionPdf {

    public static Verificacion verificar(byte[] pdf)
            throws SignatureException, OcspValidationException, KeyStoreException {
        try {
            VerificadorFirmaPdf verificador = new VerificadorFirmaPdf(pdf);
            return verificador.verificar();
        } catch (IOException e) {
            throw new RuntimeException("Error al verificar");
        }
    }

    public static String getNombre(byte[] pdf) throws SignatureException, OcspValidationException, KeyStoreException {
        try {
            VerificadorFirmaPdf verificador = new VerificadorFirmaPdf(pdf);
            Verificacion verificacion = verificador.verificar();
            List<Firma> firmas = verificacion.getFirmas();
            if (!firmas.isEmpty()) {
                Firma firma = firmas.get(0);
                X509Certificate certificado = firma.getCertificadoFirmante();

                if (CertificadoBancoCentralFactory.esCertificadoDelBancoCentral(certificado)) {
                    CertificadoBancoCentral bce = CertificadoBancoCentralFactory.construir(certificado);
                    return bce.getNombres() + " " + bce.getPrimerApellido() + " " + bce.getSegundoApellido();
                } else if (CertificadoSecurityDataFactory.esCertificadoDeSecurityData(certificado)) {
                    CertificadoSecurityData sd = CertificadoSecurityDataFactory.construir(certificado);
                    return sd.getNombres() + " " + sd.getPrimerApellido() + " " + sd.getSegundoApellido();
                }
            }
            return "Unknown";
        } catch (IOException e) {
            throw new RuntimeException("Error al verificar", e);
        }
    }

    @POST
    public JsonObject verificarPdf(String archivoBase64)
            throws KeyStoreException, SignatureException, OcspValidationException {
        try {
            byte[] pdf = Base64.getDecoder().decode(archivoBase64);
            VerificadorFirmaPdf verificador = new VerificadorFirmaPdf(pdf);
            Verificacion verificacion = verificador.verificar();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

            // Para construir un array de firmantes
            JsonArrayBuilder builder = Json.createArrayBuilder();

            List<Firma> firmas = verificacion.getFirmas();

            for (Firma firma : firmas) {
                X509Certificate certificado = firma.getCertificadoFirmante();
                Falla falla = firma.getFalla();

                if (CertificadoBancoCentralFactory.esCertificadoDelBancoCentral(certificado)) {
                    CertificadoBancoCentral bce = CertificadoBancoCentralFactory.construir(certificado);

                    if (falla == null) {
                        JsonObjectBuilder json = Json.createObjectBuilder();
                        json.add("fecha", sdf.format(firma.getFechaFirma().getTime()));
                        json.add("cedula", bce.getCedulaPasaporte());
                        json.add("nombre",
                                bce.getNombres() + " " + bce.getPrimerApellido() + " " + bce.getSegundoApellido());
                        json.add("cargo", bce.getCargo());
                        json.add("institucion", bce.getInstitucion());
                        builder.add(json);
                    } else {
                        JsonObjectBuilder json = Json.createObjectBuilder();
                        json.add("fecha", sdf.format(firma.getFechaFirma().getTime()));
                        json.add("cedula", bce.getCedulaPasaporte());
                        json.add("nombre",
                                bce.getNombres() + " " + bce.getPrimerApellido() + " " + bce.getSegundoApellido());
                        json.add("cargo", bce.getCargo());
                        json.add("institucion", bce.getInstitucion());

                        String mensaje = falla.getMensaje();
                        json.add("mensaje", mensaje);

                        builder.add(json);
                    }
                }
            }

            return Json.createObjectBuilder().add("firmantes", builder.build()).build();
        } catch (IOException e) {
            throw new RuntimeException("Error al verificar", e);
        }
    }
}