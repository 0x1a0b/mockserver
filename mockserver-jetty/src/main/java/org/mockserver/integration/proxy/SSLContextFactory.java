package org.mockserver.integration.proxy;

import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * @author jamesdbloom
 */
public class SSLContextFactory {

    private static SslContextFactory sslContextFactory;

    public static SslContextFactory createSSLContextFactory() {
        if (sslContextFactory == null) {
            sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStore(SSLFactory.buildKeyStore());
            sslContextFactory.setKeyStorePassword(SSLFactory.KEY_STORE_PASSWORD);
            sslContextFactory.checkKeyStore();
            sslContextFactory.setTrustStore(SSLFactory.buildKeyStore());
        }
        return sslContextFactory;
    }
}
