package com.openfocals.commutils.ssl;


import javax.net.ssl.SSLContext;

public class SSLInterceptDataHandler
{
    // this uses SSLServerDataHandler but uses a fixed generated key + certificate 
    // that is valid for all domains coming from the glasses 
    //
    //
    // Usually this should be stored externally, but because this is just a made up 
    // set of encryption keys and certificate that correspond to a made up root cert 
    // we're adding to the glasses, I'm just hardcoding - doesn't matter if anyone discovers it.
    

    //// own root ca - (requires adding to device)
    /// but works to intercept ALL traffic 
    private static final String customca_cert_pem =
"-----BEGIN CERTIFICATE-----" + "\n" +
"MIIDkzCCAnugAwIBAgIUIoGA0U9huvV0+IugJFVf2zGg71MwDQYJKoZIhvcNAQEL" + "\n" +
"BQAwKzELMAkGA1UEBhMCVVMxCzAJBgNVBAgMAklMMQ8wDQYDVQQKDAZhZWdyYXkw" + "\n" +
"HhcNMjAwNzE3MDE0MTAzWhcNMjIxMDIwMDE0MTAzWjAzMQswCQYDVQQGEwJVUzET" + "\n" +
"MBEGA1UECAwKU29tZS1TdGF0ZTEPMA0GA1UECgwGYWVncmF5MIIBIjANBgkqhkiG" + "\n" +
"9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxtnZIeDajFnp7gXoCXG82l8xnfzCQg569jzV" + "\n" +
"8TMsjLjOOfTpbO7sF9eRZpty0IFNUxczEUJEBogEab0ADQgsuFEJ6fCxix1bNhCe" + "\n" +
"Y8AVB9Ql6fBXRcisAmv0ueqLmbW7knn/OjZuVOBA/lXglFjzzrwOMN6A77hev8z6" + "\n" +
"mO4+wKfuGZtHjK2whXDV57XMqm8bRYRdYTDEa+8d8zZMjuo/gZjZ9CTWQCKziqy7" + "\n" +
"rXzGaDyXN1Xt4Av2jB7KOBZHXC0DCNWFLiqUWM0aTyNPqcsVg7q03npNHVnDeRX2" + "\n" +
"JNBW6aPfEp2Miys+6zcAQSCb3YpGfbnFzZ5Upy83QNSeLcT3dQIDAQABo4GmMIGj" + "\n" +
"MB8GA1UdIwQYMBaAFDzteZr4SkDfe988rdySWeD1LSuvMAkGA1UdEwQCMAAwCwYD" + "\n" +
"VR0PBAQDAgTwMGgGA1UdEQRhMF+CC2J5c291dGguY29tgg0qLmJ5c291dGguY29t" + "\n" +
"ggtieW5vcnRoLmNvbYINKi5ieW5vcnRoLmNvbYIMKi5hbWF6b24uY29tggoqLnVi" + "\n" +
"ZXIuY29tggsxOTIuMTY4LjEuNjANBgkqhkiG9w0BAQsFAAOCAQEAaOrHqIW0nqSv" + "\n" +
"NhadJx+Prs7RPhLaZRUvbME5GZDV1RT23G+bLDYPhipgyQhi74vNk/6s5EvffbTG" + "\n" +
"OsHbBDVLgdzO92dlwsd+7/7uIGw1/9ZehmgVFooAjZLhRFWu6Rqbq0B4Y7gJcXA4" + "\n" +
"f98PIIJiqZN0/iORnNOltU72grKb2oiXS+erubYSuMm2CjyFWilMzftV9Nyd8JHz" + "\n" +
"jB7VJpyqKrAoUHBq0ehtSV4NDHE0036EpvSR4Tla8P5pPHU9NuBSr82uam/NE5+q" + "\n" +
"QsZvAXdx/XzLz+jvZaO+bAdlZu+/PCLHVNqjl9Y6Zbj8CCB5x3vi4wR8oVcNhUN3" + "\n" +
"tRmo169mkg==" + "\n" +
"-----END CERTIFICATE-----";

