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

import ec.gob.firmadigital.servicio.util.ValidadorCertificadoDigital;
import io.rubrica.certificate.to.Certificado;
import io.rubrica.utils.Json;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.validation.constraints.NotNull;

import javax.ejb.EJB;

/**
 * Buscar en una lista de URLs permitidos para utilizar como API. Esto permite
 * federar la utilización de FirmaEC sobre otra infraestructura, consultando en
 * una lista de servidores permitidos.
 *
 * @author Christian Espinosa <christian.espinosa@mintel.gob.ec>, Misael Fernández
 */
@Stateless
public class ServicioValidarCertificadoDigital {

    /**
     * Busca un ApiUrl por URL.
     *
     * @param pkcs12
     * @param password
     * @return
     * @throws ServicioValidarCertificadoDigitalException
     */
    public String validarCertificadoDigital(@NotNull String pkcs12, @NotNull String password) throws ServicioValidarCertificadoDigitalException, Exception {
        String retorno = "";

        System.out.println("DATOS");
        System.out.println(pkcs12);
        System.out.println(password);

        ValidadorCertificadoDigital validadorCertificadoDigital = new ValidadorCertificadoDigital();
        Certificado certificado = validadorCertificadoDigital.validarCertificado(pkcs12, password);

        if (certificado != null) {
            return Json.generarJsonCertificado(certificado);
        }

        return retorno;
    }

}
