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

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MissingClaimException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.impl.crypto.MacProvider;

/**
 * Servicio para trabajar con tokens tipo JWT (https://jwt.io)
 * 
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Stateless
public class ServicioToken {

    /**
     * Llave privada para firmar los tokens, se genera cada vez que inicia la
     * aplicacion
     */
    private static final Key KEY = MacProvider.generateKey();

    /** Algoritmo de firma HMAC por defecto */
    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256;

    /**
     * Generar un token JWT sin expiracion.
     * 
     * @param id
     * @return
     */
    public String generarToken(Map<String, Object> parametros) {
        return generarToken(parametros, null);
    }

    /**
     * Generar un token JWT con tiempo de expiracion
     * 
     * @param id
     * @param expiracion
     * @return
     */
    public String generarToken(Map<String, Object> parametros, Date expiracion) {
        Claims claims = new DefaultClaims(parametros);
        return Jwts.builder().setClaims(claims).signWith(SIGNATURE_ALGORITHM, KEY).setExpiration(expiracion).compact();
    }

    /**
     * Analizar los contenidos de un token para sacar la información necesaria
     * para procesar un documento.
     * 
     * @param token
     * @return
     * @throws TokenInvalidoException
     * @throws TokenExpiradoException
     */
    public Map<String, Object> parseToken(String token) throws TokenInvalidoException, TokenExpiradoException {
        try {
            return Jwts.parser().setSigningKey(KEY).parseClaimsJws(token).getBody();
        } catch (SignatureException e) {
            throw new TokenInvalidoException(e);
        } catch (MissingClaimException e) {
            throw new TokenInvalidoException(e);
        } catch (ExpiredJwtException e) {
            throw new TokenExpiradoException(e);
        }
    }

    public Object parseTokenParameter(String token, String parametro)
            throws TokenInvalidoException, TokenExpiradoException {
        Map<String, Object> parametros = parseToken(token);
        return parametros.get(parametro);
    }
}