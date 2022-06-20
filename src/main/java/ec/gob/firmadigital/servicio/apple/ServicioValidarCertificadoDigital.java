/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/WebServices/GenericResource.java to edit this template
 */
package ec.gob.firmadigital.servicio.apple;

import ec.gob.firmadigital.servicio.util.ValidadorCertificadoDigital;
import io.rubrica.certificate.to.Certificado;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author barckl3y
 */
@Path("validarCertificadoDigital")
public class ServicioValidarCertificadoDigital {

    @Context
    private UriInfo context;

    /**
     * Creates a new instance of ServicioValidarCertificadoDigital
     */
    public ServicioValidarCertificadoDigital() {
    }

    /**
     * Retrieves representation of an instance of ec.gob.firmadigital.servicio.apple.ServicioValidarCertificadoDigital
     * @return an instance of java.lang.String
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response validar(String certificadoBase64,String contrasena) {
        try {
            certificadoBase64 = "MIIHFDCCBfygAwIBAgIIK2o4sL7KHQgwDQYJKoZIhvcNAQELBQAwSTELMAkGA1UEBhMCVVMxEzARBgNVBAoTCkdvb2dsZSBJbmMxJTAjBgNVBAMTHEdvb2dsZSBJbnRlcm5ldCBBdXRob3JpdHkgRzIwHhcNMTYxMjE1MTQwNDE1WhcNMTcwMzA5MTMzNTAwWjBmMQswCQYDVQQGEwJVUzETMBEGA1UECAwKQ2FsaWZvcm5pYTEWMBQGA1UEBwwNTW91bnRhaW4gVmlldzETMBEGA1UECgwKR29vZ2xlIEluYzEVMBMGA1UEAwwMKi5nb29nbGUuY29tMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEG1y99TYpFSSiawnjJKYI8hyEzJ4M+IELfLjmSsYI7fW/V8AT61quCswtBMikJYqzYBZrV2Reu5sHlLr6936cR6OCBKwwggSoMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjCCA2sGA1UdEQSCA2IwggNeggwqLmdvb2dsZS5jb22CDSouYW5kcm9pZC5jb22CFiouYXBwZW5naW5lLmdvb2dsZS5jb22CEiouY2xvdWQuZ29vZ2xlLmNvbYIWKi5nb29nbGUtYW5hbHl0aWNzLmNvbYILKi5nb29nbGUuY2GCCyouZ29vZ2xlLmNsgg4qLmdvb2dsZS5jby5pboIOKi5nb29nbGUuY28uanCCDiouZ29vZ2xlLmNvLnVrgg8qLmdvb2dsZS5jb20uYXKCDyouZ29vZ2xlLmNvbS5hdYIPKi5nb29nbGUuY29tLmJygg8qLmdvb2dsZS5jb20uY2+CDyouZ29vZ2xlLmNvbS5teIIPKi5nb29nbGUuY29tLnRygg8qLmdvb2dsZS5jb20udm6CCyouZ29vZ2xlLmRlggsqLmdvb2dsZS5lc4ILKi5nb29nbGUuZnKCCyouZ29vZ2xlLmh1ggsqLmdvb2dsZS5pdIILKi5nb29nbGUubmyCCyouZ29vZ2xlLnBsggsqLmdvb2dsZS5wdIISKi5nb29nbGVhZGFwaXMuY29tgg8qLmdvb2dsZWFwaXMuY26CFCouZ29vZ2xlY29tbWVyY2UuY29tghEqLmdvb2dsZXZpZGVvLmNvbYIMKi5nc3RhdGljLmNugg0qLmdzdGF0aWMuY29tggoqLmd2dDEuY29tggoqLmd2dDIuY29tghQqLm1ldHJpYy5nc3RhdGljLmNvbYIMKi51cmNoaW4uY29tghAqLnVybC5nb29nbGUuY29tghYqLnlvdXR1YmUtbm9jb29raWUuY29tgg0qLnlvdXR1YmUuY29tghYqLnlvdXR1YmVlZHVjYXRpb24uY29tggsqLnl0aW1nLmNvbYIaYW5kcm9pZC5jbGllbnRzLmdvb2dsZS5jb22CC2FuZHJvaWQuY29tghtkZXZlbG9wZXIuYW5kcm9pZC5nb29nbGUuY26CBGcuY2+CBmdvby5nbIIUZ29vZ2xlLWFuYWx5dGljcy5jb22CCmdvb2dsZS5jb22CEmdvb2dsZWNvbW1lcmNlLmNvbYIKdXJjaGluLmNvbYIKd3d3Lmdvby5nbIIIeW91dHUuYmWCC3lvdXR1YmUuY29tghR5b3V0dWJlZWR1Y2F0aW9uLmNvbTALBgNVHQ8EBAMCB4AwaAYIKwYBBQUHAQEEXDBaMCsGCCsGAQUFBzAChh9odHRwOi8vcGtpLmdvb2dsZS5jb20vR0lBRzIuY3J0MCsGCCsGAQUFBzABhh9odHRwOi8vY2xpZW50czEuZ29vZ2xlLmNvbS9vY3NwMB0GA1UdDgQWBBThPf/3oDfxFM/hdOi5kLv8qrZbsjAMBgNVHRMBAf8EAjAAMB8GA1UdIwQYMBaAFErdBhYbvPZotXb1gba7Yhq6WoEvMCEGA1UdIAQaMBgwDAYKKwYBBAHWeQIFATAIBgZngQwBAgIwMAYDVR0fBCkwJzAloCOgIYYfaHR0cDovL3BraS5nb29nbGUuY29tL0dJQUcyLmNybDANBgkqhkiG9w0BAQsFAAOCAQEAWZQy0Kvn9cPnIh7Z4kfUCXX/dhdvjLJYFAn3b3d5DVs1BLYuukfIjilVdAeTUHZH7TLn/uVejg3yS0ssRg1ds1iv2O9DJbnl5FHcjNAvwfN533FulWP41OC6B6dC6BGGTXTvQobDup7/EKg1GWX9ksBtTfKLH5wrjhN955Itnd25Sjw2bSjLaWEtTrjINXmnBoc2+qHFzF/fNxK1KbmkBboUIGoaGsThe3AF0Ye+XAeaZH08+GdrorknlHDQLLtHIcJ3C6PrQ/kTpwWd/TVXW42BN+N7xZiGJbvKOg0S0rk2hzhgX4QoUKZHMqqh1sS6ypkfnWx75nh325y4Tenk+A==";
            contrasena="1234";
            
            ValidadorCertificadoDigital validador = new ValidadorCertificadoDigital();
            Certificado certificado = validador.validarCertificado(certificadoBase64,contrasena);
            if (certificado != null) {
                 String respuesta="{'respuesta': {'codigo': '200','mensaje': 'ok'}}";
                 return Response.ok(respuesta, MediaType.APPLICATION_JSON).build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.SEE_OTHER).entity("Error: " + e.toString()).build();
        }
        return null;
    }

}
