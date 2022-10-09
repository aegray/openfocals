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
    private static final String cloud_cert_pem = "-----BEGIN CERTIFICATE-----" + "\n" + 
        "MIIGcjCCBFqgAwIBAgIRAKKBc8vDB7ExSAIuMkFcUEQwDQYJKoZIhvcNAQEMBQAw" + "\n" + 
        "SzELMAkGA1UEBhMCQVQxEDAOBgNVBAoTB1plcm9TU0wxKjAoBgNVBAMTIVplcm9T" + "\n" + 
        "U0wgUlNBIERvbWFpbiBTZWN1cmUgU2l0ZSBDQTAeFw0yMjEwMDUwMDAwMDBaFw0y" + "\n" + 
        "MzAxMDMyMzU5NTlaMBwxGjAYBgNVBAMTEWNsb3VkLm9mb2NhbHMuY29tMIIBIjAN" + "\n" + 
        "BgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAv9CEH6agiGryDp6crGaA+MrUGqco" + "\n" + 
        "APFEAO5/00RVhQbXgf+8H6a/WAt6zM/jndwthDOtkC81OiPZV2rAGjk8sXBS1NJh" + "\n" + 
        "NzKZ8L4XcI+c+FLqC3MjgP26OIyfY4DYLW36dwERHOjqMZtDlVkS0nkJ1q+AX4aa" + "\n" + 
        "EJWCEgu81S2uPqgQfo73Yo+CwwJFvcUWk6ay394FsQ+0kJ8VEFDiUepoH6IalAta" + "\n" + 
        "hduKSpGxU6UOccezPSlnrec0+HMjFJElhhLm2aeKx9an5wV/1Dop2Cf/MUnYRoyQ" + "\n" + 
        "XWfqL/HgNStWSAc1+WNdDXFgHD/KBCj+z4RyisIaxgZdIXgoadbNcFOUhQIDAQAB" + "\n" + 
        "o4ICfjCCAnowHwYDVR0jBBgwFoAUyNl4aKLZGWjVPXLeXwo+3LWGhqYwHQYDVR0O" + "\n" + 
        "BBYEFHL1KRQlt03wqJX2+en6XD+Nf1PbMA4GA1UdDwEB/wQEAwIFoDAMBgNVHRMB" + "\n" + 
        "Af8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjBJBgNVHSAEQjBA" + "\n" + 
        "MDQGCysGAQQBsjEBAgJOMCUwIwYIKwYBBQUHAgEWF2h0dHBzOi8vc2VjdGlnby5j" + "\n" + 
        "b20vQ1BTMAgGBmeBDAECATCBiAYIKwYBBQUHAQEEfDB6MEsGCCsGAQUFBzAChj9o" + "\n" + 
        "dHRwOi8vemVyb3NzbC5jcnQuc2VjdGlnby5jb20vWmVyb1NTTFJTQURvbWFpblNl" + "\n" + 
        "Y3VyZVNpdGVDQS5jcnQwKwYIKwYBBQUHMAGGH2h0dHA6Ly96ZXJvc3NsLm9jc3Au" + "\n" + 
        "c2VjdGlnby5jb20wggEFBgorBgEEAdZ5AgQCBIH2BIHzAPEAdgCt9776fP8QyIud" + "\n" + 
        "PZwePhhqtGcpXc+xDCTKhYY069yCigAAAYOp7UmsAAAEAwBHMEUCIA2qWeCgbnIS" + "\n" + 
        "XBaBa0z5GGlroYDNxa79wGHlnL1L0sTLAiEAsuNScs/77bVPFEaynFs62BfIIfmU" + "\n" + 
        "oXcqgtk9ueaDSKMAdwB6MoxU2LcttiDqOOBSHumEFnAyE4VNO9IrwTpXo1LrUgAA" + "\n" + 
        "AYOp7UmFAAAEAwBIMEYCIQCpjkzn/DCHg92XstBvFRnBuJ6qVdbNdpApdKncrW47" + "\n" + 
        "MQIhAPRRj/6R2stXVsd/EtPXoFALiDbQyxXbDtEkwhllh5H8MBwGA1UdEQQVMBOC" + "\n" + 
        "EWNsb3VkLm9mb2NhbHMuY29tMA0GCSqGSIb3DQEBDAUAA4ICAQAgBJKAbIIUaNis" + "\n" + 
        "sQwSDlC97afZHSW/daIFU19EoSbVFD4oK/NtyAav+dOSlYa1KA8EeU4sy4n3D/Ja" + "\n" + 
        "kob7f4EgiC82VzZ+aanK54lDhXzgxvXZ9Fd+GdpDU0D+3qj0X6KqatPs0LUhk14J" + "\n" + 
        "SBdxeRt1mA1n4+dHWFWRlfS3xwa3Xvf5BM9aZykQ60dhj7aUz6Sw+aFoiIdaSzQa" + "\n" + 
        "mGHrTuo+y+31ncpDZswL4MxlL86NMfw0ux0G4hihE7itBPirmr0+GolTQEjshFa3" + "\n" + 
        "neWdTSM3uJtw5Dh94NXeRd0YXyarDQaaxPPGtHnVkBmAod6NN7lM3YtwKGzIoDLc" + "\n" + 
        "iiQnihLRZSYBEhPrOdXFuNPHBA9s/NXTwYxPLSpe+9DBEnM61r8Mw4n/Spe0Q046" + "\n" + 
        "0VEzNkFydYz6CWEtS63SSnr7IhSpbjLHOZ+n9a4yayeqcr/9pwAXXbN3M1OwKr7+" + "\n" + 
        "d1PHVrD9rWK3rGpHrA2nRENlySQzM1boP4HxS+NCncq2ocA/pntAc5tgpVF0oLuB" + "\n" + 
        "Dxea4rwDvtEjRyxZEzLDTap4ypEG+r4MXCPdmFyIMbQYWmxFpvXPWvtINQdoXAsv" + "\n" + 
        "Ds/EIU+Ya9PfR+ZK4CdTk2BJ/yC9koIUsVzEUiOkmc/PvBgUv6gzMUjVjUiRBVFv" + "\n" + 
        "UDHk5FRpbsKqBjLsHAUfeUfOYhkc6A==" + "\n" + 
        "-----END CERTIFICATE-----" + "\n" + 
        "-----BEGIN CERTIFICATE-----" + "\n" + 
        "MIIG1TCCBL2gAwIBAgIQbFWr29AHksedBwzYEZ7WvzANBgkqhkiG9w0BAQwFADCB" + "\n" + 
        "iDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCk5ldyBKZXJzZXkxFDASBgNVBAcTC0pl" + "\n" + 
        "cnNleSBDaXR5MR4wHAYDVQQKExVUaGUgVVNFUlRSVVNUIE5ldHdvcmsxLjAsBgNV" + "\n" + 
        "BAMTJVVTRVJUcnVzdCBSU0EgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwHhcNMjAw" + "\n" + 
        "MTMwMDAwMDAwWhcNMzAwMTI5MjM1OTU5WjBLMQswCQYDVQQGEwJBVDEQMA4GA1UE" + "\n" + 
        "ChMHWmVyb1NTTDEqMCgGA1UEAxMhWmVyb1NTTCBSU0EgRG9tYWluIFNlY3VyZSBT" + "\n" + 
        "aXRlIENBMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAhmlzfqO1Mdgj" + "\n" + 
        "4W3dpBPTVBX1AuvcAyG1fl0dUnw/MeueCWzRWTheZ35LVo91kLI3DDVaZKW+TBAs" + "\n" + 
        "JBjEbYmMwcWSTWYCg5334SF0+ctDAsFxsX+rTDh9kSrG/4mp6OShubLaEIUJiZo4" + "\n" + 
        "t873TuSd0Wj5DWt3DtpAG8T35l/v+xrN8ub8PSSoX5Vkgw+jWf4KQtNvUFLDq8mF" + "\n" + 
        "WhUnPL6jHAADXpvs4lTNYwOtx9yQtbpxwSt7QJY1+ICrmRJB6BuKRt/jfDJF9Jsc" + "\n" + 
        "RQVlHIxQdKAJl7oaVnXgDkqtk2qddd3kCDXd74gv813G91z7CjsGyJ93oJIlNS3U" + "\n" + 
        "gFbD6V54JMgZ3rSmotYbz98oZxX7MKbtCm1aJ/q+hTv2YK1yMxrnfcieKmOYBbFD" + "\n" + 
        "hnW5O6RMA703dBK92j6XRN2EttLkQuujZgy+jXRKtaWMIlkNkWJmOiHmErQngHvt" + "\n" + 
        "iNkIcjJumq1ddFX4iaTI40a6zgvIBtxFeDs2RfcaH73er7ctNUUqgQT5rFgJhMmF" + "\n" + 
        "x76rQgB5OZUkodb5k2ex7P+Gu4J86bS15094UuYcV09hVeknmTh5Ex9CBKipLS2W" + "\n" + 
        "2wKBakf+aVYnNCU6S0nASqt2xrZpGC1v7v6DhuepyyJtn3qSV2PoBiU5Sql+aARp" + "\n" + 
        "wUibQMGm44gjyNDqDlVp+ShLQlUH9x8CAwEAAaOCAXUwggFxMB8GA1UdIwQYMBaA" + "\n" + 
        "FFN5v1qqK0rPVIDh2JvAnfKyA2bLMB0GA1UdDgQWBBTI2XhootkZaNU9ct5fCj7c" + "\n" + 
        "tYaGpjAOBgNVHQ8BAf8EBAMCAYYwEgYDVR0TAQH/BAgwBgEB/wIBADAdBgNVHSUE" + "\n" + 
        "FjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwIgYDVR0gBBswGTANBgsrBgEEAbIxAQIC" + "\n" + 
        "TjAIBgZngQwBAgEwUAYDVR0fBEkwRzBFoEOgQYY/aHR0cDovL2NybC51c2VydHJ1" + "\n" + 
        "c3QuY29tL1VTRVJUcnVzdFJTQUNlcnRpZmljYXRpb25BdXRob3JpdHkuY3JsMHYG" + "\n" + 
        "CCsGAQUFBwEBBGowaDA/BggrBgEFBQcwAoYzaHR0cDovL2NydC51c2VydHJ1c3Qu" + "\n" + 
        "Y29tL1VTRVJUcnVzdFJTQUFkZFRydXN0Q0EuY3J0MCUGCCsGAQUFBzABhhlodHRw" + "\n" + 
        "Oi8vb2NzcC51c2VydHJ1c3QuY29tMA0GCSqGSIb3DQEBDAUAA4ICAQAVDwoIzQDV" + "\n" + 
        "ercT0eYqZjBNJ8VNWwVFlQOtZERqn5iWnEVaLZZdzxlbvz2Fx0ExUNuUEgYkIVM4" + "\n" + 
        "YocKkCQ7hO5noicoq/DrEYH5IuNcuW1I8JJZ9DLuB1fYvIHlZ2JG46iNbVKA3ygA" + "\n" + 
        "Ez86RvDQlt2C494qqPVItRjrz9YlJEGT0DrttyApq0YLFDzf+Z1pkMhh7c+7fXeJ" + "\n" + 
        "qmIhfJpduKc8HEQkYQQShen426S3H0JrIAbKcBCiyYFuOhfyvuwVCFDfFvrjADjd" + "\n" + 
        "4jX1uQXd161IyFRbm89s2Oj5oU1wDYz5sx+hoCuh6lSs+/uPuWomIq3y1GDFNafW" + "\n" + 
        "+LsHBU16lQo5Q2yh25laQsKRgyPmMpHJ98edm6y2sHUabASmRHxvGiuwwE25aDU0" + "\n" + 
        "2SAeepyImJ2CzB80YG7WxlynHqNhpE7xfC7PzQlLgmfEHdU+tHFeQazRQnrFkW2W" + "\n" + 
        "kqRGIq7cKRnyypvjPMkjeiV9lRdAM9fSJvsB3svUuu1coIG1xxI1yegoGM4r5QP4" + "\n" + 
        "RGIVvYaiI76C0djoSbQ/dkIUUXQuB8AL5jyH34g3BZaaXyvpmnV4ilppMXVAnAYG" + "\n" + 
        "ON51WhJ6W0xNdNJwzYASZYH+tmCWI+N60Gv2NNMGHwMZ7e9bXgzUCZH5FaBFDGR5" + "\n" + 
        "S9VWqHB73Q+OyIVvIbKYcSc2w/aSuFKGSA==" + "\n" + 
        "-----END CERTIFICATE-----";


    private static final String cloud_privkey_hex = 
