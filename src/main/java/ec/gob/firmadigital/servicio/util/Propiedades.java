package ec.gob.firmadigital.servicio.util;

import java.io.IOException;
import java.util.Properties;

import io.rubrica.sign.pdf.PDFSignerItext;
import io.rubrica.sign.pdf.RectanguloUtil;
import io.rubrica.utils.TiempoUtils;

public class Propiedades {

    public static Properties propiedades(String version, String llx, String lly, String pagina, String tipoEstampa, String url, String fechaHora) throws IOException {
        Properties properties = new Properties();
        properties.setProperty(PDFSignerItext.SIGNING_LOCATION, "");
        properties.setProperty(PDFSignerItext.SIGNING_REASON, "Firmado digitalmente con FirmaEC mobile " + version);
        if (fechaHora == null) {
            properties.setProperty(PDFSignerItext.SIGN_TIME, TiempoUtils.getFechaHoraServidor(url != null ? url + "/fecha-hora" : null));
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
