package com.openfocals.commutils.ssl;

import android.util.Base64;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;



public class PEMImporter {
    public static SSLContext createSSLContextForStrings(String privateKeyHex, String certificatePem, String password) throws Exception {
        final SSLContext context = SSLContext.getInstance("TLS");
        final KeyStore keystore = createKeyStoreForStrings(privateKeyHex, certificatePem, password);
        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keystore, password.toCharArray());
        final KeyManager[] km = kmf.getKeyManagers();
        context.init(km, null, null);
        return context;
    }
    
    public static KeyStore createKeyStoreForStrings(String privateKeyHex, String certificatePem, final String password)
            throws Exception, KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        final X509Certificate[] cert = createCertificates(certificatePem);
        final KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(null);
        // Import private key
        final PrivateKey key = createPrivateKeyFromString(privateKeyHex);
        keystore.setKeyEntry("privkey", key, password.toCharArray(), cert);
        return keystore;
    }


    private static PrivateKey createPrivateKeyFromString(String hexString) throws Exception {
        final byte[] bytes = Base64.decode(hexString, Base64.DEFAULT);
        return generatePrivateKeyFromDER(bytes);
    }


    private static X509Certificate[] createCertificates(String certificatePem) throws Exception {
        final List<X509Certificate> result = new ArrayList<X509Certificate>();

        String[] lines = certificatePem.split("\n");

        boolean in_cert = false;
        StringBuilder b = new StringBuilder();
        for (String s : lines)
        {
            if (!in_cert && !s.contains("BEGIN CERTIFICATE")) {
                throw new IllegalArgumentException("No CERTIFICATE found");
            } else if (!in_cert) {
                in_cert = true;     
            } else if (s.contains("END CERTIFICATE")) {
                in_cert = false;
                String hexString = b.toString();
                final byte[] bytes = Base64.decode(hexString, Base64.DEFAULT);
                X509Certificate cert = generateCertificateFromDER(bytes);
                result.add(cert);
                b = new StringBuilder();
            } else {
                if (!s.startsWith("----")) {
                    b.append(s);
                }
            }
        }
        if (in_cert)
            throw new IllegalArgumentException("Final certificate didn't close");

        if (result.size() == 0)
            throw new IllegalArgumentException("No certificates found");

        return result.toArray(new X509Certificate[result.size()]);
    }


    private static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
        final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        final KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) factory.generatePrivate(spec);
    }

    private static X509Certificate generateCertificateFromDER(byte[] certBytes) throws CertificateException {
        final CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
    }

}
