//package ec.gob.firmadigital.servicio.util;
//
//import java.io.IOException;
//import java.security.InvalidKeyException;
//import java.security.KeyStore;
//import java.security.KeyStoreException;
//import java.security.cert.X509Certificate;
//import java.util.Date;
//
//import io.rubrica.certificate.CertEcUtils;
//import io.rubrica.certificate.to.DatosUsuario;
//import io.rubrica.exceptions.CertificadoInvalidoException;
//import io.rubrica.exceptions.EntidadCertificadoraNoValidaException;
//import io.rubrica.exceptions.HoraServidorException;
//import io.rubrica.utils.TiempoUtils;
//import io.rubrica.utils.UtilsCrlOcsp;
//
///**
// * Utilidades para X509Certificate.
// *
// * @author mfernandez
// */
//public class X509CertificateUtils {
//
//    private String revocado = null;
//    private boolean caducado = false;
//    private boolean desconocido = false;
//
//    public X509CertificateUtils() {
//    }
//
//    public String getCedula(KeyStore keyStore, String alias) {
//        try {
//            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
//            DatosUsuario datosUsuario = CertEcUtils.getDatosUsuarios(certificate);
//            return datosUsuario.getCedula();
//        } catch (KeyStoreException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public boolean validarX509Certificate(X509Certificate x509Certificate, String apiUrl) throws EntidadCertificadoraNoValidaException, InvalidKeyException, CertificadoInvalidoException, IOException, HoraServidorException {
//        boolean retorno = false;
//        int diasAnticipacion = 30;
//        try {
//            if (x509Certificate != null) {
//                String apiUrlFecha = null;
//                String apiUrlRevocado = null;
//                if (apiUrl != null) {
//                    apiUrlFecha = apiUrl + "/fecha-hora";
//                    apiUrlRevocado = apiUrl + "/certificado/fechaRevocado";
//                }
//                Date fechaHora = TiempoUtils.getFechaHora(apiUrlFecha);
//
//                Date fechaRevocado = UtilsCrlOcsp.validarFechaRevocado(x509Certificate, apiUrlRevocado);
//                if (fechaRevocado != null && fechaRevocado.compareTo(fechaHora) <= 0) {
//                    revocado = fechaRevocado.toString();
//                }
//                if (fechaHora.compareTo(x509Certificate.getNotBefore()) <= 0 || fechaHora.compareTo(x509Certificate.getNotAfter()) >= 0) {
//                    caducado = true;
//                } else {
//                    java.util.Calendar calendarRecordatorio = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("America/Guayaquil"));
//                    calendarRecordatorio.setTime(x509Certificate.getNotAfter());
//                    calendarRecordatorio.add(java.util.Calendar.DATE, -diasAnticipacion);
//                    if (calendarRecordatorio.getTime().compareTo(fechaHora) <= 0) {
//                        @SuppressLint("SimpleDateFormat") java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                        Toast.makeText(context, "Certificado digital caducará el " + simpleDateFormat.format(x509Certificate.getNotAfter().getTime()), Toast.LENGTH_LONG).show();
//                    }
//                }
//                if (!io.rubrica.utils.Utils.verifySignature(x509Certificate)) {
//                    desconocido = true;
//                }
//                if ((revocado != null) || caducado || desconocido) {
//                    Toast.makeText(context, "Certificado Inválido", Toast.LENGTH_LONG).show();
//                } else {
//                    retorno = true;
//                }
//            }
//        } catch (Exception e) {//no funciona con InvalidKeyException
//            e.printStackTrace();
//            resultado = "No es posible procesar el documento desde dispositivo móvil\nIntentar en FirmaEC de Escritorio";
//        }
//        return retorno;
//    }
//}
