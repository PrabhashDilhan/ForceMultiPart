# AsyncHTTPClient


This class mediator can be used to convert multipart/form-data requests to multipart/related inside the API. The below mediation sequence should be used to invoke the class mediator and generate the soap message.
If you send the multipart/form-data request as content and the attachment as the file. You can read those into properties. Here we read the file into the ultipart.body.file property and we use that inside the class mediators to generate the multipart-related attachment.

```
<sequence xmlns="http://ws.apache.org/ns/synapse" name="custom_policy">

<header description="SOAPAction" name="SOAPAction" scope="transport" value=""/>
<property name="REST_URL_POSTFIX" scope="axis2" action="remove"/>
<property expression="json-eval($.mediate.content.$)" name="multipart.body.json"/>
<property expression="json-eval($.mediate.file.$)" name="multipart.body.file"/>

<property name="enableSwA" value="true" scope="axis2"/>
<property name="messageType" scope="axis2" type="STRING" value="text/xml"/>

<payloadFactory description="transform" media-type="xml">
  <format>
  <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:tem="http://tempuri.org/">
      <soapenv:Header/>
      <soapenv:Body>
          <tem:test>
              <Name>test</Name>
              <age>test</age>
          </tem:test>
      </soapenv:Body>
    </soapenv:Envelope>
  </format>
  <args/>
</payloadFactory>
<class name="org.custom.ForceMIME"/>
</sequence>
