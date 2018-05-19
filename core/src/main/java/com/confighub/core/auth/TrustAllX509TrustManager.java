package com.confighub.core.auth;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * This TrustManager will blindly accept any certificate, useful only if you operate in a trusted environment where
 * the risk of MITM attacks is low, but you still want to prevent clear text connections to protect against
 * casual network sniffing.
 */
public class TrustAllX509TrustManager
        implements X509TrustManager
{
    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
