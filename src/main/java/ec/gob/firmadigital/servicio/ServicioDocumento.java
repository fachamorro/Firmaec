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

import static ec.gob.firmadigital.servicio.token.TokenTimeout.DEFAULT_TIMEOUT;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;

import ec.gob.firmadigital.servicio.model.Documento;
import ec.gob.firmadigital.servicio.pdf.ServicioValidacionPdf;
import ec.gob.firmadigital.servicio.token.ServicioToken;
import ec.gob.firmadigital.servicio.token.TokenExpiradoException;
import ec.gob.firmadigital.servicio.token.TokenInvalidoException;
import ec.gob.firmadigital.servicio.token.TokenTimeout;
import ec.gob.firmadigital.servicio.util.Base64InvalidoException;
import io.rubrica.ocsp.OcspValidationException;
import io.rubrica.sign.InvalidFormatException;

/**
 * Servicio para almacenar, actualizar y obtener documentos desde los sistemas
 * transversales y la aplicación en api.firmadigital.gob.ec
 *
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Stateless
public class ServicioDocumento {

    @EJB
    private ServicioToken servicioToken;

    @EJB
    private ServicioSistemaTransversal servicioSistemaTransversal;

    @EJB
    private ServicioValidacionPdf servicioValidacionPdf;

    @EJB
    private ServicioLog servicioLog;

    @PersistenceContext(unitName = "FirmaDigitalDS")
    private EntityManager em;

    private static final Logger logger = Logger.getLogger(ServicioDocumento.class.getName());

    /**
     * Crea documentos en el sistema, para ser firmados por un cliente.
     * 
     * @param cedula
     * @param sistema
     * @param archivos
     * @return
     */
    public String crearDocumentos(@NotNull String cedula, @NotNull String sistema,
            @NotNull Map<String, String> archivos) throws Base64InvalidoException {

        // Verificar si existe el sistema
        servicioSistemaTransversal.buscarSistema(sistema);

        List<String> ids = new ArrayList<>();

        for (String nombre : archivos.keySet()) {
            String archivo = archivos.get(nombre);

            // Crear nuevo documento
            Documento documento = new Documento();
            documento.setCedula(cedula);
            documento.setNombre(nombre);
            documento.setFecha(new Date());
            documento.setSistema(sistema);
            documento.setArchivo(decodificarBase64(archivo));

            // Almacenar
            em.persist(documento);

            // Agregar a la lista de Ids
            ids.add(documento.getId().toString());
        }

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("cedula", cedula);
        parametros.put("sistema", sistema);
        parametros.put("ids", String.join(",", ids));

        // Expiracion del Token
        Date expiracion = TokenTimeout.addMinutes(new Date(), DEFAULT_TIMEOUT);

        // Retorna el Token
        return servicioToken.generarToken(parametros, expiracion);
    }

    /**
     * Obtiene un documento mediante un token.
     *
     * @param token
     * @return
     * @throws TokenInvalidoException
     * @throws TokenExpiradoException
     */
    public Map<Long, String> obtenerDocumentos(String token) throws TokenInvalidoException, TokenExpiradoException {
        Map<String, Object> parametros = servicioToken.parseToken(token);
        String ids = (String) parametros.get("ids");
        logger.fine("ids=" + ids);

        Map<Long, String> archivos = new HashMap<>();

        for (String id : convertirEnList(ids)) {
            Long primaryKey = Long.parseLong(id);
            Documento documento = em.find(Documento.class, primaryKey);
            String archivo = codificarBase64(documento.getArchivo());
            archivos.put(primaryKey, archivo);
        }

        return archivos;
    }

    /**
     * 
     * @param token
     * @param archivoBase64
     * @return
     * @throws SistemaTransversalException
     */
    public void actualizarDocumentos(String token, Map<Long, String> archivos, String cedulaJson)
            throws TokenInvalidoException, CedulaInvalidaException, TokenExpiradoException, Base64InvalidoException {

        Map<String, Object> parametros = servicioToken.parseToken(token);

        String ids = (String) parametros.get("ids");
        logger.info("ids=" + ids);

        String cedulaToken = (String) parametros.get("cedula");
        logger.info("cedulaToken=" + cedulaToken);
        logger.info("cedulaJson=" + cedulaJson);

        if (!cedulaToken.equals(cedulaJson)) {
            throw new CedulaInvalidaException("La cedula " + cedulaJson + " es incorrecta");
        }

        String sistema = (String) parametros.get("sistema");
        URL url = servicioSistemaTransversal.buscarUrlSistema(sistema);
        logger.info("sistema=" + sistema + "; url=" + url);

        // Verificar si el sistema transversal está disponible
        if (!servicioSistemaTransversal.pingSistemaTransversal(url)) {
            String mensajeError = "El sistema transversal NO está disponible, el documento "
                    + " no fue enviado al sistema transversal";
            servicioLog.error("ServicioDocumento::actualizarDocumentos", mensajeError);
            logger.warning(mensajeError);
        }

        List<String> idList = convertirEnList(ids);

        if (idList.size() != archivos.size()) {
            throw new IllegalArgumentException("El token contiene " + idList.size()
                    + " archivos por procesar pero se enviaron solo " + archivos.size() + " archivos!");
        }

        for (String id : idList) {
            Long primaryKey = Long.parseLong(id);
            String archivoBase64 = archivos.get(primaryKey);

            if (archivoBase64 == null) {
                throw new IllegalArgumentException(
                        "El token contiene una lista de archivos distinta a los archivos solicitados para actualizar: "
                                + ids);
            }

            // Actualizar el archivo
            Documento documento = em.find(Documento.class, primaryKey);

            if (documento == null) {
                logger.severe("El documento " + primaryKey + " no existe en la base de datos");
                throw new IllegalArgumentException("El documento " + primaryKey + " no existe en la base de datos");
            }

            byte[] archivo = decodificarBase64(archivoBase64);

            // Obtener el nombre del firmante para almacenar el documento en el
            // sistema transversal
            String datosFirmante;

            try {
                datosFirmante = servicioValidacionPdf.getNombre(archivo);
            } catch (InvalidFormatException e) {
                throw new IllegalArgumentException("Error en la verificacion de firma", e);
            } catch (IOException e) {
                throw new IllegalArgumentException("Error en la verificacion de firma", e);
            } catch (OcspValidationException e) {
                throw new IllegalArgumentException("Error en la verificacion de firma", e);
            }

            try {
                servicioSistemaTransversal.almacenarDocumento(documento.getCedula(), documento.getNombre(),
                        archivoBase64, datosFirmante, url);
            } catch (SistemaTransversalException e) {
                String mensajeError = "No se pudo enviar el documento " + documento.getId()
                        + " al sistema transversal: ";
                servicioLog.error("ServicioDocumento::actualizarDocumentos", mensajeError);
                logger.log(Level.SEVERE, mensajeError);
            }

            // Eliminar el documento
            em.remove(documento);
        }
    }

    /**
     * Convierte una cadena de texto con una lista separada por comas de ints en una
     * List.
     * 
     * @param ids
     * @return
     */
    private List<String> convertirEnList(String ids) {
        // Separar por "espacio en blanco, coma, espacio en blanco":
        return Arrays.asList(ids.split("\\s*,\\s*"));
    }

    private byte[] decodificarBase64(String base64) throws Base64InvalidoException {
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new Base64InvalidoException(e);
        }
    }

    private String codificarBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
}