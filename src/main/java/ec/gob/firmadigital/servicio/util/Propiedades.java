/*
 * Copyright (C) 2020 
 * Authors: Ricardo Arguello, Misael Fernández
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.*
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ec.gob.firmadigital.servicio.util;

import io.rubrica.exceptions.HoraServidorException;
import java.io.IOException;
import java.util.Properties;

import io.rubrica.sign.pdf.PDFSignerItext;
import io.rubrica.sign.pdf.RectanguloUtil;
import io.rubrica.utils.TiempoUtils;

public class Propiedades {

    public static Properties propiedades(String version, String llx, String lly, String pagina, String tipoEstampa, String url, String fechaHora, String base64) throws IOException, HoraServidorException {
        Properties properties = new Properties();
        properties.setProperty(PDFSignerItext.SIGNING_LOCATION, "");
        properties.setProperty(PDFSignerItext.SIGNING_REASON, "Firmado digitalmente con FirmaEC mobile " + version);
        if (fechaHora == null) {
            properties.setProperty(PDFSignerItext.SIGN_TIME, TiempoUtils.getFechaHoraServidor(url != null ? url + "/fecha-hora" : null, base64));
        } else {
            properties.setProperty(PDFSignerItext.SIGN_TIME, fechaHora);
        }
        properties.setProperty(PDFSignerItext.INFO_QR, "VALIDAR CON: www.firmadigital.gob.ec\n" + "version");
        if (llx != null) {
            properties.setProperty(RectanguloUtil.POSITION_ON_PAGE_LOWER_LEFT_X, llx);
        }
        if (lly != null) {
            properties.setProperty(RectanguloUtil.POSITION_ON_PAGE_LOWER_LEFT_Y, lly);
        }
        if (pagina != null) {
            properties.setProperty(PDFSignerItext.LAST_PAGE, pagina);
        }
        if (pagina != null) {
            properties.setProperty(PDFSignerItext.TYPE_SIG, tipoEstampa);
        } else {
            properties.setProperty(PDFSignerItext.TYPE_SIG, "information2"); //no funciona con QR
        }
        return properties;
    }
}
