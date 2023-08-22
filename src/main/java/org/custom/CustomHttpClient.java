package org.custom;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.Duration;
import org.apache.axiom.om.OMElement;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.utils.CarbonUtils;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class CustomHttpClient extends AbstractMediator implements ManagedLifecycle {

    private static final Log log = LogFactory.getLog(CustomHttpClient.class);

    private String connectionTimeout = "30";

    private String socketTimeout = "60";

    private String backEndUrl = null;

    private HttpClient httpClient;

    @Override
    public boolean mediate(MessageContext messageContext) {

        try {
            if(backEndUrl==null){
                log.error("Back end Url is empty, Please provide the back end url as a class mediator parameter");
                return true;
            }
            org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext).getAxis2MessageContext();
            String requestBody = axis2MessageContext.getEnvelope().getBody().getText();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(backEndUrl))
                    .timeout(Duration.ofSeconds(Long.parseLong(socketTimeout)))
                    .header("Content-Type","text/plain")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        int statusCode = response.statusCode();
                        axis2MessageContext.getEnvelope().getBody().getFirstElement().detach();
                        // Define the namespace URI and prefix
                        String namespaceURI = "http://ws.apache.org/commons/ns/payload";
                        String namespacePrefix = "xmlns";

                        // Create an OMNamespace with the namespace URI and prefix
                        OMNamespace namespace = OMAbstractFactory.getOMFactory().createOMNamespace(namespaceURI, namespacePrefix);
                        OMElement textElement = OMAbstractFactory.getOMFactory().createOMElement("text", namespace);
                        textElement.setText(response.body());
                        axis2MessageContext.getEnvelope().getBody().addChild(textElement);
                        axis2MessageContext.setProperty("HTTP_SC",statusCode);
                    })
                    .join(); // This will block until the response is available.

        } catch (Exception e) {
            handleException("Exception occured in the CustomHttpClient class mediator", e);
        }
        return true;
    }
    private static SSLContext createSSLContext() {

        String keyStorePath = CarbonUtils.getServerConfiguration()
                .getFirstProperty(APIConstants.TRUST_STORE_LOCATION);
        String keyStorePassword = CarbonUtils.getServerConfiguration()
                .getFirstProperty(APIConstants.TRUST_STORE_PASSWORD);
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");

            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            sslContext.init(null,trustManagerFactory.getTrustManagers(),null);
            return sslContext;
        } catch (KeyStoreException e) {
            handleException("Failed to read from Key Store", e);
        } catch (IOException e) {
            handleException("Key Store not found in " + keyStorePath, e);
        } catch (CertificateException e) {
            handleException("Failed to read Certificate", e);
        } catch (NoSuchAlgorithmException e) {
            handleException("Failed to load Key Store from " + keyStorePath, e);
        } catch (KeyManagementException e) {
            handleException("Failed to load key from" + keyStorePath, e);
        } catch (Exception e){
            handleException("Exception occurred when creating ssl context",e);
        }
        return null;
    }

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
        try {
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(Long.parseLong(connectionTimeout)))
                    .sslContext(createSSLContext())
                    .build();
        } catch (Exception e) {
            handleException("CustomHttpClient class mediator initialisation failed ",e);
        }
    }
    @Override
    public void destroy() {

    }
    public static void handleException(String msg, Throwable t) throws SynapseException {
        log.error(msg,t);
        throw new SynapseException(msg, t);
    }
    public String getConnectionTimeout() {
        return connectionTimeout;
    }
    public void setConnectionTimeout(String connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    public String getSocketTimeout() {
        return socketTimeout;
    }
    public void setSocketTimeout(String socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public String getBackEndUrl() {
        return backEndUrl;
    }
    public void setBackEndUrl(String backEndUrl) {
        this.backEndUrl = backEndUrl;
    }
}

