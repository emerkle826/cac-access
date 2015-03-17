/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.emerkle.cac;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyStoreBuilderParameters;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import sun.security.pkcs11.SunPKCS11;

/**
 *
 * @author emerkle
 */
public class CACAccess {

    private static boolean initialized = false;
    private static KeyStore cacKeyStore;
    public static SSLSocketFactory sslFactory;

    public static synchronized void init() {
        if (initialized) {
            // already initialized, just exit
            return;
        }
        CACConfig cacConfig = CACConfig.generateConfig(System.getProperty("cac.library"));
        if (cacConfig == null) {
            // CAC config generation failed, we're done
            return;
        }
        try {
            SunPKCS11 provider = new SunPKCS11(cacConfig.getStreamedConfig());
            Security.insertProviderAt(provider, 1);
//            char[] pin = System.console().readPassword("Please enter you PIN: ");
//            cacKeyStore = KeyStore.getInstance("PKCS11", provider);
//            cacKeyStore.load(null, pin);
            // set the keystore type
            //System.setProperty("javax.net.ssl.keyStoreType", "PKCS11");
            //System.setProperty("javax.net.ssl.keyStore", "NONE");
            // KeyStoreBuilder
            KeyStore.Builder builder = KeyStore.Builder.newInstance("PKCS11", provider, (new CACPasswordProtection()).getProtection());
            // KeyStore Parameters
            ManagerFactoryParameters ksParams = new KeyStoreBuilderParameters(Arrays.asList(new KeyStore.Builder[]{builder}));
            // Create KeyManagerFactory
            KeyManagerFactory factory = KeyManagerFactory.getInstance("NewSunX509");
            // initialize the factory
            factory.init(ksParams);
            for (KeyManager km : factory.getKeyManagers()) {
                System.out.println(km);
            }
            final KeyStore keystoreTrusted = KeyStore.getInstance("JKS");
            keystoreTrusted.load(CACAccess.class.getResourceAsStream("rulebotTrustStore"), "rulebotAgent".toCharArray());
            final TrustManagerFactory tTrustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            tTrustManagerFactory.init(keystoreTrusted);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(factory.getKeyManagers(), tTrustManagerFactory.getTrustManagers(), null);
            sslFactory = ctx.getSocketFactory();
            com.sun.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sslFactory);
            // set initialized to true
            initialized = true;
            //provider.list(System.out);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private CACAccess() {
        // no construction needed
    }

    public static void main(String arg[]) throws Exception {
        CACAccess cac = new CACAccess();
        init();
        cac.showInfoAboutCAC();
    }

    private void showInfoAboutCAC() throws KeyStoreException, CertificateException {
        Enumeration<String> aliases = cacKeyStore.aliases();

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            X509Certificate[] cchain = (X509Certificate[]) cacKeyStore.getCertificateChain(alias);

            System.out.println("Certificate Chain for : " + alias);
            if (cchain != null) {
                for (int i = 0; i < cchain.length; i++) {
                    System.out.println(i + " SubjectDN: " + cchain[i].getSubjectDN());
                    System.out.println(i + " IssuerDN:  " + cchain[i].getIssuerDN());
                }
            } else {
                System.out.println("X509Certificate Chain is NULL for alias: " + alias);
            }
            X509Certificate cert = (X509Certificate) (cacKeyStore.getCertificate(alias));
            System.out.println("\nCERT:\n" + cert);
        }
    }

    private static class CACPasswordProtection implements PasswordHandler {

        private char[] password = null;
        private final Object mutex = new Object();

        private CACPasswordProtection() {
            PasswordDialog pd = new PasswordDialog(this);
            pd.displayIt();
        }

        KeyStore.PasswordProtection getProtection() {
            try {
                synchronized (mutex) {
                    while (password == null) {
                        mutex.wait();
                    }
                    mutex.notifyAll();
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            return new KeyStore.PasswordProtection(password);
        }

        @Override
        public void handlerPassword(char[] password) {
            synchronized (mutex) {
                this.password = password;
                mutex.notifyAll();
            }
        }
    }
}
