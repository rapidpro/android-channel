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

import android.net.http.AndroidHttpClient;
import android.os.Build;
import io.rapidpro.androidchannel.RapidPro;
import io.rapidpro.androidchannel.json.JSON;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.*;

/**
 * HttpWrapper wraps all http interaction to allow for intelligent
 * caching and shared client settings.
 *
 * All HTTP calls pass through this class, which makes sure that caching is done
 * intelligently.
 */
public class Http {
    private static final int TIMEOUT = 120000;
    private static final String USER_AGENT = "RapidPro/" + Build.VERSION.RELEASE;

    private DefaultHttpClient m_client;

    public Http() {
        initializeClient();
    }

    private void initializeClient() {
        m_client = new TrustingHttpClient(RapidPro.get());
        HttpParams params = m_client.getParams();
        params.setParameter("http.connection-manager.timeout", Integer.valueOf(Http.TIMEOUT));
        params.setParameter("http.connection.timeout", Integer.valueOf(Http.TIMEOUT));
        params.setParameter("http.socket.timeout", Integer.valueOf(TIMEOUT));
        params.setParameter("http.headers.user-agent", USER_AGENT);
        m_client.setParams(params);

        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);
    }

    /**
     * Converts an InputStream to a byte array
     * @param is
     * @return
     * @throws IOException
     */
    public byte[] readStreamFully(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = null;
        try{
            buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            byte[] output = buffer.toByteArray();
            return output;
        } finally {
            // close our streams
            is.close();
            buffer.close();
        }
    }

    /**
     * Fetch content from the wire, first checking if there is a cached version available.
     */
    public byte[] fetch(HttpRequestBase request) throws IOException {
        // add that we accept GZIP compressed content
        request.setHeader("Accept-Encoding", "gzip");

        // dump the request to the console
        if (RapidPro.SHOW_WIRE) {

            RapidPro.LOG.d("    " + request.getMethod() + " " + request.getURI());
            for (Header header : request.getAllHeaders()) {
                RapidPro.LOG.d("    > " + header.getName() + ": " + header.getValue());
            }
        }

        long start = System.currentTimeMillis();

        try {
            HttpResponse response = m_client.execute(request);
            InputStream inputStream = new BufferedInputStream(AndroidHttpClient.getUngzippedContent(response.getEntity()));
            byte[] content = readStreamFully(inputStream);

            if (RapidPro.SHOW_WIRE) {
                String body = new String(content);
                RapidPro.LOG.d("\n    " + response.getStatusLine().toString());
                RapidPro.LOG.d("    Received response with " + content.length + " bytes");

                for (Header header : response.getAllHeaders()) {
                    RapidPro.LOG.d("    < " + header.getName() + ": " + header.getValue());
                }

                RapidPro.LOG.d("    " + body);
                RapidPro.LOG.d("\n");
            }

            return content;

        } catch (IOException e) {
            request.abort();

            // no cached response?  throw our exception
            throw e;
        } finally {
            RapidPro.LOG.d("Fetch took: " + (System.currentTimeMillis() - start));
        }
    }

    public JSON fetchJSON(String url, String postData) throws IOException {
        HttpPost post = new HttpPost(url);
        post.addHeader("Content-Type", "application/json; charset=UTF-8");
        post.setEntity(new StringEntity(postData, "UTF-8"));

        // fetch our response, we use a cache if available
        return new JSON(new String(fetch(post)));
    }
}
