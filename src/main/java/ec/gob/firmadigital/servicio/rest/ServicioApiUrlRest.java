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

package ec.gob.firmadigital.servicio.rest;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import ec.gob.firmadigital.servicio.ApiUrlNoEncontradoException;
import ec.gob.firmadigital.servicio.ServicioApiUrl;
import ec.gob.firmadigital.servicio.model.ApiUrl;

/**
 * Servicio REST para verificar si existe un API URL.
 *
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Stateless
@Path("/apiurl")
public class ServicioApiUrlRest {

    @EJB
    private ServicioApiUrl servicioApiUrl;

    @GET
    @Path("{url}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response buscarUrl(@PathParam("url") String url) {
        try {
            ApiUrl apiUrl = servicioApiUrl.buscarPorUrl(url);
            return Response.ok(apiUrl).build();
        } catch (ApiUrlNoEncontradoException e) {
            return Response.status(Status.NOT_FOUND).build();
        }
    }
}