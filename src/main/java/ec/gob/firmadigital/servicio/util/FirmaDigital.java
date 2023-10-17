package ec.gob.firmadigital.servicio.util;

import com.itextpdf.kernel.crypto.BadPasswordException;
import ec.gob.firmadigital.exceptions.CertificadoInvalidoException;
import ec.gob.firmadigital.exceptions.ConexionException;
import ec.gob.firmadigital.exceptions.DocumentoException;
import ec.gob.firmadigital.exceptions.EntidadCertificadoraNoValidaException;
import ec.gob.firmadigital.exceptions.HoraServidorException;
import ec.gob.firmadigital.exceptions.RubricaException;
import ec.gob.firmadigital.exceptions.SignatureVerificationException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Properties;
import ec.gob.firmadigital.model.Document;
import ec.gob.firmadigital.model.InMemoryDocument;
import ec.gob.firmadigital.sign.DigestAlgorithm;
import ec.gob.firmadigital.sign.PrivateKeySigner;
import ec.gob.firmadigital.sign.SignConstants;
import ec.gob.firmadigital.sign.pdf.PadesBasicSigner;
import ec.gob.firmadigital.sign.xades.XAdESSigner;
import ec.gob.firmadigital.utils.X509CertificateUtils;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

public class FirmaDigital {

    /**
     * Firmar un documento PDF usando un KeyStore y una clave.
     *
     * @param keyStore
     * @param alias
     * @param docByteArry
     * @param keyStorePassword
     * @param properties
     * @param api
     * @param base64
     * @return
     * @throws java.security.InvalidKeyException
     * @throws ec.gob.firmadigital.exceptions.EntidadCertificadoraNoValidaException
     * @throws ec.gob.firmadigital.exceptions.HoraServidorException
     * @throws java.security.UnrecoverableKeyException
     * @throws java.security.KeyStoreException
     * @throws ec.gob.firmadigital.exceptions.CertificadoInvalidoException
     * @throws java.io.IOException
     * @throws java.security.NoSuchAlgorithmException
     * @throws ec.gob.firmadigital.exceptions.RubricaException
     */
    final private String hashAlgorithm = "SHA512";

    public byte[] firmarPDF(KeyStore keyStore, String alias, byte[] docByteArry, char[] keyStorePassword, Properties properties, String api, String base64) throws
            BadPasswordException, InvalidKeyException, EntidadCertificadoraNoValidaException, HoraServidorException, UnrecoverableKeyException, KeyStoreException, CertificadoInvalidoException, IOException, NoSuchAlgorithmException, RubricaException, CertificadoInvalidoException, SignatureVerificationException, DocumentoException, ConexionException {
        byte[] signed = null;
        try {
            PrivateKey key = (PrivateKey) keyStore.getKey(alias, keyStorePassword);
            Certificate[] certChain = keyStore.getCertificateChain(alias);
            X509CertificateUtils x509CertificateUtils = new X509CertificateUtils();
            if (x509CertificateUtils.validarX509Certificate((X509Certificate) keyStore.getCertificate(alias), api, base64)) {//validación de firmaEC
                Document document = new InMemoryDocument(docByteArry);
                try (InputStream is = document.openStream()) {
                    // Crear un RubricaSigner para firmar el MessageDigest del documento
                    PrivateKeySigner signer = new PrivateKeySigner(key, DigestAlgorithm.forName(hashAlgorithm));
                    // Crear un PdfSigner para firmar el documento
                    PadesBasicSigner pdfSigner = new PadesBasicSigner(signer);
                    // Firmar el documento
                    signed = pdfSigner.sign(is, signer, certChain, properties);
                } catch (com.itextpdf.io.IOException ioe) {
                    throw new DocumentoException("El archivo no es PDF");
                }
            } else {
                throw new CertificadoInvalidoException(x509CertificateUtils.getError());
            }
            if (x509CertificateUtils.getError() != null) {
                throw new SignatureVerificationException(x509CertificateUtils.getError());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return signed;
    }

    /**
     * Firmar un documento XML usando un KeyStore y una clave.
     *
     * @param keyStore
     * @param alias
     * @param docByteArry
     * @param keyStorePassword
     * @param properties
     * @param api
     * @param base64
     * @return
     * @throws java.security.InvalidKeyException
     * @throws ec.gob.firmadigital.exceptions.EntidadCertificadoraNoValidaException
     * @throws ec.gob.firmadigital.exceptions.HoraServidorException
     * @throws java.security.UnrecoverableKeyException
     * @throws java.security.KeyStoreException
     * @throws ec.gob.firmadigital.exceptions.CertificadoInvalidoException
     * @throws java.io.IOException
     * @throws java.security.NoSuchAlgorithmException
     * @throws ec.gob.firmadigital.exceptions.RubricaException
     * @throws ec.gob.firmadigital.exceptions.SignatureVerificationException
     */
    public byte[] firmarXML(KeyStore keyStore, String alias, byte[] docByteArry, char[] keyStorePassword, Properties properties, String api, String base64) throws
            BadPasswordException, InvalidKeyException, EntidadCertificadoraNoValidaException, HoraServidorException, UnrecoverableKeyException, KeyStoreException, CertificadoInvalidoException, IOException, NoSuchAlgorithmException, RubricaException, CertificadoInvalidoException, SignatureVerificationException, ConexionException {
        byte[] signed = null;
        PrivateKey key = (PrivateKey) keyStore.getKey(alias, keyStorePassword);
        Certificate[] certChain = keyStore.getCertificateChain(alias);
        X509CertificateUtils x509CertificateUtils = new X509CertificateUtils();
        if (x509CertificateUtils.validarX509Certificate((X509Certificate) keyStore.getCertificate(alias), api, base64)) {//validación de firmaEC
            XAdESSigner signer = new XAdESSigner();
            signed = signer.sign(docByteArry, SignConstants.SIGN_ALGORITHM_SHA512WITHRSA, key, certChain, null, base64);
        } else {
            throw new CertificadoInvalidoException(x509CertificateUtils.getError());
        }
        if (x509CertificateUtils.getError() != null) {
            throw new SignatureVerificationException(x509CertificateUtils.getError());
        }
        return signed;
    }
}
