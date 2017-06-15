package grace.core.factory;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import grace.core.http.Executor;
import grace.core.http.Request;
import grace.core.http.Response;
import grace.core.util.ExecutorUtil;
import grace.core.util.GLogger;

import static grace.core.util.ExecutorUtil.CHARSET_UTF_8;

/**
 * Created by hechao on 2017/4/11.
 */

final class SimpleExecutor implements Executor.SynExecutor<String> {
    private static final int CONNECT_TIMEOUT = 30 * 1000;

    private static final int READ_TIMEOUT = 30 * 1000;

    private SSLContext sslContext;

    private List<String> hostNames;

    SimpleExecutor(SSLContext sslContext, List<String> hostNames) {
        this.sslContext = sslContext;
        this.hostNames = hostNames;
    }

    SimpleExecutor() {

    }

    @Override
    public Response<String> executeRequest(Request request){
        try {
            return _executeRequest(request);
        }catch (Exception e){
            e.printStackTrace();
            return new Response.Builder().status(Response.Status.UNEXPECTED).code(-1).message("execute failed").build();
        }
    }

    private Response<String> _executeRequest(Request request) {
        if(request == null){
            return new Response.Builder().status(Response.Status.INVALID_URL).code(-1).message("No request info").build();
        }

        String urlStr = ExecutorUtil.formUrl(request);

        if (urlStr == null || urlStr.isEmpty()) {
            return new Response.Builder().status(Response.Status.INVALID_URL).code(-1).message("empty url").build();
        }

        final URL url;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            return new Response.Builder().status(Response.Status.INVALID_URL).code(-1).message(String.format("URL(%s) init failed",urlStr)).build();
        }
        final HttpURLConnection connection;
        if(request.protocol() == Request.Protocol.HTTP){
            try {
                connection = createHttpConnection(url);
            } catch (IOException e) {
                e.printStackTrace();
                return new Response.Builder().status(Response.Status.CONNECTION_FAILED).code(-1).message("open http connection failed").build();
            }
        }else{
            try {
                connection = createHttpsConnection(url);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return new Response.Builder().status(Response.Status.CONNECTION_FAILED).code(-1).message("init https connection failed").build();
            } catch (KeyManagementException e) {
                e.printStackTrace();
                return new Response.Builder().status(Response.Status.CONNECTION_FAILED).code(-1).message("init https connection failed").build();
            } catch (IOException e) {
                e.printStackTrace();
                return new Response.Builder().status(Response.Status.CONNECTION_FAILED).code(-1).message("open https connection failed").build();
            }
        }

        try {
            initConnection(request,connection);
        } catch (ProtocolException e) {
            return new Response.Builder().status(Response.Status.CONNECTION_FAILED).code(-1).message(String.format("invalid http method:%s",request.method().name())).build();
        }

        if(request.method() == Request.Method.POST){
            if (request.body() != null && !request.body().isEmpty()) {
                try {
                    writeBody(connection,request.body());
                } catch (IOException e) {
                    e.printStackTrace();
                    return new Response.Builder().status(Response.Status.UNEXPECTED).code(-1).message(String.format("write body failed :%s",request.body())).build();
                }
            }
        }

        final int code;
        try {
            code = connection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            return new Response.Builder().code(-1).status(Response.Status.UNEXPECTED).message("Get http response code failed").build();
        }

        InputStream inputStream = null;
        String message = null;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            message = "Get input stream failed!";
        }

        Map<String,String> headers = new HashMap<>();
        for(Map.Entry<String,List<String>> entry :connection.getHeaderFields().entrySet()){
            headers.put(entry.getKey(), Arrays.toString(entry.getValue().toArray()));
        }

        String body;
        if(inputStream == null){
            body = "";
        }else{
            try {
                body = inputStreamToString(inputStream,getDataCharset(connection.getHeaderField("Content-Type")));
            } catch (IOException e) {
                e.printStackTrace();
                message = "Read input stream failed!";
                body = "";
            }
        }


        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (connection != null) {
            connection.disconnect();
        }

        if(code != 200) message = String.format("%s,url(%s)",message,urlStr);
        return new Response.Builder().code(code).headers(headers).message(message).status(Response.Status.OK).build(body);
    }

    private String getDataCharset(String contentType){
        String charset = CHARSET_UTF_8;
        GLogger.info("contentType: "+contentType);
        if(contentType == null || contentType.isEmpty()) return charset;
        String[] values = contentType.split(";");
        for (String value : values){
            if(value != null && value.trim().toLowerCase().startsWith("charset")){
                int index = value.indexOf('=');
                if(index > 0 && index < value.length() - 1){
                    String charsetValue = value.substring(index+1);
                    if(charsetValue != null && !charsetValue.isEmpty()){
                        charset = charsetValue;
                        GLogger.info("data charset is:"+charset);
                    }
                }
                break;
            }
        }

        return charset;
    }

    private void writeBody(HttpURLConnection connection,String body) throws IOException {
        byte[] data = body.getBytes(CHARSET_UTF_8);
        connection.setFixedLengthStreamingMode(data.length);
        OutputStream out = new BufferedOutputStream(connection.getOutputStream());
        out.write(data);
        out.flush();
        out.close();
    }

    private static void initConnection(Request request, HttpURLConnection connection) throws ProtocolException {
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setRequestMethod(request.method().name());
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(false);
        for (Map.Entry<String,String> entry : request.headers().entrySet()){
            String field = entry.getKey();
            String value = entry.getValue();
            if(field != null && !field.isEmpty() && value != null && !value.isEmpty()){
                connection.setRequestProperty(field,value);
            }
        }
        if (request.method() == Request.Method.POST) {
            connection.setDoOutput(true);
        }else{
            connection.setDoInput(true);
            connection.setDoOutput(false);
        }
    }

    private HttpURLConnection createHttpConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    private HttpsURLConnection createHttpsConnection(URL url) throws NoSuchAlgorithmException, KeyManagementException, IOException {
        SSLContext sslContext = this.sslContext == null ? createSSLContext("SSL") : this.sslContext;
        final List<String> hostNames = this.hostNames == null ? new ArrayList<String>() : this.hostNames;

        HostnameVerifier verifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                for (String host : hostNames) {
                    if (host.equalsIgnoreCase(hostname)) {
                        return true;
                    }
                }
                return false;
            }
        };
        HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) url.openConnection();
        httpsUrlConnection.setSSLSocketFactory(sslContext.getSocketFactory());
        httpsUrlConnection.setHostnameVerifier(verifier);
        return httpsUrlConnection;
    }

    private static SSLContext createSSLContext(String protocol) throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager manager = new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }
        };

        SSLContext sslContext = SSLContext.getInstance(protocol);
        sslContext.init(null, new TrustManager[]{manager}, new SecureRandom());
        return sslContext;
    }

    private static String inputStreamToString(InputStream is, String charsetName)
            throws IOException, IllegalCharsetNameException {
        byte[] data = inputStreamToByteArray(is);
        if (null == data) {
            return "";
        }
        if (null == charsetName || !Charset.isSupported(charsetName)) {
            return new String(data, CHARSET_UTF_8);
        } else {
            return new String(data, charsetName);
        }
    }

    private static byte[] inputStreamToByteArray(InputStream is) throws IOException {
        return inputStreamToByteArray(is, 1024);
    }

    private static byte[] inputStreamToByteArray(InputStream is, int bufferSize) throws IOException {
        if (null == is) {
            return null;
        }
        if (bufferSize < 1) {
            bufferSize = 1;
        }
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the
        // byteBuffer
        int len = 0;
        while ((len = is.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        byteBuffer.close();
        is.close();

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }
}
