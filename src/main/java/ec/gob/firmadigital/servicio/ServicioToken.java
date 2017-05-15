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

import java.security.Key;
import java.util.Date;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MissingClaimException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.impl.crypto.MacProvider;

/**
 * Servicio para trabajar con tokens tipo JWT (https://jwt.io)
 * 
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
public class ServicioToken {

    /**
     * Llave privada para firmar los tokens, se genera cada vez que inicia la
     * aplicacion
     */
    private static final Key KEY = MacProvider.generateKey();

    /** Algoritmo de firma HMAC por defecto */
    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS512;

    /**
     * Generar un token JWT
     * 
     * @param id
     * @return
     */
    public String generarToken(long id) {
        return generarToken(id, null);
    }

    /**
     * Generar un token JWT con tiempo de expiracion
     * 
     * @param id
     * @param expiracion
     * @return
     */
    public String generarToken(long id, Date expiracion) {
        return Jwts.builder().claim("id", id).signWith(SIGNATURE_ALGORITHM, KEY).setExpiration(expiracion).compact();
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
    public int parseToken(String token) throws TokenInvalidoException, TokenExpiradoException {
        try {
            return (int) Jwts.parser().setSigningKey(KEY).parseClaimsJws(token).getBody().get("id");
        } catch (SignatureException e) {
            throw new TokenInvalidoException(e);
        } catch (MissingClaimException e) {
            throw new TokenInvalidoException(e);
        } catch (ExpiredJwtException e) {
            throw new TokenExpiradoException(e);
        }
    }
}