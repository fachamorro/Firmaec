package ec.gob.firmadigital.servicio.util;


import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Properties;

import io.rubrica.model.Document;
import io.rubrica.model.InMemoryDocument;
import io.rubrica.sign.DigestAlgorithm;
import io.rubrica.sign.PrivateKeySigner;
import io.rubrica.sign.pdf.PadesBasicSigner;

public class FirmaDigitalPdf {

    /**
     * Firmar un documento PDF usando un KeyStore y una clave.
     */
    final private String hashAlgorithm = "SHA512";

    public byte[] firmar(KeyStore keyStore, String alias, byte[] docByteArry, char[] keyStorePassword, Properties properties, String api) {
        byte[] signed = null;
        try {
            PrivateKey key = (PrivateKey) keyStore.getKey(alias, keyStorePassword);
            Certificate[] certChain = keyStore.getCertificateChain(alias);
            X509CertificateUtils x509CertificateUtils = new X509CertificateUtils();
            if (x509CertificateUtils.validarX509Certificate((X509Certificate) keyStore.getCertificate(alias), api)) {//validación de firmaEC
                Document document = new InMemoryDocument(docByteArry);
                try (InputStream is = document.openStream()) {
                    // Crear un RubricaSigner para firmar el MessageDigest del documento
                    PrivateKeySigner signer = new PrivateKeySigner(key, DigestAlgorithm.forName(hashAlgorithm));
                    // Crear un PdfSigner para firmar el documento
                    PadesBasicSigner pdfSigner = new PadesBasicSigner(signer);
                    // Firmar el documento
                    signed = pdfSigner.sign(is, signer, certChain, properties);
                } catch (IOException e) {
//                    Toast.makeText(context, "Excepción no conocida: " + e, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } catch (com.itextpdf.io.IOException ioe) {
//                    Toast.makeText(context, "El archivo no es PDF", Toast.LENGTH_LONG).show();
                }
            } else {
//                Toast.makeText(context, "Certificado no válido", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        } catch (BadPasswordException bpe) {
//            Toast.makeText(context, "Documento protegido con contraseña", Toast.LENGTH_LONG).show();
//        } catch (InvalidKeyException ie) {
//            Toast.makeText(context, "Problemas al abrir el documento", Toast.LENGTH_LONG).show();
//        } catch (EntidadCertificadoraNoValidaException ecnve) {
//            Toast.makeText(context, "Certificado no válido", Toast.LENGTH_LONG).show();
//        } catch (HoraServidorException hse) {
//            Toast.makeText(context, "Problemas en la red\nIntente nuevamente o verifique su conexión", Toast.LENGTH_LONG).show();
//        } catch (UnrecoverableKeyException uke) {
//            Toast.makeText(context, "Certificado Corrupto", Toast.LENGTH_LONG).show();
//        } catch (KeyStoreException kse) {
//            Toast.makeText(context, "No se encontró archivo o la contraseña es inválida.", Toast.LENGTH_LONG).show();
//        } catch (CertificadoInvalidoException | IOException | NoSuchAlgorithmException e) {
//            Toast.makeText(context, "Excepción no conocida: " + e, Toast.LENGTH_LONG).show();
//            //e.printStackTrace();
//        }
        return signed;
    }
}