///// PUT CLOUD PRIVATE KEY HERE (not the header or footer) - this is left out of the repo
        "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC/0IQfpqCIavIO" + 
        "npysZoD4ytQapygA8UQA7n/TRFWFBteB/7wfpr9YC3rMz+Od3C2EM62QLzU6I9lX" + 
        "asAaOTyxcFLU0mE3Mpnwvhdwj5z4UuoLcyOA/bo4jJ9jgNgtbfp3AREc6Ooxm0OV" + 
        "WRLSeQnWr4BfhpoQlYISC7zVLa4+qBB+jvdij4LDAkW9xRaTprLf3gWxD7SQnxUQ" + 
        "UOJR6mgfohqUC1qF24pKkbFTpQ5xx7M9KWet5zT4cyMUkSWGEubZp4rH1qfnBX/U" + 
        "OinYJ/8xSdhGjJBdZ+ov8eA1K1ZIBzX5Y10NcWAcP8oEKP7PhHKKwhrGBl0heChp" + 
        "1s1wU5SFAgMBAAECggEAViFXcq7rntYG4zGtwGw2tYC+RUmR2Pp9Qr3VgBRLInS0" + 
        "CJfA7aV+fhaJibY/yv63IYnjAb9w8y5txacVo7DX5AU/7HibVqHOJh/1foG+RnNU" + 
        "zNditeU43XOnjpKof53GvfEosgaUQropWsKohQH8vP4JctuKCrBXu6qxnDfq6g7e" + 
        "JrUg3De5FuC+1ynUUSISSmfrNAKk8LXY+B0tm4UBeGn1wMid+jdo/f5CF2QnOYBM" + 
        "nLFShmK5+4bpCpHGCEaS4M5N8S0+8eYJxAipQSkOIIyCHZJD7wxGJTBfp7ZtVRYM" + 
        "5rBLuWi+juEVOczJ2h7t8XeiNAJ3kFfdhj0ojkFw8QKBgQDqZPiJlsIHctwnyS/r" + 
        "M4aBjo7x8ogDH3090qeRj5uAnFAhla+oTVXYFv9vTdYF2aAmIAvPlc3I6+jiyzTt" + 
        "DZJJ4CsG5KoO2R4QNriefhPGrHGq22xiG8gBk3cfTUE6llccr6kSUm10DJ9fEsAa" + 
        "2ZlIPd6/wXxcaBSOnEr/wdu7cwKBgQDRfshvhjQ5qeiduuI/tLIrkm3koUjgbFc9" + 
        "P3yn/Q6pLff4InTmW0WtSGvOhI3NN3VWpet1cGcV1nVDgoHC1vhtB6xWPQ+3KTYC" + 
        "lDG1lE8IlvxF94cG48agiMwiyd3iqpg+5ZtfEmLQMSyCO1XcEjyFAxrtyzApRdV5" + 
        "dVkCt4hiJwKBgGJmmzRf1vr7jFMjkftiuTAAoiAOmmz9Nj3TBsgECts2RCmlRoYY" + 
        "WVaLaRbYDWaNUz9OKsct86NK7ozvlDuEfAqJkmalboMnQQi7gEwBT9oTIPEChwC0" + 
        "+wU2XEcGzsCCxvVjBU6Mzihu+CUYoQ9klIYQe0fzOXstspugH5g5UT7jAoGBAMGb" + 
        "sPNYjPVpsD/tEeEcRrj797Ge2LIHjkxdWwAJX748bHmWpoCpg2hUkMVoPYCVq0xr" + 
        "xIJhYfONogvFFdUSRL7Go5+DiP2WCYMyoAoWyT/AAd9Lli9o3A1Kv5W7B+POEyu1" + 
        "mPSxEj8j4uVsnrnm5KVsrDttV9Ic5t/78koPgexrAoGATjoG3vLM2W0ZEOnc0Ac9" + 
        "nShdD+MW+DkO2V3MGNfiUvoNQonUjG/l9bknPbzAoBg0HJFnuXnBBESgoy+SBtkv" + 
        "SOHD9xen/nv39pD3t/V9QkYiU6OqkjtYuQVltzNKFCksI1FGrHbn75U8zblsy7OK" + 
        "2k9qbnsD5bkYzpsoH4Kk4K0=";

    

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


