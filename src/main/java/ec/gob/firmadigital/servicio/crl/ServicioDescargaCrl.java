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
package ec.gob.firmadigital.servicio.crl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TimerService;
import javax.sql.DataSource;

import io.rubrica.crl.ServicioCRL;
import io.rubrica.utils.HttpClient;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio para cargar los CRLs de las CAs soportadas en una tabla.
 *
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Singleton
@Startup
public class ServicioDescargaCrl {

    @Resource
    private TimerService timerService;

    @Resource(lookup = "java:/FirmaDigitalDS")
    private DataSource ds;

    private static final Logger logger = Logger.getLogger(ServicioDescargaCrl.class.getName());

    @PostConstruct
    public void init() {
        crearTablaSiNoExiste();
        importarCrls();
    }

    @Schedule(minute = "0", hour = "*", persistent = false)
    public void importarCrls() {
        logger.info("Iniciando el proceso de descarga de CRL");

        logger.info("Descargando CRL de BCE...");
        X509CRL bceCrl = downloadCrl(ServicioCRL.BCE_CRL);

        logger.info("Descargando CRL de Security Data...");
        X509CRL sdCrl = downloadCrl(ServicioCRL.SD_CRL);

        logger.info("Descargando CRL de CJ...");
        X509CRL cjCrl = downloadCrl(ServicioCRL.CJ_CRL);

        logger.info("Descargando CRL de ANFAC...");
        X509CRL anfAcCrl = downloadCrl(ServicioCRL.ANFAC_CRL);

        try (Connection conn = ds.getConnection();
                Statement st = conn.createStatement();
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO crl_new (serial, fecharevocacion, razonrevocacion, entidadcertificadora) VALUES (?,?,?,?)")) {

            logger.info("Creando tabla temporal");
            st.executeUpdate("CREATE TABLE crl_new (LIKE crl)");

            int contadorBCE = 0, contadorSD = 0, contadorCJ = 0, contadorANFAC = 0;

            if (bceCrl != null) {
                contadorBCE = insertarCrl(bceCrl, 1, ps);
                logger.info("Registros insertados BCE: " + contadorBCE);
            } else {
                logger.info("No se inserta BCE");
            }

            if (sdCrl != null) {
                contadorSD = insertarCrl(sdCrl, 2, ps);
                logger.info("Registros insertados Security Data: " + contadorSD);
            } else {
                logger.info("No se inserta Security Data");
            }

            if (cjCrl != null) {
                contadorCJ = insertarCrl(cjCrl, 3, ps);
                logger.info("Registros insertados CJ: " + contadorCJ);
            } else {
                logger.info("No se inserta CJ");
            }

            if (anfAcCrl != null) {
                contadorANFAC = insertarCrl(anfAcCrl, 4, ps);
                logger.info("Registros insertados ANFAC: " + contadorANFAC);
            } else {
                logger.info("No se inserta ANFAC");
            }

            int total = contadorBCE + contadorSD + contadorCJ + contadorANFAC;
            logger.info("Registros insertados Total: " + total);

            logger.info("Moviendo tabla temporal a definitiva");
            st.execute("ALTER TABLE crl RENAME TO crl_old");
            st.execute("ALTER TABLE crl_new RENAME TO crl");
            st.execute("DROP TABLE crl_old");

            logger.info("Finalizado!");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al insertar certificado", e);
            throw new EJBException(e);
        }
    }

    private int insertarCrl(X509CRL crl, int entidadCertificadora, PreparedStatement ps) throws SQLException {
        // Existen CRLs?
        if (crl.getRevokedCertificates() == null) {
            return 0;
        }

        for (X509CRLEntry cert : crl.getRevokedCertificates()) {
            BigInteger serial = cert.getSerialNumber();
            Date fechaRevocacion = cert.getRevocationDate();
            String razonRevocacion = cert.getRevocationReason() == null ? "" : cert.getRevocationReason().toString();
            LocalDateTime ldt = LocalDateTime.ofInstant(fechaRevocacion.toInstant(), ZoneId.systemDefault());

            //https://www.ipa.go.jp/security/rfc/RFC3280-04EN.html#41202
            Pattern pattern = Pattern.compile("\\d{1,2000}");
            Matcher matcher = pattern.matcher(serial.toString());
            if (matcher.matches()) {
                ps.setString(1, serial.toString());
                ps.setObject(2, ldt);
                ps.setString(3, razonRevocacion);
                ps.setInt(4, entidadCertificadora);
                ps.addBatch();
            } else {
                logger.log(Level.SEVERE, "Error con el serial number {0} de la entidad certificadora {1}", new Object[]{serial.toString(), entidadCertificadora});
            }
        }

        int[] count = ps.executeBatch();
        return count.length;
    }

    private X509CRL downloadCrl(String url) {
        byte[] content;

        try {
            HttpClient http = new HttpClient();
            content = http.download(url);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error al descargar CRL de " + url, e);
            return null;
        }

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509CRL) cf.generateCRL(new ByteArrayInputStream(content));
        } catch (CertificateException | CRLException e) {
            logger.log(Level.SEVERE, "Error al descargar CRL de " + url + ": " + e.getMessage());
            return null;
        }
    }

    private void crearTablaSiNoExiste() {
        logger.info("Creando tabla CRL si es que no existe...");

        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS crl (serial VARCHAR(2000), fecharevocacion VARCHAR(2000), "
                    + "razonrevocacion VARCHAR(2000), entidadcertificadora VARCHAR(2000))");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al crear tabla CRL", e);
            throw new EJBException(e);
        }
    }
}
