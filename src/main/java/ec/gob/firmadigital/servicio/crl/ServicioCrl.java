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
import java.sql.ResultSet;
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
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import io.rubrica.util.HttpClient;

/**
 * Servicio para cargar los CRLs de las CAs soportadas en una tabla.
 *
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Singleton
@Startup
public class ServicioCrl {

    @Resource
    private TimerService timerService;

    @Resource(lookup = "java:/FirmaDigitalDS")
    private DataSource ds;

    @PersistenceContext(unitName = "FirmaDigitalDS")
    private EntityManager em;

    private static final String BCE_CRL = "http://www.eci.bce.ec/CRL/eci_bce_ec_crlfilecomb.crl";
    private static final String SD_CRL = "https://direct.securitydata.net.ec/~crl/autoridad_de_certificacion_sub_security_data_entidad_de_certificacion_de_informacion_curity_data_s.a._c_ec_crlfile.crl";
    private static final String CJ_CRL = "https://www.icert.fje.gob.ec/crl/icert.crl";

    private static final Logger logger = Logger.getLogger(ServicioCrl.class.getName());

    @PostConstruct
    public void init() {
        crearTablaSiNoExiste();
        importarCrls();
    }

    @Schedule(minute = "0", hour = "*", persistent = false)
    public void importarCrls() {
        logger.info("Iniciando el proceso de descarga de CRL");

        logger.info("Descargando CRL de BCE...");
        X509CRL bceCrl = downloadDrl(BCE_CRL);

        logger.info("Descargando CRL de Security Data...");
        X509CRL sdCrl = downloadDrl(SD_CRL);

        logger.info("Descargando CRL de CJ...");
        X509CRL cjCrl = downloadDrl(CJ_CRL);

        Connection conn = null;
        Statement st = null;
        PreparedStatement ps = null;

        try {
            conn = ds.getConnection();
            st = conn.createStatement();

            logger.info("Creando tabla temporal");
            st.executeUpdate("CREATE TABLE crl_new (LIKE crl)");

            ps = conn.prepareStatement(
                    "INSERT INTO crl_new (serial, fecharevocacion, razonrevocacion, entidadcertificadora) VALUES (?,?,?,?)");

            int contadorBCE = insertarCrl(bceCrl, 1, ps);
            logger.info("Registros insertados BCE: " + contadorBCE);

            int contadorSD = insertarCrl(sdCrl, 2, ps);
            logger.info("Registros insertados Security Data: " + contadorSD);

            int contadorCJ = insertarCrl(cjCrl, 3, ps);
            logger.info("Registros insertados CJ: " + contadorCJ);

            int total = contadorBCE + contadorSD + contadorCJ;
            logger.info("Registros insertados Total: " + total);

            logger.info("Moviendo tabla temporal a definitiva");
            st.execute("ALTER TABLE crl RENAME TO crl_old");
            st.execute("ALTER TABLE crl_new RENAME TO crl");
            st.execute("DROP TABLE crl_old");

            logger.info("Finalizado!");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al insertar certificado", e);
            throw new EJBException(e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    public boolean isRevocado(BigInteger serial) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = ds.getConnection();
            ps = conn.prepareStatement("SELECT serial FROM crl WHERE serial=?");
            ps.setBigDecimal(1, new BigDecimal(serial));
            rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al buscar certificado", e);
            throw new EJBException(e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    private int insertarCrl(X509CRL crl, int entidadCertificadora, PreparedStatement ps) throws SQLException {
        for (X509CRLEntry cert : crl.getRevokedCertificates()) {
            BigInteger serial = cert.getSerialNumber();
            Date fechaRevocacion = cert.getRevocationDate();
            String razonRevocacion = cert.getRevocationReason() == null ? "" : cert.getRevocationReason().toString();
            LocalDateTime ldt = LocalDateTime.ofInstant(fechaRevocacion.toInstant(), ZoneId.systemDefault());

            ps.setBigDecimal(1, new BigDecimal(serial));
            ps.setObject(2, ldt);
            ps.setString(3, razonRevocacion);
            ps.setInt(4, entidadCertificadora);
            ps.addBatch();
        }

        int[] count = ps.executeBatch();
        return count.length;
    }

    private X509CRL downloadDrl(String url) {
        byte[] content;

        try {
            HttpClient http = new HttpClient();
            content = http.download(url);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error al descargar CRL de " + url, e);
            throw new EJBException(e);
        }

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509CRL) cf.generateCRL(new ByteArrayInputStream(content));
        } catch (CertificateException | CRLException e) {
            logger.log(Level.SEVERE, "Error al procesar CRL de " + url, e);
            throw new EJBException(e);
        }
    }

    private void crearTablaSiNoExiste() {
        logger.info("Creando tabla CRL si es que no existe...");

        Connection conn = null;
        Statement st = null;

        try {
            conn = ds.getConnection();
            st = conn.createStatement();
            st.executeUpdate("CREATE TABLE IF NOT EXISTS crl (serial BIGINT, fecharevocacion VARCHAR(2000), "
                    + "razonrevocacion VARCHAR(2000), entidadcertificadora VARCHAR(2000))");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al crear tabla CRL", e);
            throw new EJBException(e);
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }
}
