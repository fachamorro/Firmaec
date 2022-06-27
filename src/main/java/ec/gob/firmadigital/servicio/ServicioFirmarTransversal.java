///*
// * Firma Digital: Servicio
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package ec.gob.firmadigital.servicio;
//
//import ec.gob.firmadigital.firmaec_app.exceptions.ProtocoloInvalidoException;
//import ec.gob.firmadigital.servicio.util.FirmaDigitalPdf;
//import ec.gob.firmadigital.servicio.util.JsonProcessor;
//import ec.gob.firmadigital.servicio.util.Propiedades;
//import ec.gob.firmadigital.servicio.util.ProtocoloFirmaDigital;
//import io.rubrica.utils.X509CertificateUtils;
//import java.io.BufferedReader;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.UnsupportedEncodingException;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.net.URLEncoder;
//import java.security.KeyStore;
//import java.util.Base64;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Properties;
//
//import javax.ejb.Stateless;
//
///**
// * Buscar en una lista de URLs permitidos para utilizar como API. Esto permite
// * federar la utilización de FirmaEC sobre otra infraestructura, consultando en
// * una lista de servidores permitidos.
// *
// * @author Christian Espinosa <christian.espinosa@mintel.gob.ec>, Misael
// * Fernández
// */
//@Stateless
//public class ServicioFirmarTransversal {
//
//    String resultado = null;
//
//    private String versionFirmaEC;
//    private String sistema;
//    private String operacion;
//    private String tokenJwt;
//    private String tipoCertificado;
//    private boolean esPreproduccion;
//    private boolean esDesarrollo;
//    private String formato;
//
//    private String llx, lly, urx, ury, pagina, estampado, tamano, razon, url;
//    private String uuid;
//
//    private String restServiceUrl;
//    private KeyStore keyStore;
//    private char[] keyStorePassword = new char[0];
//    private String alias;
//
//    private final String REST_SERVICE_URL_PREPRODUCCION = "https://impapi.firmadigital.gob.ec/api";
//    private final String REST_SERVICE_URL_DESARROLLO = "http://desapi.firmadigital.gob.ec/api";
//    private final String REST_SERVICE_URL_PRODUCCION = "https://api.firmadigital.gob.ec/api";
//    private final int TIME_OUT = 5000; //set timeout to 5 seconds
//    private final int BUFFER_SIZE = 8192;
//
//    public String firmarTransversal(String versionFirmaEC, String uri, String tipoCertificado, String formato, KeyStore keyStore, String keyStorePassword, String alias) throws Exception {
//        this.versionFirmaEC = versionFirmaEC;
//        this.resultado = null;
//        this.tipoCertificado = tipoCertificado;
//        this.formato = formato;
//        this.keyStore = keyStore;
//        this.keyStorePassword = keyStorePassword.toCharArray();
//        this.alias = alias;
//        // Protocolo enviado por el cliente
//        ProtocoloFirmaDigital protocolo = null;
//        try {
//            protocolo = new ProtocoloFirmaDigital(uri);
//        } catch (ProtocoloInvalidoException e) {
//            resultado = "El protocolo es inválido";
//            System.exit(1);
//        }
//
//        assert protocolo != null;
//        sistema = protocolo.getSistema();
//        if (sistema == null) {
//            resultado = "No se incluye un sistema";
//            System.exit(1);
//        }
//
//        operacion = protocolo.getOperacion();
//        if (operacion == null) {
//            resultado = "No se incluye una operacion";
//            System.exit(1);
//        }
//
//        // Parametros del URI
//        Map<String, String> parametros = protocolo.getParametros();
//
//        // Token JWT
//        tokenJwt = parametros.get("token");
//        if (tokenJwt == null) {
//            resultado = "No se incluye un token";
//            System.exit(1);
//        }
//
//        // Posición de la firma
//        llx = parametros.get("llx");
//        lly = parametros.get("lly");
//        urx = parametros.get("urx");
//        ury = parametros.get("ury");
//        pagina = parametros.get("pagina");
//        estampado = parametros.get("estampado");
//        tamano = parametros.get("tamano");
//        razon = parametros.get("razon");
//        url = parametros.get("url");
//        uuid = "msp";
//
//        // Parametros opcionales
//        esPreproduccion = Boolean.parseBoolean(parametros.get("pre"));
//        esDesarrollo = Boolean.parseBoolean(parametros.get("des"));
//        ambiente();
//
//        formato = parametros.get("format");
//        if (formato == null) {
//            formato = "pdf";
//        }
//
//        //en caso de ser firma federada
//        if (url != null) {
//            // comprobar api
//            comprobarApi(url);
//        }
//
//        Map<Long, byte[]> documentosFirmados;
//        try {
//            //bajar documentos a firmar
//            String json = bajarDocumentos();
//            if (json != null) {
//                //firmando documentos descargados
//                documentosFirmados = firmarDocumentos(json);
//                // Cedula de identidad contenida en el certificado:
//                X509CertificateUtils x509CertificateUtils = new X509CertificateUtils();
//                String cedula = x509CertificateUtils.getCedula(keyStore, alias);
//                // Actualizar documentos
//                actualizarDocumentos(tokenJwt, documentosFirmados, cedula);
//            }
//        } catch (java.security.UnrecoverableKeyException e) {
//            resultado = "Certificado Corrupto";
//            throw e;
//        } catch (Exception ex) {
//            if (ex.getClass() == java.net.UnknownHostException.class
//                    || ex.getClass() == java.net.NoRouteToHostException.class
//                    || ex.getClass() == java.net.SocketTimeoutException.class
//                    || ex.getClass() == java.net.SocketException.class
//                    || ex.getClass() == java.net.ConnectException.class) {
//                resultado = "No es posible procesar documento(s), revise su conexión a internet.";
//            }
//            if (ex.getClass() == java.io.IOException.class) {
//                resultado = "Problemas con los servicios web.\nComuníquese con el administrador de su sistema.";
//            }
//            throw ex;
//        }
//
//        return resultado;
//    }
//
//    private void comprobarApi(String apiUrl) {
//        String jsonApi = JsonProcessor.buildJsonApi(this.sistema, apiUrl);
//        try {
//            jsonApi = Base64.getEncoder().encodeToString((URLEncoder.encode(jsonApi, "UTF-8")).getBytes());
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        try {
//            URL url = new URL(restServiceUrl + "/url/" + jsonApi);
//            HttpURLConnection con = (HttpURLConnection) url.openConnection();
//            con.setRequestMethod("GET");
//            con.setConnectTimeout(TIME_OUT);
//
//            int responseCode = con.getResponseCode();
//            if (responseCode >= 300 && responseCode < 400) {
//                con = (HttpURLConnection) new URL(con.getHeaderField("Location")).openConnection();
//                con.setConnectTimeout(TIME_OUT);
//                responseCode = con.getResponseCode();
//            }
//            if (responseCode >= 400) {
//                resultado = "Problemas con los servicios web.";
//                throw new RuntimeException("Failed : HTTP error code : " + responseCode);
//            }
//            byte[] buffer = new byte[BUFFER_SIZE];
//            int count;
//            String jsonRespuesta;
//            // Leer la respuesta del sw
//            try (InputStream in = con.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//                while ((count = in.read(buffer)) != -1) {
//                    out.write(buffer, 0, count);
//                }
//                jsonRespuesta = out.toString();
//                System.out.println("resultado=" + jsonRespuesta);
//            }
//            con.disconnect();
//            //validar respuesta (apiUrl)
//            if (!jsonRespuesta.equals("Url habilitada")) {
////                // make a handler that throws a runtime exception when a message is received
////                @SuppressLint("HandlerLeak") final Handler handler = new Handler() {
////                    @Override
////                    public void handleMessage(Message message) {
////                        throw new RuntimeException();
////                    }
////                };
////                // dialog and show it
////                AlertDialog.Builder builder = new AlertDialog.Builder(context, THEME_DEVICE_DEFAULT_LIGHT);
////                builder.setTitle("Alerta");
//                if (jsonRespuesta.isEmpty()) {
//                    resultado = "Revisar el estado de la url registrada\n" + apiUrl + "\nDesea continuar firmando bajo su propio riesgo?";
//                } else {
//                    resultado = jsonRespuesta + " y sin autorización de FirmaEC:\n" + apiUrl + "\nDesea continuar firmando bajo su propio riesgo?";
//                }
////                builder.setMessage(resultado);
////                builder.setPositiveButton("Aceptar", (dialog, whichButton) -> handler.sendMessage(handler.obtainMessage()));
////                builder.setNegativeButton("Cancelar", (dialog, whichButton) -> {
////                    handler.sendMessage(handler.obtainMessage());
////                    Toast.makeText(context, "Cancelado por el usuario", Toast.LENGTH_LONG).show();
////                });
////                builder.show();
////                // loop till a runtime exception is triggered.
////                try {
////                    Looper.loop();
////                } catch (RuntimeException e) {
////                }
//            }
//            restServiceUrl = apiUrl;
//        } catch (java.net.SocketTimeoutException | java.net.UnknownHostException | java.net.SocketException ex) {
//            resultado = "Problemas con los servicios web.";
//        } catch (IOException ex) {
//            resultado = "Problemas con el estandar JWT.\nComuníquese con el administrador de su sistema.";
//            System.exit(0);
//        } catch (RuntimeException ex) {
//            resultado = "Error desconocido\nNo es posible verificar la url:\n" + url + "\nDesea continuar firmando bajo su propio riesgo?";
//        }
////        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
////            Toast.makeText(context, "El tiempo de vida del documento en el servidor, se encuentra expirado", Toast.LENGTH_LONG).show();
////        } catch (io.jsonwebtoken.UnsupportedJwtException | io.jsonwebtoken.MalformedJwtException ex) {
////            Toast.makeText(context, "No se incluye estandar JWT.\nComuníquese con el administrador de su sistema.", Toast.LENGTH_LONG).show();
////            System.exit(0);
////        }
//    }
//
//    private void ambiente() {
//        // Invocar el servicio de Preproduccio o Produccion?
//        if (esPreproduccion) {
//            restServiceUrl = REST_SERVICE_URL_PREPRODUCCION;
//        } else if (esDesarrollo) {
//            restServiceUrl = REST_SERVICE_URL_DESARROLLO;
//        } else {
//            restServiceUrl = REST_SERVICE_URL_PRODUCCION;
//        }
//    }
//
//    private Map<Long, byte[]> firmarDocumentos(String json)
//            throws Exception {
//        Map<Long, byte[]> documentos = JsonProcessor.parseJsonDocumentos(json);
//        Map<Long, byte[]> documentosFirmados = new HashMap<>();
//        String fechaHora = JsonProcessor.dparseJsonFechaHora(json);
//        // Firmar!
//        for (Long id : documentos.keySet()) {
//            byte[] documento = documentos.get(id);
//            byte[] documentoFirmado = null;
////            if ("xml".equalsIgnoreCase(formato)) {
////                FirmaDigitalXml firmador = new FirmaDigitalXml();
////                // FIXME
////                documentoFirmado = firmador.firmar(keyStore, documento, clave);
////            }
//            if ("pdf".equalsIgnoreCase(formato)) {
//                FirmaDigitalPdf firmador = new FirmaDigitalPdf();
//                Properties properties = Propiedades.propiedades(versionFirmaEC, llx, lly, pagina, null, fechaHora);
//                documentoFirmado = firmador.firmar(keyStore, alias, documento, keyStorePassword.toString().toCharArray(), properties, url);
//            }
//            documentosFirmados.put(id, documentoFirmado);
//        }
//        return documentosFirmados;
//    }
//
//    private String bajarDocumentos() throws Exception {
//        String json = null;
//        URL url = new URL(restServiceUrl + "/firmadigital/" + tokenJwt);
//        HttpURLConnection con = (HttpURLConnection) url.openConnection();
//        con.setRequestMethod("GET");
//        con.setConnectTimeout(TIME_OUT);
//        resultado = leerBodyErrores(con);
//        if (resultado.isEmpty()) {
//            byte[] buffer = new byte[BUFFER_SIZE];
//            int count;
//
//            try (InputStream in = con.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//                while ((count = in.read(buffer)) != -1) {
//                    out.write(buffer, 0, count);
//                }
//                con.disconnect();
//                json = out.toString();
//            }
//        }
//        return json;
//    }
//
//    private void actualizarDocumentos(String tokenJwt, Map<Long, byte[]> documentosFirmados, String cedula)
//            throws Exception {
//        String json = JsonProcessor.buildJson(documentosFirmados, cedula);
//
//        URL url = new URL(restServiceUrl + "/firmadigital/" + tokenJwt);
//        HttpURLConnection con = (HttpURLConnection) url.openConnection();
//        con.setRequestMethod("PUT");
//        con.setDoOutput(true);
//        con.setRequestProperty("Content-Type", "application/json");
//        con.setConnectTimeout(TIME_OUT);
//
//        byte[] buffer = new byte[BUFFER_SIZE];
//        int count;
//        try (InputStream in = new ByteArrayInputStream(json.getBytes()); OutputStream out = con.getOutputStream()) {
//            while ((count = in.read(buffer)) != -1) {
//                out.write(buffer, 0, count);
//            }
//        }
//        resultado = leerBodyErrores(con);
//        System.out.println("resultado: " + resultado);
//        if (resultado.isEmpty()) {
//            buffer = new byte[BUFFER_SIZE];
//            String parametroJson;
//            if (con.getResponseCode() == 200) {
//                // Leer la respuesta
//                try (InputStream in = con.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//                    while ((count = in.read(buffer)) != -1) {
//                        out.write(buffer, 0, count);
//                    }
//                    parametroJson = out.toString();
//                }
//                resultado = JsonProcessor.parseJsonDocumentoFirmado(parametroJson);
//            }
//            con.disconnect();
//        }
//    }
//
//    private String leerBodyErrores(HttpURLConnection con) {
//        String error = "";
//        try {
//            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
//                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getErrorStream()));
//                String inputLine;
//                StringBuilder response = new StringBuilder();
//                while ((inputLine = bufferedReader.readLine()) != null) {
//                    response.append(inputLine);
//                }
//
//                String body = response.toString();
//                if (body.contains("Token expirado")) {
//                    error = "El tiempo de vida del documento en el servidor, se encuentra expirado";
//                }
//                if (body.contains("Token invalido")
//                        || body.contains("Error al invocar servicio de obtencion de documentos")
//                        || body.contains("Base 64 inválido")) {
//                    error = "No se encontraron documentos para firmar.";
//                }
//                if (body.contains("Cedula invalida")) {
//                    error = "Certificado no corresponde al usuario.\nVuelva a intentarlo.";
//                }
//                if (body.contains("Certificado revocado")) {
//                    error = "Certificado puede estar caducado o revocado.\nVuelva a intentarlo.";
//                }
//                if (response.toString().contains("Request Entity Too Large")) {
//                    error = "Problemas con los servicios web.\nComuníquese con el administrador de su sistema.";
//                }
//            }
//        } catch (IOException ex) {
//            resultado = "Problemas con los servicios web.\nComuníquese con el administrador de su sistema.";
//        }
//        return error;
//    }
//}
//
///**
// * Busca un ApiUrl por URL.
// *
// * @param pkcs12
// * @param password
// * @return json
// */
///*public String validarCertificadoDigital(@NotNull String pkcs12, @NotNull String password) {
//        Certificado certificado = null;
//        String retorno = null;
//        boolean caducado = true, revocado = true;
//        try {
//            byte encodedPkcs12[] = Base64.getDecoder().decode(pkcs12);
//            InputStream inputStreamPkcs12 = new ByteArrayInputStream(encodedPkcs12);
//
//            KeyStoreProvider ksp = new FileKeyStoreProvider(inputStreamPkcs12);
//            KeyStore keyStore;
//            keyStore = ksp.getKeystore(password.toCharArray());
//
//            List<Alias> signingAliases = KeyStoreUtilities.getSigningAliases(keyStore);
//            String alias = signingAliases.get(0).getAlias();
//
//            X509CertificateUtils x509CertificateUtils = new X509CertificateUtils();
//            if (x509CertificateUtils.validarX509Certificate((X509Certificate) keyStore.getCertificate(alias), null)) {//validación de firmaEC
//                X509Certificate x509Certificate = (X509Certificate) keyStore.getCertificate(alias);
//                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
//                TemporalAccessor accessor = dateTimeFormatter.parse(TiempoUtils.getFechaHoraServidor(null));
//                Date fechaHoraISO = Date.from(Instant.from(accessor));
//                //Validad certificado revocado
////                Date fechaRevocado = fechaString_Date("2022-06-01 10:00:16");
//                Date fechaRevocado = UtilsCrlOcsp.validarFechaRevocado(x509Certificate, null);
//                if (fechaRevocado != null && fechaRevocado.compareTo(fechaHoraISO) <= 0) {
//                    retorno = "Certificado revocado: " + fechaRevocado;
//                    revocado = true;
//                } else {
//                    revocado = false;
//                }
////                if (fechaHoraISO.compareTo(x509Certificate.getNotBefore()) <= 0 || fechaHoraISO.compareTo(fechaString_Date("2022-06-21 10:00:16")) >= 0) {
//                if (fechaHoraISO.compareTo(x509Certificate.getNotBefore()) <= 0 || fechaHoraISO.compareTo(x509Certificate.getNotAfter()) >= 0) {
//                    retorno = "Certificado caducado";
//                    caducado = true;
//                } else {
//                    caducado = false;
//                }
//                DatosUsuario datosUsuario = CertEcUtils.getDatosUsuarios(x509Certificate);
//                certificado = new Certificado(
//                        Util.getCN(x509Certificate),
//                        CertEcUtils.getNombreCA(x509Certificate),
//                        Utils.dateToCalendar(x509Certificate.getNotBefore()),
//                        Utils.dateToCalendar(x509Certificate.getNotAfter()),
//                        null,
//                        //                        Utils.dateToCalendar(fechaString_Date("2022-06-01 10:00:16")),
//                        Utils.dateToCalendar(UtilsCrlOcsp.validarFechaRevocado(x509Certificate, null)),
//                        caducado,
//                        datosUsuario);
//            } else {
//                retorno = "Certificado no válido";
//            }
//        } catch (EntidadCertificadoraNoValidaException ecnve) {
//            retorno = "Certificado no válido";
//        } catch (HoraServidorException hse) {
//            retorno = "Problemas en la red\\nIntente nuevamente o verifique su conexión";
//        } catch (KeyStoreException kse) {
//            if (kse.getCause().toString().contains("Invalid keystore format")) {
//                retorno = "Certificado digital es inválido.";
//            }
//            if (kse.getCause().toString().contains("keystore password was incorrect")) {
//                retorno = "La contraseña es inválida.";
//            }
//        } catch (IOException ioe) {
//            retorno = "Excepción no conocida: " + ioe;
//        } finally {
//            Gson gson = new Gson();
//            JsonObject jsonDoc = new JsonObject();
//            jsonDoc.addProperty("error", retorno);
//            JsonArray arrayCer = new JsonArray();
//            if (certificado != null) {
//                boolean signValidate = true;
//                if (revocado || certificado.getValidated() || !certificado.getDatosUsuario().isCertificadoDigitalValido()) {
//                    signValidate = false;
//                } else {
//                    signValidate = true;
//
//                }
//                jsonDoc.addProperty("firmaValida", signValidate);
//                JsonObject jsonCer = new JsonObject();
//                jsonCer.addProperty("emitidoPara", certificado.getIssuedTo());
//                jsonCer.addProperty("emitidoPor", certificado.getIssuedBy());
//                jsonCer.addProperty("validoDesde", calendarToString(certificado.getValidFrom()));
//                jsonCer.addProperty("validoHasta", calendarToString(certificado.getValidTo()));
//                jsonCer.addProperty("fechaRevocado", certificado.getRevocated() != null ? calendarToString(certificado.getRevocated()) : "");
//                jsonCer.addProperty("certificadoVigente", !certificado.getValidated());
//                jsonCer.addProperty("clavesUso", certificado.getKeyUsages());
//                jsonCer.addProperty("integridadFirma", certificado.getSignVerify());
//                jsonCer.addProperty("cedula", certificado.getDatosUsuario().getCedula());
//                jsonCer.addProperty("nombre", certificado.getDatosUsuario().getNombre());
//                jsonCer.addProperty("apellido", certificado.getDatosUsuario().getApellido());
//                jsonCer.addProperty("institucion", certificado.getDatosUsuario().getInstitucion());
//                jsonCer.addProperty("cargo", certificado.getDatosUsuario().getCargo());
//                jsonCer.addProperty("entidadCertificadora", certificado.getDatosUsuario().getEntidadCertificadora());
//                jsonCer.addProperty("serial", certificado.getDatosUsuario().getSerial());
//                jsonCer.addProperty("certificadoDigitalValido", certificado.getDatosUsuario().isCertificadoDigitalValido());
//                arrayCer.add(jsonCer);
//                jsonDoc.add("certificado", arrayCer);
//            }
//            return gson.toJson(jsonDoc);
//        }
//    }
//
//    private String calendarToString(Calendar calendar) {
//        Date date = calendar.getTime();
//        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        return dateFormat.format(date);
//    }
//}*/
