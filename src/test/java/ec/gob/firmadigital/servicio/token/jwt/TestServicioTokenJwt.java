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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.Test;

/**
 * Pruebas de unidad de ServicioTokenJwt.
 *
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
public class TestServicioTokenJwt {

	@Test
	public void testParseToken() throws Exception {
		ServicioTokenJwt servicioToken = new ServicioTokenJwt();
		servicioToken.init();

		Map<String, Object> parametros = new HashMap<>();
		parametros.put("a", 1);
		String token = servicioToken.generarToken(parametros);

		Map<String, Object> parametros2 = servicioToken.parseToken(token);
		assertEquals(parametros, parametros2);
	}

	@Test
	public void testSecretKey() throws Exception {
		SecretKey secretKey1 = ServicioTokenJwt.generarLlaveSecreta();
		String keyBase64 = ServicioTokenJwt.codificarLlaveSecreta(secretKey1);
		SecretKey secretKey2 = ServicioTokenJwt.decodificarLlaveSecreta(keyBase64);
		assertTrue(secretKey1.equals(secretKey2));
	}
}