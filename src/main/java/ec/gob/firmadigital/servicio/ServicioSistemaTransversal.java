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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Stateless;
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

/**
 * Servicio para invocar Web Services de los sistemas transaccionales.
 * Principalmente utilizado para almacenar el documento ya firmado.
 * 
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Stateless
public class ServicioSistemaTransversal {

    private static final Logger logger = Logger.getLogger(ServicioSistemaTransversal.class.getName());

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    public String almacenarDocumento(String url, String usuario, String documento, String archivo,
            String nombreFirmante) throws SOAPException {
        try {
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage message = factory.createMessage();
            SOAPBody body = message.getSOAPBody();

            SOAPFactory soapFactory = SOAPFactory.newInstance();
            Name bodyName = soapFactory.createName("grabar_archivos_firmados", "urn", "urn:soapapiorfeo");
            SOAPBodyElement bodyElement = body.addBodyElement(bodyName);
            bodyElement.addChildElement("set_var_usuario").addTextNode(usuario);
            bodyElement.addChildElement("set_var_documento").addTextNode(documento);
            bodyElement.addChildElement("set_var_archivo").addTextNode(archivo);
            bodyElement.addChildElement("set_var_datos_firmante").addTextNode(nombreFirmante);
            bodyElement.addChildElement("set_var_fecha").addTextNode(sdf.format(new Date()));

            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection connection = soapConnectionFactory.createConnection();
            URL endpoint = new URL(url);
            SOAPMessage response = connection.call(message, endpoint);
            connection.close();

            //0 is error, 1 ok
            
            SOAPBody soapBody = response.getSOAPBody();
            NodeList nl = soapBody.getElementsByTagName("result");
            System.out.println(nl.item(0).getTextContent());
            return nl.item(0).getTextContent();
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Error al invocar el servicio web", e);
            throw new RuntimeException(e);
        }
    }

    public static void main(String args[]) throws Exception {
        // INSERT INTO sistema(url, nombre, descripcion)
        // values('http://quipuxpruebas.gestiondocumental.gob.ec/interconexion/ws_firma_digital.php?wsdl',
        // 'quipux', 'Sistema Quipux');
        String url = "http://quipuxpruebas.gestiondocumental.gob.ec/interconexion/ws_firma_digital.php";

        Path path = Paths.get("/var/tmp/test.pdf");
        byte[] data = Files.readAllBytes(path);
        String documento = Base64.getEncoder().encodeToString(data);

        ServicioSistemaTransversal st = new ServicioSistemaTransversal();
        st.almacenarDocumento(url, "1710803196", "ricardo.pdf", documento, "Juan");
    }
}