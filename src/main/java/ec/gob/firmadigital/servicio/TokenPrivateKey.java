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
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;

/**
 * Clase utilitaria para manejar la llave privada usada para firmar un token.
 * 
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
public class TokenPrivateKey {

    // FIXME: Sacar esto a un archivo, eliminado temporalmente
    private static final String ENCODED_KEY = "xxx";

    /** Algoritmo de firma HMAC por defecto */
    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS512;

    public static Key obtenerKey() {
        byte[] decodedKey = Base64.getDecoder().decode(ENCODED_KEY);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, SIGNATURE_ALGORITHM.getJcaName());
    }

    public static Key generarKey() {
        return MacProvider.generateKey();
    }

    public static String generarKeyBase64() {
        Key key = generarKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static SignatureAlgorithm getSignatureAlgorithm() {
        return SIGNATURE_ALGORITHM;
    }
}