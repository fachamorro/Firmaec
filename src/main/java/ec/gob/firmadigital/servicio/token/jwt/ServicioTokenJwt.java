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

package ec.gob.firmadigital.servicio.token.jwt;

import static io.jsonwebtoken.SignatureAlgorithm.HS256;

import java.security.Key;
import java.util.Date;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.crypto.spec.SecretKeySpec;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jboss.logging.Logger;

import ec.gob.firmadigital.servicio.token.ServicioToken;
import ec.gob.firmadigital.servicio.token.TokenExpiradoException;
import ec.gob.firmadigital.servicio.token.TokenInvalidoException;
import ec.gob.firmadigital.servicio.util.Base64Util;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.MissingClaimException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.impl.crypto.MacProvider;

/**
 * Servicio para trabajar con tokens tipo JWT (https://jwt.io).
 *
 * La llave para firmar los tokens se genera al iniciar la aplicacion. Sin
 * embargo, se puede almacenar una version en Base64 de la llave en el archivo
 * de configuracion del servidor WildFly (standalone.xml), asi:
 *
 * <system-properties> <property name="jwt.key" value= "Jgh46..." />
 * </system-properties>
 *
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Singleton
@Startup
public class ServicioTokenJwt implements ServicioToken {

    private static final Logger logger = Logger.getLogger(ServicioTokenJwt.class.getName());

    /** Llave privada para firmar los tokens */
    private Key key;

    /** Algoritmo de firma HMAC por defecto */
    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = HS256;

    /**
     * Nombre de la propiedad de sistema que contiene la llave secreta, en
     * formato Base64
     */
    private static final String KEY_SYSTEM_PROPERTY = "jwt.key";

    @PostConstruct
    private void init() {
        logger.info("Inicializando llave secreta para tokens JWT...");
        String keyBase64 = System.getProperty(KEY_SYSTEM_PROPERTY);

        if (keyBase64 != null) {
            logger.info("Se encontro la propiedad de sistema \"jwt.key\"");

            try {
                // Cargar la llave secreta
                this.key = new SecretKeySpec(Base64Util.decode(keyBase64), "RAW");
                logger.info("Se creo una llave secreta a partir de la propiedad de sistema \"jwt.key\"");
                return;
            } catch (Throwable e) {
                logger.warn("ERROR: No se pudo crear una llave secreta a partir de la propiedad \"jwt.key\"", e);
            }
        }

        // Llave secreta autogenerada
        this.key = MacProvider.generateKey();
        logger.info("Se creo una llave secreta autogenerada");
    }

    /**
     * @see ec.gob.firmadigital.servicio.ServicioToken#generarToken(java.util.Map)
     */
    @Override
    public String generarToken(Map<String, Object> parametros) {
        return generarToken(parametros, null);
    }

    /**
     * @see ec.gob.firmadigital.servicio.ServicioToken#generarToken(java.util.Map,
     *      java.util.Date)
     */
    @Override
    public String generarToken(Map<String, Object> parametros, Date expiracion) {
        Claims claims = new DefaultClaims(parametros);
        return Jwts.builder().setClaims(claims).signWith(SIGNATURE_ALGORITHM, key).setExpiration(expiracion).compact();
    }

    /**
     * @see ec.gob.firmadigital.servicio.ServicioToken#parseToken(java.lang.String)
     */
    @Override
    public Map<String, Object> parseToken(String token) throws TokenInvalidoException, TokenExpiradoException {
        try {
            return Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
        } catch (MalformedJwtException | SignatureException | MissingClaimException e) {
            throw new TokenInvalidoException(e);
        } catch (ExpiredJwtException e) {
            throw new TokenExpiradoException(e);
        }
    }
}