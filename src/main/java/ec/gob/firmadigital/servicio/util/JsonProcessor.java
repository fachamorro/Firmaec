//package ec.gob.firmadigital.servicio.util;
//
//import java.util.Base64;
//import java.util.HashMap;
//import java.util.Map;
//
//public class JsonProcessor {
//
//    /**
//     * Base 64 decoder
//     */
//    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();
//
//    /**
//     * Base 64 encoder
//     */
//    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
//
//    /**
//     * Transforma una cadena de texto JSON con documentos en Base 64 en un Map
//     * de archivos binarios.
//     */
//    public static Map<Long, byte[]> parseJsonDocumentos(String json) {
//        Map<Long, byte[]> documentosDecoder = new HashMap<>();
//        try {
//            JSONObject jsonObject = new JSONObject(json);
//            JSONArray documentos;
//            documentos = jsonObject.getJSONArray("documentos");
//
//            Map<Long, String> documentosBase64 = new HashMap<>();
//            for (int i = 0; i < documentos.length(); i++) {
//                Long id = documentos.getJSONObject(i).getLong("id");
//                String documento = documentos.getJSONObject(0).getString("documento");
//                documentosBase64.put(id, documento);
//            }
//            // Documentos a retornar
//            documentosDecoder = new HashMap<>();
//            for (Long id : documentosBase64.keySet()) {
//                String base64 = documentosBase64.get(id);
//                documentosDecoder.put(id, BASE64_DECODER.decode(base64));
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        return documentosDecoder;
//    }
//
//    public static String dparseJsonFechaHora(String json) {
//        try {
//            JSONObject jsonObject = new JSONObject(json);
//            String fechaHora;
//            fechaHora = jsonObject.getString("fecha_hora");
//            return fechaHora;
//        } catch (JSONException e) {
//            e.printStackTrace();
//            return "Problemas con la respuesta desde el servidor";
//        }
//    }
//
//    public static String parseJsonDocumentoFirmado(String json) {
//        JSONObject jsonObject;
//        try {
//            jsonObject = new JSONObject(json);
//            int documentosRecibidos = jsonObject.getInt("documentos_recibidos");
//            int documentosFirmados = jsonObject.getInt("documentos_firmados");
//            if (documentosFirmados == documentosRecibidos
//                    || (documentosFirmados > 0 && documentosFirmados < documentosRecibidos)) {
//                return "Se firmó exitosamente " + documentosFirmados + " documento(s) de " + documentosRecibidos;
//            } else {
//                return "No se firmaron los documentos";
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//            return "Problemas con la respuesta desde el servidor";
//        }
//    }
//
//    /**
//     * Crear una cadena de texto JSON para representar documentos en Base 64.
//     */
//    public static String buildJson(Map<Long, byte[]> documentos, String cedula) {
//        // Documentos a retornar
//        JSONArray jsonArray = new JSONArray();
//        JSONObject jsonObject = new JSONObject();
//        Map<String, Object> documentosEncoder = new HashMap<>();
//        for (Long id : documentos.keySet()) {
//            byte[] documento = documentos.get(id);
//            String base64 = BASE64_ENCODER.encodeToString(documento);
//            documentosEncoder.put("id", id);
//            documentosEncoder.put("documento", base64);
//            jsonArray.put(new JSONObject(documentosEncoder));
//        }
//        try {
//            jsonObject.put("cedula", cedula);
//            jsonObject.put("documentos", jsonArray);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        return jsonObject.toString();
//    }
//
//    /**
//     * Crear una cadena de texto JSON con información del API para autorizar.
//     */
//    public static String buildJsonApi(String sistema, String url) {
//        // Api para verificar
//        JSONObject jsonObject = new JSONObject();
//        try {
//            jsonObject.put("sistema", sistema);
//            jsonObject.put("url", url);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        return jsonObject.toString();
//    }
//}
