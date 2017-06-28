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

package ec.gob.firmadigital.servicio.pdf;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;

import ec.gob.firmadigital.servicio.crl.ServicioCrl;
import ec.gob.firmadigital.servicio.util.Base64InvalidoException;
import ec.gob.firmadigital.servicio.util.Base64Util;
import io.rubrica.certificate.ec.bce.CertificadoBancoCentral;
import io.rubrica.certificate.ec.bce.CertificadoBancoCentralFactory;
import io.rubrica.certificate.ec.securitydata.CertificadoSecurityData;
import io.rubrica.certificate.ec.securitydata.CertificadoSecurityDataFactory;
import io.rubrica.ocsp.OcspValidationException;
import io.rubrica.sign.Falla;
import io.rubrica.sign.Verificacion;
import io.rubrica.sign.pdf.Firma;
import io.rubrica.sign.pdf.VerificadorFirmaPdf;

/**
 * Servicio de verificacion de archivos PDF.
 * 
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Stateless
@Path("/validacionpdf")
public class ServicioValidacionPdf {

    @EJB
    private ServicioCrl servicioCrl;

    private static final Logger logger = Logger.getLogger(ServicioValidacionPdf.class.getName());

    public String getNombre(byte[] pdf) throws SignatureException, OcspValidationException, KeyStoreException {
        try {
            VerificadorFirmaPdf verificador = new VerificadorFirmaPdf(pdf);
            Verificacion verificacion = verificador.verificar();
            List<Firma> firmas = verificacion.getFirmas();

            if (!firmas.isEmpty()) {
                Firma firma = firmas.get(0);
                X509Certificate certificado = firma.getCertificadoFirmante();

                logger.info("Verificando CRL local del certificado");
                boolean revocado = servicioCrl.isRevocado(certificado.getSerialNumber());
                logger.info("revocado=" + revocado);

                if (revocado) {
                    // FIXME
                    throw new OcspValidationException();
                }

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
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verificarPdf(String archivoBase64)
            throws KeyStoreException, SignatureException, OcspValidationException {

        byte[] pdf;

        try {
            pdf = Base64Util.decode(archivoBase64);
        } catch (Base64InvalidoException e) {
            return Response.status(Status.BAD_REQUEST).entity("Error al decodificar Base64").build();
        }

        Verificacion verificacion;

        try {
            VerificadorFirmaPdf verificador = new VerificadorFirmaPdf(pdf);
            verificacion = verificador.verificar();
        } catch (IOException e) {
            return Response.status(Status.BAD_REQUEST).entity("Error al verificar PDF: \"" + e.getMessage() + "\"")
                    .build();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        // Para construir un array de firmantes
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        List<Firma> firmas = verificacion.getFirmas();

        for (Firma firma : firmas) {
            X509Certificate certificado = firma.getCertificadoFirmante();
            Falla falla = firma.getFalla();

            if (CertificadoBancoCentralFactory.esCertificadoDelBancoCentral(certificado)) {
                CertificadoBancoCentral bce = CertificadoBancoCentralFactory.construir(certificado);

                if (falla == null) {
                    JsonObjectBuilder builder = Json.createObjectBuilder();
                    builder.add("fecha", sdf.format(firma.getFechaFirma().getTime()));
                    builder.add("cedula", bce.getCedulaPasaporte());
                    builder.add("nombre",
                            bce.getNombres() + " " + bce.getPrimerApellido() + " " + bce.getSegundoApellido());
                    builder.add("cargo", bce.getCargo());
                    builder.add("institucion", bce.getInstitucion());
                    arrayBuilder.add(builder);
                } else {
                    JsonObjectBuilder builder = Json.createObjectBuilder();
                    builder.add("fecha", sdf.format(firma.getFechaFirma().getTime()));
                    builder.add("cedula", bce.getCedulaPasaporte());
                    builder.add("nombre",
                            bce.getNombres() + " " + bce.getPrimerApellido() + " " + bce.getSegundoApellido());
                    builder.add("cargo", bce.getCargo());
                    builder.add("institucion", bce.getInstitucion());

                    String mensaje = falla.getMensaje();
                    builder.add("mensaje", mensaje);

                    arrayBuilder.add(builder);
                }
            }
        }

        // Construir JSON
        JsonArray jsonArray = arrayBuilder.build();
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        String json = objectBuilder.add("firmantes", jsonArray).build().toString();

        return Response.ok(json, MediaType.APPLICATION_JSON).build();
    }
}