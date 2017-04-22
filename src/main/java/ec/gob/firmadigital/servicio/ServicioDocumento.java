/*
 * Firma Digital: Servicio
 * Copyright (C) 2017 Secretaría Nacional de la Administración Pública
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

import java.util.Date;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Servicio REST para almacenar, actualizar y obtener documentos desde los
 * sistemas transversales y la aplicación en api.firmadigital.gob.ec
 * 
 * FIXME: Proteger este metodo con usuario/password para cada sistema
 * transversal
 * 
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Stateless
@Path("/documentos")
public class ServicioDocumento {

    private ServicioToken servicioToken = new ServicioToken();

    @PersistenceContext(unitName = "FirmaDigitalDS")
    private EntityManager entityManager;

    /**
     * Minutos antes de que el Token expire
     */
    private static int MINUTOS_EXPIRACION = 5; // FIXME: Sacar esto de aqui:

    /***
     * Almacena un documento desde un Sistema Transversal.
     * 
     * @param base64
     *            el documento en Base64
     * @return el token para poder buscar el documento
     */
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String crearDocumento(String base64) {
        // Crear nuevo documento
        Documento archivo = new Documento();
        archivo.setContenido(base64);

        // Almacenar
        entityManager.persist(archivo);

        // Expiracion del Token en 5 minutos
        Date expiracion = addMinutes(new Date(), MINUTOS_EXPIRACION);

        // Retorna el JWT
        return servicioToken.generarToken(archivo.getId(), expiracion);
    }

    /**
     * Obtiene un documento mediante un token.
     * 
     * @param token
     * @return el documento en Base64
     */
    @GET
    @Path("{token}")
    @Produces(MediaType.TEXT_PLAIN)
    public String obtenerDocumento(@PathParam("token") String token) {
        try {
            long id = servicioToken.parseToken(token);
            Documento archivo = entityManager.find(Documento.class, id);

            return archivo.getContenido();
        } catch (TokenInvalidoException | TokenExpiradoException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    /**
     * Almacenar el documento firmado.
     * 
     * @param id
     *            el identificador del
     * @param base64
     * @return
     */
    @PUT
    @Path("{token}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response actualizarDocumento(@PathParam("token") String token, String base64) {
        try {
            long id = servicioToken.parseToken(token);
            Documento archivo = entityManager.find(Documento.class, id);
            archivo.setContenido(base64);

            return Response.status(Status.OK).build();
        } catch (TokenInvalidoException | TokenExpiradoException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    /**
     * Agregar una cantidad de minutos a una hora dada.
     * 
     * @param date
     * @param minutes
     * @return
     */
    private Date addMinutes(Date date, int minutes) {
        long time = date.getTime() + (minutes * 60 * 1000);
        return new Date(time);
    }
}