    private static final String customca_privkey_hex =
"MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDG2dkh4NqMWenu" +
"BegJcbzaXzGd/MJCDnr2PNXxMyyMuM459Ols7uwX15Fmm3LQgU1TFzMRQkQGiARp" +
"vQANCCy4UQnp8LGLHVs2EJ5jwBUH1CXp8FdFyKwCa/S56ouZtbuSef86Nm5U4ED+" +
"VeCUWPPOvA4w3oDvuF6/zPqY7j7Ap+4Zm0eMrbCFcNXntcyqbxtFhF1hMMRr7x3z" +
"NkyO6j+BmNn0JNZAIrOKrLutfMZoPJc3Ve3gC/aMHso4FkdcLQMI1YUuKpRYzRpP" +
"I0+pyxWDurTeek0dWcN5FfYk0Fbpo98SnYyLKz7rNwBBIJvdikZ9ucXNnlSnLzdA" +
"1J4txPd1AgMBAAECgf837Q0kehj9YExqpT6daV+0CxHpXS1FZs0uc+236Kye51sY" +
"8ci02n2MNxStxwqDmdolhGDYecqoC5kyI/XftGzfaetf4FIOzjLZAdOKOnuvt/rx" +
"0Ka6+bVm+PXbVBvvHy2iBK3DsYtWU7vzzDNdhacMfdd/5xRcV35Oc1gHqvn7NL/d" +
"/jbY50Ksxe8pGwX8vvY3s6rYdjVx28DbJ0ACMsRzqv+Brew2hnIc9iQOdVUccnsP" +
"wfZrsO3Q9OGFSndA6tUXmYgLsRq/ccDjOqCy+2gGLENm3vm2FyHQrI0ZKtnJ8WJB" +
"Ie9MjmWlXBz7rVAVX9k/uqGjVLmD1iZu7X2ByQECgYEA/3KcVBy+BRpSD67nTIcT" +
"5uCFtJOx3oLoWRc1NpSmbdD/jlbdWp3O7uWKu7GeN17XfwPnuhj6EDdZscxiPVGy" +
"onjMLlbBJqRAY7TxZAX90ppDKEc0jokYGuVGjJs2l7dvw+hxwkH0YmFObbDPf25u" +
"XOfLav0avYsWZZlYnHh6yJ0CgYEAx0fpVAFLuyPjuDw4g10QuBRpvDCXmAU7Zd/k" +
"Q5/IK9KbQFPCZq70+0OvUbKguzv0xJ0/5OvbUr/X8ysXpIyr2WrxyeFI5QfufIh6" +
"wTqFxnIS+MUEZyzpU5R/4zxhCgS3DD2rHhWw1LVwiju6ubrjQ9BSVfQ0FBoaAnj8" +
"R0YWlrkCgYEAmXpzEtZyFP7Lb0DXF9PQ0Vb0/pn+tIJSt7SJ9FE77Z807ICkXl2S" +
"h1bDFCKTvRDuyRG/mMCI/lVDPuBd5hnACn4pr23QfzcQuNducXpoFrE4yGp/2WPl" +
"Z8N5r+FR27YLINn3/49BFhsA1Eb3ZMkk5g0e2xlXh6qZulF4PArnOsECgYEAjjvN" +
"VZ/3JYLgLccNrvTl0fz/2snz6RUS6At9KKcKf/y5wpgF0LRfNleA8pbEEd8hPJPf" +
"3sxYph83SuAe59tfbLbgDG83UADxPwFKXFAg2xrgX55/HgT4JCeBf/bAbB4haCM7" +
"1yU0KR23TXKEO7wgKW6u1ZqO8SoQFmOz7EsiJEECgYB2hSASAFFGOqhfV30YkWVS" +
"cN80PCPE0HMASXCXQY8hKjcfuVPcZX5dZja+vMwSx+E5TGQG0+oTCVrqfdvLcHvC" +
"bYW/b9PHU5BoEWaAQkZE+oihTfK/DX61eK+h+mTYG6kkdSbFtxwnLNCuneUgL4dD" +
"TnFlFqBJ6fqU23MT8jJbOw==";




    
    
    
    
    // letsencrypt - cloud.ofocals.com - doesn't require writing to device filesystem
    //  and works to decrypt cloud traffic
    private static final String cloud_cert_pem = 
"-----BEGIN CERTIFICATE-----" + "\n" + 
///// PUT CERTIFICATE CHAIN PEM HERE - this is left out of the repo
"-----END CERTIFICATE-----";


    private static final String cloud_privkey_hex = 
///// PUT CLOUD PRIVATE KEY HERE (not the header or footer) - this is left out of the repo
    "";

    

    public static SSLServerDataHandler createCloudInterceptSSLHandler() throws Exception {
        return new SSLServerDataHandler(cloud_privkey_hex, cloud_cert_pem);
    }

    public static SSLServerDataHandler createBroadInterceptSSLHandler() throws Exception {
        return new SSLServerDataHandler(customca_privkey_hex, customca_cert_pem);
    }
    
    public static SSLContext createBroadSSLContext() throws Exception {
        return PEMImporter.createSSLContextForStrings(customca_privkey_hex, customca_cert_pem, "");
    }

}


