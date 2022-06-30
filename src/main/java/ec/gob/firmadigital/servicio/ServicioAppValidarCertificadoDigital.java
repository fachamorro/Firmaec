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
import ec.gob.firmadigital.servicio.util.X509CertificateUtils;
import io.rubrica.certificate.CertEcUtils;
import io.rubrica.certificate.to.Certificado;
import io.rubrica.certificate.to.DatosUsuario;
import io.rubrica.core.Util;
import io.rubrica.exceptions.EntidadCertificadoraNoValidaException;
import io.rubrica.exceptions.HoraServidorException;
import io.rubrica.keystore.Alias;
import io.rubrica.keystore.FileKeyStoreProvider;
import io.rubrica.keystore.KeyStoreProvider;
import io.rubrica.keystore.KeyStoreUtilities;
import io.rubrica.utils.TiempoUtils;
import io.rubrica.utils.Utils;
import io.rubrica.utils.UtilsCrlOcsp;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.validation.constraints.NotNull;

/**
 * Buscar en una lista de URLs permitidos para utilizar como API. Esto permite
 * federar la utilización de FirmaEC sobre otra infraestructura, consultando en
 * una lista de servidores permitidos.
 *
 * @author Christian Espinosa <christian.espinosa@mintel.gob.ec>, Misael
 * Fernández
 */
@Stateless
public class ServicioAppValidarCertificadoDigital {

    /**
     * Busca un ApiUrl por URL.
     *
     * @param pkcs12
     * @param password
     * @return json
     */
    public String appValidarCertificadoDigital(@NotNull String pkcs12, @NotNull String password) {
        Certificado certificado = null;
        String retorno = null;
        boolean caducado = true, revocado = true;

        try {
            byte encodedPkcs12[] = Base64.getDecoder().decode(pkcs12);
            InputStream inputStreamPkcs12 = new ByteArrayInputStream(encodedPkcs12);

            KeyStoreProvider ksp = new FileKeyStoreProvider(inputStreamPkcs12);
            KeyStore keyStore;
            keyStore = ksp.getKeystore(password.toCharArray());

            List<Alias> signingAliases = KeyStoreUtilities.getSigningAliases(keyStore);
            String alias = signingAliases.get(0).getAlias();

            X509CertificateUtils x509CertificateUtils = new X509CertificateUtils();
            if (x509CertificateUtils.validarX509Certificate((X509Certificate) keyStore.getCertificate(alias), null)) {//validación de firmaEC
                X509Certificate x509Certificate = (X509Certificate) keyStore.getCertificate(alias);
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
                TemporalAccessor accessor = dateTimeFormatter.parse(TiempoUtils.getFechaHoraServidor(null));
                Date fechaHoraISO = Date.from(Instant.from(accessor));
                //Validad certificado revocado
//                Date fechaRevocado = fechaString_Date("2022-06-01 10:00:16");
                Date fechaRevocado = UtilsCrlOcsp.validarFechaRevocado(x509Certificate, null);
                System.out.println("ENTRO A 1");
                if (fechaRevocado != null && fechaRevocado.compareTo(fechaHoraISO) <= 0) {
                    retorno = "Certificado revocado: " + fechaRevocado;
                    revocado = true;
                } else {
                    revocado = false;
                }
//                if (fechaHoraISO.compareTo(x509Certificate.getNotBefore()) <= 0 || fechaHoraISO.compareTo(fechaString_Date("2022-06-21 10:00:16")) >= 0) {
                if (fechaHoraISO.compareTo(x509Certificate.getNotBefore()) <= 0 || fechaHoraISO.compareTo(x509Certificate.getNotAfter()) >= 0) {
                    retorno = "Certificado caducado";
                    caducado = true;
                } else {
                    caducado = false;
                }
                DatosUsuario datosUsuario = CertEcUtils.getDatosUsuarios(x509Certificate);
                certificado = new Certificado(
                        Util.getCN(x509Certificate),
                        CertEcUtils.getNombreCA(x509Certificate),
                        Utils.dateToCalendar(x509Certificate.getNotBefore()),
                        Utils.dateToCalendar(x509Certificate.getNotAfter()),
                        null,
                        //Utils.dateToCalendar(fechaString_Date("2022-06-01 10:00:16")),
                        Utils.dateToCalendar(UtilsCrlOcsp.validarFechaRevocado(x509Certificate, null)),
                        caducado,
                        datosUsuario);
            } else {
                retorno = "Certificado no válido";
            }
        } catch (EntidadCertificadoraNoValidaException ecnve) {
            retorno = "Certificado no válido";
        } catch (HoraServidorException hse) {
            retorno = "Problemas en la red\\nIntente nuevamente o verifique su conexión";
        } catch (KeyStoreException kse) {
            if (kse.getCause().toString().contains("Invalid keystore format")) {
                retorno = "Certificado digital es inválido.";
            }
            if (kse.getCause().toString().contains("keystore password was incorrect")) {
                retorno = "La contraseña es inválida.";
            }
        } catch (IOException ioe) {
            retorno = "Excepción no conocida: " + ioe;
        } catch (Exception ex) {
            retorno = "Excepción no conocida: " + ex;
        } finally {
            Gson gson = new Gson();
            JsonObject jsonDoc = new JsonObject();
            jsonDoc.addProperty("error", retorno);
            JsonArray arrayCer = new JsonArray();
            if (certificado != null) {
                boolean signValidate = true;
                if (revocado || certificado.getValidated() || !certificado.getDatosUsuario().isCertificadoDigitalValido()) {
                    signValidate = false;
                } else {
                    signValidate = true;

                }
                jsonDoc.addProperty("firmaValida", signValidate);
                JsonObject jsonCer = new JsonObject();
                jsonCer.addProperty("emitidoPara", certificado.getIssuedTo());
                jsonCer.addProperty("emitidoPor", certificado.getIssuedBy());
                jsonCer.addProperty("validoDesde", calendarToString(certificado.getValidFrom()));
                jsonCer.addProperty("validoHasta", calendarToString(certificado.getValidTo()));
                jsonCer.addProperty("fechaRevocado", certificado.getRevocated() != null ? calendarToString(certificado.getRevocated()) : "");
                jsonCer.addProperty("certificadoVigente", !certificado.getValidated());
                jsonCer.addProperty("clavesUso", certificado.getKeyUsages());
                jsonCer.addProperty("integridadFirma", certificado.getSignVerify());
                jsonCer.addProperty("cedula", certificado.getDatosUsuario().getCedula());
                jsonCer.addProperty("nombre", certificado.getDatosUsuario().getNombre());
                jsonCer.addProperty("apellido", certificado.getDatosUsuario().getApellido());
                jsonCer.addProperty("institucion", certificado.getDatosUsuario().getInstitucion());
                jsonCer.addProperty("cargo", certificado.getDatosUsuario().getCargo());
                jsonCer.addProperty("entidadCertificadora", certificado.getDatosUsuario().getEntidadCertificadora());
                jsonCer.addProperty("serial", certificado.getDatosUsuario().getSerial());
                jsonCer.addProperty("certificadoDigitalValido", certificado.getDatosUsuario().isCertificadoDigitalValido());
                arrayCer.add(jsonCer);
                jsonDoc.add("certificado", arrayCer);
            }
            return gson.toJson(jsonDoc);
        }
    }

    private String calendarToString(Calendar calendar) {
        Date date = calendar.getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date);
    }
}
