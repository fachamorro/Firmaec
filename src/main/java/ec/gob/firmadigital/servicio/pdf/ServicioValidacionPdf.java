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
import io.rubrica.certificate.ec.cj.CertificadoConsejoJudicatura;
import io.rubrica.certificate.ec.cj.CertificadoConsejoJudicaturaDataFactory;
import io.rubrica.certificate.ec.cj.CertificadoDepartamentoEmpresaConsejoJudicatura;
import io.rubrica.certificate.ec.cj.CertificadoEmpresaConsejoJudicatura;
import io.rubrica.certificate.ec.cj.CertificadoMiembroEmpresaConsejoJudicatura;
import io.rubrica.certificate.ec.cj.CertificadoPersonaJuridicaPrivadaConsejoJudicatura;
import io.rubrica.certificate.ec.cj.CertificadoPersonaJuridicaPublicaConsejoJudicatura;
import io.rubrica.certificate.ec.cj.CertificadoPersonaNaturalConsejoJudicatura;
import io.rubrica.certificate.ec.securitydata.CertificadoFuncionarioPublicoSecurityData;
import io.rubrica.certificate.ec.securitydata.CertificadoMiembroEmpresaSecurityData;
import io.rubrica.certificate.ec.securitydata.CertificadoPersonaJuridicaSecurityData;
import io.rubrica.certificate.ec.securitydata.CertificadoPersonaNaturalSecurityData;
import io.rubrica.certificate.ec.securitydata.CertificadoPruebaSecurityData;
import io.rubrica.certificate.ec.securitydata.CertificadoRepresentanteLegalSecurityData;
import io.rubrica.certificate.ec.securitydata.CertificadoSecurityData;
import io.rubrica.certificate.ec.securitydata.CertificadoSecurityDataFactory;
import io.rubrica.ocsp.OcspValidationException;
import io.rubrica.sign.InvalidFormatException;
import io.rubrica.sign.SignInfo;
import io.rubrica.sign.Signer;
import io.rubrica.sign.pdf.PDFSigner;
import io.rubrica.util.Utils;

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

    public String getNombre(byte[] pdf) throws IOException, InvalidFormatException, OcspValidationException {
        Signer signer = new PDFSigner();
        List<SignInfo> singInfos = signer.getSigners(pdf);

        if (!singInfos.isEmpty()) {
            SignInfo firma = singInfos.get(0);
            X509Certificate certificado = firma.getCerts()[0];

            logger.info("Verificando CRL local del certificado");
            boolean revocado = servicioCrl.isRevocado(certificado.getSerialNumber());
            logger.info("revocado=" + revocado);

            if (revocado) {
                // FIXME
                throw new OcspValidationException();
            }

            return Utils.getCN(certificado);
        } else {
            return "Unknown";
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

        Signer signer = new PDFSigner();
        List<SignInfo> firmas;

        try {
            firmas = signer.getSigners(pdf);
        } catch (InvalidFormatException | IOException e) {
            return Response.status(Status.BAD_REQUEST).entity("Error al verificar PDF: \"" + e.getMessage() + "\"")
                    .build();
        }

        // Para construir un array de firmantes
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        for (SignInfo firma : firmas) {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            X509Certificate certificado = firma.getCerts()[0];

            if (CertificadoBancoCentralFactory.esCertificadoDelBancoCentral(certificado)) {
                CertificadoBancoCentral bce = CertificadoBancoCentralFactory.construir(certificado);
                builder.add("fecha", sdf.format(firma.getSigningTime()));
                builder.add("cedula", bce.getCedulaPasaporte());
                builder.add("nombre",
                        bce.getNombres() + " " + bce.getPrimerApellido() + " " + bce.getSegundoApellido());
                builder.add("cargo", bce.getCargo());
                builder.add("institucion", bce.getInstitucion());
                arrayBuilder.add(builder);
            }

            if (CertificadoSecurityDataFactory.esCertificadoDeSecurityData(certificado)) {
                CertificadoSecurityData sd = CertificadoSecurityDataFactory.construir(certificado);

                builder.add("nombre", sd.getNombres() + " " + sd.getPrimerApellido() + " " + sd.getSegundoApellido());
                builder.add("fecha", sdf.format(firma.getSigningTime()));

                if (sd instanceof CertificadoFuncionarioPublicoSecurityData) {
                    builder.add("cedula", ((CertificadoFuncionarioPublicoSecurityData) sd).getCedulaPasaporte());
                    builder.add("cargo", ((CertificadoFuncionarioPublicoSecurityData) sd).getCargo());
                    builder.add("institucion", ((CertificadoFuncionarioPublicoSecurityData) sd).getInstitucion());
                }

                if (sd instanceof CertificadoMiembroEmpresaSecurityData) {
                    builder.add("cedula", ((CertificadoMiembroEmpresaSecurityData) sd).getCedulaPasaporte());
                    builder.add("cargo", ((CertificadoMiembroEmpresaSecurityData) sd).getCargo());
                    builder.add("institucion", "N/A");
                }

                if (sd instanceof CertificadoPersonaJuridicaSecurityData) {
                    builder.add("cedula", "N/A");
                    builder.add("cargo", "N/A");
                    builder.add("institucion", "N/A");
                }

                if (sd instanceof CertificadoPersonaNaturalSecurityData) {
                    builder.add("cedula", ((CertificadoPersonaNaturalSecurityData) sd).getCedulaPasaporte());
                    builder.add("cargo", "N/A");
                    builder.add("institucion", "N/A");
                }

                if (sd instanceof CertificadoRepresentanteLegalSecurityData) {
                    builder.add("cedula", ((CertificadoRepresentanteLegalSecurityData) sd).getCedulaPasaporte());
                    builder.add("cargo", ((CertificadoRepresentanteLegalSecurityData) sd).getCargo());
                    builder.add("institucion", "N/A");
                }

                if (sd instanceof CertificadoPruebaSecurityData) {
                    builder.add("cedula", ((CertificadoPruebaSecurityData) sd).getCedulaPasaporte());
                    builder.add("cargo", "N/A");
                    builder.add("institucion", "N/A");
                }

                arrayBuilder.add(builder);
            }

            if (CertificadoConsejoJudicaturaDataFactory.esCertificadoDelConsejoJudicatura(certificado)) {
                CertificadoConsejoJudicatura cj = CertificadoConsejoJudicaturaDataFactory.construir(certificado);
                builder.add("fecha", sdf.format(firma.getSigningTime()));

                if (cj instanceof CertificadoDepartamentoEmpresaConsejoJudicatura) {
                    builder.add("cedula", ((CertificadoDepartamentoEmpresaConsejoJudicatura) cj).getCedulaPasaporte());
                    builder.add("nombre",
                            ((CertificadoDepartamentoEmpresaConsejoJudicatura) cj).getNombres() + " "
                                    + ((CertificadoDepartamentoEmpresaConsejoJudicatura) cj).getPrimerApellido() + " "
                                    + ((CertificadoDepartamentoEmpresaConsejoJudicatura) cj).getSegundoApellido());
                    builder.add("cargo", ((CertificadoDepartamentoEmpresaConsejoJudicatura) cj).getCargo());
                    builder.add("institucion", "N/A");
                }

                if (cj instanceof CertificadoEmpresaConsejoJudicatura) {
                    builder.add("cedula", ((CertificadoEmpresaConsejoJudicatura) cj).getCedulaPasaporte());
                    builder.add("nombre",
                            ((CertificadoEmpresaConsejoJudicatura) cj).getNombres() + " "
                                    + ((CertificadoEmpresaConsejoJudicatura) cj).getPrimerApellido() + " "
                                    + ((CertificadoEmpresaConsejoJudicatura) cj).getSegundoApellido());
                    builder.add("cargo", ((CertificadoEmpresaConsejoJudicatura) cj).getCargo());
                    builder.add("institucion", "N/A");
                }

                if (cj instanceof CertificadoMiembroEmpresaConsejoJudicatura) {
                    builder.add("cedula", ((CertificadoMiembroEmpresaConsejoJudicatura) cj).getCedulaPasaporte());
                    builder.add("nombre",
                            ((CertificadoMiembroEmpresaConsejoJudicatura) cj).getNombres() + " "
                                    + ((CertificadoMiembroEmpresaConsejoJudicatura) cj).getPrimerApellido() + " "
                                    + ((CertificadoMiembroEmpresaConsejoJudicatura) cj).getSegundoApellido());
                    builder.add("cargo", ((CertificadoMiembroEmpresaConsejoJudicatura) cj).getCargo());
                    builder.add("institucion", "N/A");
                }

                if (cj instanceof CertificadoPersonaJuridicaPrivadaConsejoJudicatura) {
                    builder.add("cedula",
                            ((CertificadoPersonaJuridicaPrivadaConsejoJudicatura) cj).getCedulaPasaporte());
                    builder.add("nombre", ((CertificadoPersonaJuridicaPrivadaConsejoJudicatura) cj).getNombres() + " "
                            + ((CertificadoPersonaJuridicaPrivadaConsejoJudicatura) cj).getPrimerApellido() + " "
                            + ((CertificadoPersonaJuridicaPrivadaConsejoJudicatura) cj).getSegundoApellido());
                    builder.add("cargo", ((CertificadoPersonaJuridicaPrivadaConsejoJudicatura) cj).getCargo());
                    builder.add("institucion", "N/A");
                }

                if (cj instanceof CertificadoPersonaJuridicaPublicaConsejoJudicatura) {
                    builder.add("cedula",
                            ((CertificadoPersonaJuridicaPublicaConsejoJudicatura) cj).getCedulaPasaporte());
                    builder.add("nombre", ((CertificadoPersonaJuridicaPublicaConsejoJudicatura) cj).getNombres() + " "
                            + ((CertificadoPersonaJuridicaPublicaConsejoJudicatura) cj).getPrimerApellido() + " "
                            + ((CertificadoPersonaJuridicaPublicaConsejoJudicatura) cj).getSegundoApellido());
                    builder.add("cargo", ((CertificadoPersonaJuridicaPublicaConsejoJudicatura) cj).getCargo());
                    builder.add("institucion", "N/A");
                }

                if (cj instanceof CertificadoPersonaNaturalConsejoJudicatura) {
                    builder.add("cedula", ((CertificadoPersonaNaturalConsejoJudicatura) cj).getCedulaPasaporte());
                    builder.add("nombre",
                            ((CertificadoPersonaNaturalConsejoJudicatura) cj).getNombres() + " "
                                    + ((CertificadoPersonaNaturalConsejoJudicatura) cj).getPrimerApellido() + " "
                                    + ((CertificadoPersonaNaturalConsejoJudicatura) cj).getSegundoApellido());
                    builder.add("cargo", "N/A");
                    builder.add("institucion", "N/A");
                }

                arrayBuilder.add(builder);
            }
        }

        // Construir JSON
        JsonArray jsonArray = arrayBuilder.build();
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        String json = objectBuilder.add("firmantes", jsonArray).build().toString();

        return Response.ok(json, MediaType.APPLICATION_JSON).build();
    }
}