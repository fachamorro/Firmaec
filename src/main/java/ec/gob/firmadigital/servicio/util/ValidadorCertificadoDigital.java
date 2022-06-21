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
package ec.gob.firmadigital.servicio.util;

import com.itextpdf.kernel.crypto.BadPasswordException;
import ec.gob.firmadigital.servicio.ServicioValidarCertificadoDigitalException;
import io.rubrica.certificate.CertEcUtils;
import io.rubrica.certificate.to.Certificado;
import io.rubrica.certificate.to.DatosUsuario;
import io.rubrica.core.Util;
import io.rubrica.exceptions.CertificadoInvalidoException;
import io.rubrica.exceptions.EntidadCertificadoraNoValidaException;
import io.rubrica.exceptions.HoraServidorException;
import io.rubrica.exceptions.RubricaException;
import io.rubrica.keystore.Alias;
import io.rubrica.keystore.FileKeyStoreProvider;
import io.rubrica.keystore.KeyStoreProvider;
import io.rubrica.keystore.KeyStoreUtilities;
import io.rubrica.utils.TiempoUtils;
import io.rubrica.utils.Utils;
import io.rubrica.utils.UtilsCrlOcsp;
import io.rubrica.utils.X509CertificateUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Christian Espinosa <christian.espinosa@mintel.gob.ec>, Misael Fernández
 */
public class ValidadorCertificadoDigital {

    public Certificado Certificate(KeyStore keyStore, String alias) throws RubricaException, ServicioValidarCertificadoDigitalException {
        Certificado certificado = null;
        try {
            X509CertificateUtils x509CertificateUtils = new X509CertificateUtils();
            if (x509CertificateUtils.validarX509Certificate((X509Certificate) keyStore.getCertificate(alias), null)) {//validación de firmaEC
                X509Certificate x509Certificate = (X509Certificate) keyStore.getCertificate(alias);
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
                TemporalAccessor accessor = dateTimeFormatter.parse(TiempoUtils.getFechaHoraServidor(null));
                Date fechaHoraISO = Date.from(Instant.from(accessor));
                //Validad certificado revocado
                Date fechaRevocado = UtilsCrlOcsp.validarFechaRevocado(x509Certificate, null);
                if (fechaRevocado != null && fechaRevocado.compareTo(fechaHoraISO) <= 0) {
                    throw new ServicioValidarCertificadoDigitalException("Certificado revocado: " + fechaRevocado);
                }
                boolean caducado;
                if (fechaHoraISO.compareTo(x509Certificate.getNotBefore()) <= 0 || fechaHoraISO.compareTo(x509Certificate.getNotAfter()) >= 0) {
//                    Toast.makeText(getBaseContext(), "Certificado caducado", Toast.LENGTH_LONG).show();
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
                        Utils.dateToCalendar(UtilsCrlOcsp.validarFechaRevocado(x509Certificate, null)),
                        caducado,
                        datosUsuario);
            } else {
                throw new ServicioValidarCertificadoDigitalException("Certificado no válido");
            }
        } catch (BadPasswordException bpe) {
            throw new ServicioValidarCertificadoDigitalException("Documento protegido con contraseña");
        } catch (InvalidKeyException ie) {
            throw new ServicioValidarCertificadoDigitalException("Problemas al abrir el documento");
        } catch (EntidadCertificadoraNoValidaException ecnve) {
            throw new ServicioValidarCertificadoDigitalException("Certificado no válido");
        } catch (HoraServidorException hse) {
            throw new ServicioValidarCertificadoDigitalException("Problemas en la red\\nIntente nuevamente o verifique su conexión");
        } catch (KeyStoreException kse) {
            throw new ServicioValidarCertificadoDigitalException("No se encontró archivo o la contraseña es inválida.");
        } catch (CertificadoInvalidoException | IOException e) {
            throw new ServicioValidarCertificadoDigitalException("Excepción no conocida: " + e);
        }
        return certificado;
    }

    public Certificado validarCertificado(String pkcs12, String password) throws Exception {
        byte encodedPkcs12[] = Base64.getDecoder().decode(pkcs12);
        InputStream inputStreamPkcs12 = new ByteArrayInputStream(encodedPkcs12);

        KeyStoreProvider ksp = new FileKeyStoreProvider(inputStreamPkcs12);
        KeyStore keyStore = ksp.getKeystore(password.toCharArray());

        List<Alias> signingAliases = KeyStoreUtilities.getSigningAliases(keyStore);
        String alias = signingAliases.get(0).getAlias();
        return Certificate(keyStore, alias);
    }

}
