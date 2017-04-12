/*
 * RapidPro Android Channel - Relay SMS messages where MNO connections aren't practical.
 * Copyright (C) 2014 Nyaruka, UNICEF
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.rapidpro.androidchannel.util;

import io.rapidpro.androidchannel.RapidPro;
import org.apache.http.conn.ssl.SSLSocketFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class EasySSLSocketFactory extends SSLSocketFactory {

    SSLContext sslContext = SSLContext.getInstance("TLS");

    public EasySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException,
            KeyStoreException, UnrecoverableKeyException {

        super(truststore);

        TrustManager tm = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                //RapidPro.LOG.d("CLIENT: Certificates ignored, dumping what is there:");
                //RapidPro.LOG.d("  checkClientTrusted: " + chain + "; " + authType);
                if (chain != null) {
                    //RapidPro.LOG.d("  authType: " + authType);
                    for (X509Certificate cert : chain) {
                        //RapidPro.LOG.d(cert.toString());
                    }
                }
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                //RapidPro.LOG.d("SERVER: Certificates ignored, dumping what is there:");
                //RapidPro.LOG.d("  checkServerTrusted: " + chain + "; " + authType);
                if (chain != null) {
                    // RapidPro.LOG.d("  authType: " + authType);
                    for (X509Certificate cert : chain) {
                        // RapidPro.LOG.d(cert.toString());
                    }
                }
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        sslContext.init(null, new TrustManager[] { tm }, null);

    }

    @Override
    public Socket createSocket(Socket socket, String host, int port,
                               boolean autoClose) throws IOException, UnknownHostException {
        return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket() throws IOException {
        return sslContext.getSocketFactory().createSocket();
    }

}
