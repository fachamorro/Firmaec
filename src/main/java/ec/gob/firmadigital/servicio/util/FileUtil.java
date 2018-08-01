package ec.gob.firmadigital.servicio.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.Tika;

/**
*
* @author bolivar.murillo msp 
*/
public class FileUtil {
	 public static String getMimeType(byte[] data) {

	        Tika tika = new Tika();

	        String mimeType = "";

	        try (InputStream is = new ByteArrayInputStream(data)) {
	            mimeType = tika.detect(is);
	            mimeType = mimeType == null ? "" : mimeType;            
	        } catch (IOException e) {            
	            System.out.println("No se pudo determinar el tipo de archivo");
	            mimeType = "";
	        }
	        return mimeType;
	    }
}
