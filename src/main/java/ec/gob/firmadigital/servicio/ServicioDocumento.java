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

import java.security.KeyStoreException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.soap.SOAPException;

import io.rubrica.ocsp.OcspValidationException;

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
    private ServicioSistema servicioSistema;

    @EJB
    private ServicioSistemaTransversal servicioSistemaTransversal;

    @PersistenceContext(unitName = "FirmaDigitalDS")
    private EntityManager entityManager;

    /** Base 64 decoder */
    private static final Decoder BASE64_DECODER = Base64.getDecoder();

    /** Base 64 encoder */
    private static final Encoder BASE64_ENCODER = Base64.getEncoder();

    private static final Logger logger = Logger.getLogger(ServicioDocumento.class.getName());

    /**
     * Almacena un documento desde un Sistema Transversal.
     * 
     * @param cedula
     * @param archivoBase64
     * @return el token para poder buscar el documento
     */
    @Deprecated
    public String crearDocumento(@NotNull String cedula, @NotNull String sistema, @NotNull String nombre,
            @Size(min = 1) String archivoBase64) throws IllegalArgumentException {

        // Decodificar el archivo
        byte[] archivo = BASE64_DECODER.decode(archivoBase64);

        // Crear nuevo documento
        Documento documento = new Documento();
        documento.setCedula(cedula);
        documento.setSistema(sistema);
        documento.setNombre(nombre);
        documento.setFecha(new Date());
        documento.setArchivo(archivo);

        // Almacenar
        entityManager.persist(documento);

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("id", documento.getId().toString());
        parametros.put("cedula", cedula);
        parametros.put("sistema", sistema);

        // Expiracion del Token
        Date expiracion = Timeout.addMinutes(new Date(), Timeout.DEFAULT_TIMEOUT);

        // Retorna el Token
        return servicioToken.generarToken(parametros, expiracion);
    }

    public String crearDocumentos(@NotNull String cedula, @NotNull String sistema, Map<String, String> documentos)
            throws IllegalArgumentException {

        List<String> ids = new ArrayList<>();
        Set<String> nombres = documentos.keySet();

        for (String nombre : nombres) {
            String base64 = documentos.get(nombre);

            // Decodificar el archivo
            byte[] archivo = BASE64_DECODER.decode(base64);

            // Crear nuevo documento
            Documento documento = new Documento();
            documento.setCedula(cedula);
            documento.setSistema(sistema);
            documento.setNombre(nombre);
            documento.setFecha(new Date());
            documento.setArchivo(archivo);

            // Almacenar
            entityManager.persist(documento);

            // Agregar a la lista de Ids
            ids.add(documento.getId().toString());
        }

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("cedula", cedula);
        parametros.put("sistema", sistema);
        parametros.put("ids", String.join(",", ids));

        // Expiracion del Token
        Date expiracion = Timeout.addMinutes(new Date(), Timeout.DEFAULT_TIMEOUT);

        // Retorna el Token
        return servicioToken.generarToken(parametros, expiracion);
    }

    /**
     * Obtiene un documento mediante un token.
     * 
     * @param token
     * @return el documento en Base64
     */
    @Deprecated
    public String obtenerDocumento(String token) throws TokenInvalidoException, TokenExpiradoException {
        Long id = Long.parseLong((String) servicioToken.parseTokenParameter(token, "id"));
        Documento documento = entityManager.find(Documento.class, id);
        return BASE64_ENCODER.encodeToString(documento.getArchivo());
    }

    /**
     * Obtiene un documento mediante un token.
     * 
     * @param token
     * @return el documento en Base64
     */
    public Map<Long, String> obtenerDocumentos(String token) throws TokenInvalidoException, TokenExpiradoException {
        Map<Long, String> documentos = new HashMap<>();
        String ids = (String) servicioToken.parseTokenParameter(token, "ids");
        logger.info("ids=" + ids);

        for (String id : convertirEnList(ids)) {
            Long pk = Long.parseLong(id);
            Documento documento = entityManager.find(Documento.class, pk);
            String base64 = BASE64_ENCODER.encodeToString(documento.getArchivo());
            documentos.put(pk, base64);
        }

        return documentos;
    }

    /**
     * 
     * @param token
     * @param documentoBase64
     * @return
     */
    @Deprecated
    public void actualizarDocumento(String token, String documentoBase64)
            throws IllegalArgumentException, TokenInvalidoException, TokenExpiradoException {
        long id = (int) servicioToken.parseTokenParameter(token, "id");
        Documento documento = entityManager.find(Documento.class, id);

        byte[] archivo = BASE64_DECODER.decode(documentoBase64);
        documento.setArchivo(archivo);

        String nombreFirmante;
        try {
            nombreFirmante = ServicioValidacionPdf.getNombre(archivo);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (OcspValidationException e) {
            throw new RuntimeException(e);
        }

        // Actualizar el documento en el sistema transversal
        try {
            String resultado = actualizarSistemaTransversal(documento, nombreFirmante);
        } catch (SistemaNoEncontradoException e) {
            throw new RuntimeException(e);
        } catch (SOAPException e) {
            throw new RuntimeException(e);
        }

        // Borrar documento
        entityManager.remove(documento);
    }

    /**
     * 
     * @param token
     * @param archivoBase64
     * @return
     */
    public void actualizarDocumentos(String token, Map<Long, String> documentos)
            throws IllegalArgumentException, TokenInvalidoException, TokenExpiradoException {
        String ids = (String) servicioToken.parseTokenParameter(token, "ids");
        logger.info("ids=" + ids);

        for (String id : convertirEnList(ids)) {
            Long pk = Long.parseLong(id);
            String documentoBase64 = documentos.get(pk);

            // Actualizar el archivo
            Documento documento = entityManager.find(Documento.class, pk);
            byte[] archivo = BASE64_DECODER.decode(documentoBase64);
            documento.setArchivo(archivo);

            String nombreFirmante;

            try {
                nombreFirmante = ServicioValidacionPdf.getNombre(archivo);
            } catch (SignatureException e) {
                throw new RuntimeException(e);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            } catch (OcspValidationException e) {
                throw new RuntimeException(e);
            }

            // Actualizar el documento en el sistema transversal
            try {
                String resultado = actualizarSistemaTransversal(documento, nombreFirmante);
                logger.info("Resultado de actualizar en el sistema transversal:" + resultado);
            } catch (SistemaNoEncontradoException e) {
                throw new RuntimeException(e);
            } catch (SOAPException e) {
                throw new RuntimeException(e);
            }

            // Borrar documento
            entityManager.remove(documento);
        }
    }

    private String actualizarSistemaTransversal(Documento documento, String nombreFirmante)
            throws SistemaNoEncontradoException, SOAPException {
        String url = servicioSistema.buscarUrlSistema(documento.getSistema());
        return servicioSistemaTransversal.almacenarDocumento(url, documento.getCedula(), documento.getNombre(),
                BASE64_ENCODER.encodeToString(documento.getArchivo()), nombreFirmante);
    }

    /**
     * Convierte una cadena de texto con una lista separada por comas de ints en
     * una List.
     * 
     * @param ids
     * @return
     */
    private List<String> convertirEnList(String ids) {
        return Arrays.asList(ids.split("\\s*,\\s*"));
    }
}