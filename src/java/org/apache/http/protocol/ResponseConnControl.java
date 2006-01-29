/*
 * $HeadURL$
 * $Revision$
 * $Date$
 *
 * ====================================================================
 *
 *  Copyright 1999-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.protocol;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpMutableResponse;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;

/**
 * <p>
 * </p>
 * @author <a href="mailto:oleg at ural.ru">Oleg Kalnichevski</a>
 *
 * @version $Revision$
 * 
 * @since 4.0
 */
public class ResponseConnControl implements HttpResponseInterceptor {

    public ResponseConnControl() {
        super();
    }
    
    public void process(final HttpMutableResponse response, final HttpContext context) 
        throws HttpException, IOException {
        if (response == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("HTTP context may not be null");
        }
        // Always drop connection after certain type of responses
        int status = response.getStatusLine().getStatusCode();
        if (status == HttpStatus.SC_BAD_REQUEST ||
        		status == HttpStatus.SC_REQUEST_TIMEOUT ||
        		status == HttpStatus.SC_LENGTH_REQUIRED ||
        		status == HttpStatus.SC_REQUEST_TOO_LONG ||
        		status == HttpStatus.SC_REQUEST_URI_TOO_LONG ||
        		status == HttpStatus.SC_INTERNAL_SERVER_ERROR ||
        		status == HttpStatus.SC_SERVICE_UNAVAILABLE ||
        		status == HttpStatus.SC_NOT_IMPLEMENTED) {
            response.setHeader(new Header(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE, true));
            return;
        }
        // Always drop connection for HTTP/1.0 responses and below
        // if the content body cannot be correctly delimited
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            HttpVersion ver = response.getStatusLine().getHttpVersion();
            if (entity.getContentLength() < 0 && 
                    (!entity.isChunked()) || ver.lessEquals(HttpVersion.HTTP_1_0)) {
                response.setHeader(new Header(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE, true));
                return;
            }
        }
        // Drop connection if requested by the client
        HttpRequest request = (HttpRequest) context.getAttribute(HttpContext.HTTP_REQUEST);
        Header header = request.getFirstHeader(HTTP.CONN_DIRECTIVE);
        if (header != null) {
        	response.setHeader(new Header(HTTP.CONN_DIRECTIVE, header.getValue(), true));
        }
    }
    
}
