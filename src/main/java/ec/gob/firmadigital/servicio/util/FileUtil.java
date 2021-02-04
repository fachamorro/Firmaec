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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import javax.xml.bind.DatatypeConverter;

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

    public static String hashMD5(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(texto.getBytes("UTF-8"));
            byte[] digest = md.digest();
            return DatatypeConverter.printHexBinary(digest).toLowerCase();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
