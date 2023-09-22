package org.custom;

import org.apache.axiom.om.util.Base64;
import org.apache.axiom.util.UIDGenerator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

import javax.activation.DataHandler;

public class ForceMIME extends AbstractMediator {

    public boolean mediate(MessageContext messageContext) {

        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext).getAxis2MessageContext();
        axis2MessageContext.addAttachment(new DataHandler(Base64.decode((String) messageContext.getProperty("multipart.body.file")), "application/pdf"));
        axis2MessageContext.setDoingSwA(true);
        axis2MessageContext.setDoingREST(false);
        String boundary = UIDGenerator.generateMimeBoundary();
        String ct = "multipart/related; boundary=" + boundary;
        axis2MessageContext.setProperty("mimeBoundary",boundary);
        axis2MessageContext.setProperty("ContentType",ct);
        return true;
    }
}

