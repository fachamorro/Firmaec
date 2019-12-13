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

import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.validation.constraints.NotNull;

import ec.gob.firmadigital.servicio.model.ApiUrl;

/**
 * Buscar en una lista de URLs permitidos para utilizar como API. Esto permite
 * federar la utilización de FirmaEC sobre otra infraestructura, consultando en
 * una lista de servidores permitidos.
 *
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Stateless
public class ServicioApiUrl {

    @PersistenceContext
    private EntityManager em;

    private static final Logger logger = Logger.getLogger(ServicioApiUrl.class.getName());

    /**
     * Busca un ApiUrl por URL.
     *
     * @param url
     * @return
     * @throws ApiUrlNoEncontradoException
     */
    public ApiUrl buscarPorUrl(@NotNull String url) throws ApiUrlNoEncontradoException {
        try {
            TypedQuery<ApiUrl> query = em.createNamedQuery("ApiUrl.findByUrl", ApiUrl.class);
            query.setParameter("url", url);
            return query.getSingleResult();
        } catch (NoResultException e) {
            logger.info("URL no encontrado: " + url);
            throw new ApiUrlNoEncontradoException();
        } catch (NonUniqueResultException e) { 
            logger.severe("Se encontraron multiples URLs " + url);
            throw new ApiUrlNoEncontradoException("Se encontraron multiples URLs!");
        }
    }
}