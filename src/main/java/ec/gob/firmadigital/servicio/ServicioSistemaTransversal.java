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
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.NodeList;

import ec.gob.firmadigital.servicio.model.Sistema;

/**
 * Servicio para invocar Web Services de los sistemas transaccionales, utilizado
 * para almacenar el documento ya firmado.
 * 
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Stateless
public class ServicioSistemaTransversal {

    @PersistenceContext(unitName = "FirmaDigitalDS")
    private EntityManager em;

    private static final int PING_TIMEOUT = 5000;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private static final Logger logger = Logger.getLogger(ServicioSistemaTransversal.class.getName());

    /**
     * Obtiene el URL del Web Service de un sistema transversal, para devolver
     * el documento firmado por el usuario.
     * 
     * @param nombre
     *            nombre del sistema transversal
     * @return el URL del sistema traansversal
     * @throws IllegalArgumentException
     *             si no se encuentra ese nombre de sistema transversal
     */
    public URL buscarUrlSistema(String nombre) throws IllegalArgumentException {
        try {
            TypedQuery<Sistema> q = em.createQuery("SELECT s FROM Sistema s WHERE s.nombre = :nombre", Sistema.class);
            q.setParameter("nombre", nombre);
            Sistema sistema = q.getSingleResult();
            return new URL(sistema.getURL());
        } catch (NoResultException e) {
            throw new IllegalArgumentException("No se encontró el sistema " + nombre);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("El URL no es correcto: " + e.getMessage());
        }
    }

    public boolean pingSistemaTransversal(URL url) {
        try {
            InetAddress inet = InetAddress.getByName(url.getHost());
            logger.info("Enviando ping a " + inet);
            boolean reacheable = inet.isReachable(PING_TIMEOUT);
            logger.info(reacheable ? "Servidor si responde" : "Servidor NO responde");
            return reacheable;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error al tratar de hacer ping al servidor del sistema transversal: " + url, e);
            return false;
        }
    }

    public void almacenarDocumento(String usuario, String documento, String archivo, String datosFirmante, URL url)
            throws SistemaTransversalException {
        try {
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage soapMessage = factory.createMessage();
            SOAPBody body = soapMessage.getSOAPBody();

            SOAPFactory soapFactory = SOAPFactory.newInstance();
            Name bodyName = soapFactory.createName("grabar_archivos_firmados", "urn", "urn:soapapiorfeo");
            SOAPBodyElement bodyElement = body.addBodyElement(bodyName);
            bodyElement.addChildElement("set_var_usuario").addTextNode(usuario);
            bodyElement.addChildElement("set_var_documento").addTextNode(documento);
            bodyElement.addChildElement("set_var_archivo").addTextNode(archivo);
            bodyElement.addChildElement("set_var_datos_firmante").addTextNode(datosFirmante);
            bodyElement.addChildElement("set_var_fecha").addTextNode(sdf.format(new Date()));

            SOAPConnection connection = SOAPConnectionFactory.newInstance().createConnection();
            SOAPMessage response = connection.call(soapMessage, url);
            connection.close();

            SOAPBody soapBody = response.getSOAPBody();
            NodeList nl = soapBody.getElementsByTagName("result");

            // 0 is error, 1 ok
            String resultado = nl.item(0).getTextContent();
            logger.fine("Resultado enviado por el sistema transversal: " + resultado);

            if ("1".equals(resultado)) {
                return;
            } else if ("0".equals(resultado)) {
                throw new SistemaTransversalException("Se devuelve error del sistema transversal: " + resultado);
            } else {
                throw new SistemaTransversalException("Resultado invalido del sistema transversal: " + resultado);
            }
        } catch (SOAPException e) {
            logger.log(Level.SEVERE, "Error al actualizar el documento en el sistema transversal", e);
            throw new SistemaTransversalException("Error al invocar Web Service del sistema transversal", e);
        }
    }
}