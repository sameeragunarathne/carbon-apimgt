/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.gateway.mediators;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.wso2.carbon.apimgt.gateway.APIMgtGatewayConstants;
import org.wso2.carbon.apimgt.gateway.threatprotection.APIMThreatAnalyzerException;
import org.wso2.carbon.apimgt.gateway.threatprotection.AnalyzerHolder;
import org.wso2.carbon.apimgt.gateway.threatprotection.analyzer.APIMThreatAnalyzer;
import org.wso2.carbon.apimgt.gateway.threatprotection.configuration.ConfigurationHolder;
import org.wso2.carbon.apimgt.gateway.threatprotection.configuration.XMLConfig;
import org.wso2.carbon.apimgt.gateway.threatprotection.utils.ThreatExceptionHandler;
import org.wso2.carbon.apimgt.gateway.threatprotection.utils.ThreatProtectorConstants;
import org.wso2.carbon.apimgt.gateway.utils.GatewayUtils;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

/**
 * This mediator would protect the backend resources from the XML threat vulnerabilities by validating the
 * XML schema.
 */
public class XMLSchemaValidator extends AbstractMediator {
    /**
     * This mediate method validate the xml schema.
     *
     * @param messageContext This message context contains the request message properties of the relevant API which was
     *                       enabled the XML_Validator message mediation in flow.
     * @return A boolean value.True if successful and false if not.
     */
    public boolean mediate(MessageContext messageContext) {
        InputStream inputStreamSchema;
        InputStream inputStreamXml;
        Map<String, InputStream> inputStreams = null;
        Boolean xmlValidationStatus;
        Boolean schemaValidationStatus;
        APIMThreatAnalyzer apimThreatAnalyzer = null;
        String apiContext;
        String requestMethod;
        String contentType;
        boolean validInput = true;

        org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext) messageContext).
                getAxis2MessageContext();
        requestMethod = axis2MC.getProperty(ThreatProtectorConstants.HTTP_REQUEST_METHOD).toString();
        contentType = axis2MC.getProperty(ThreatProtectorConstants.CONTENT_TYPE).toString();
        apiContext = messageContext.getProperty(ThreatProtectorConstants.API_CONTEXT).toString();

        if (!APIConstants.SupportedHTTPVerbs.GET.name().equalsIgnoreCase(requestMethod)
                && (ThreatProtectorConstants.APPLICATION_XML.equals(contentType) ||
                ThreatProtectorConstants.TEXT_XML.equals(contentType))) {
            try {
                inputStreams = GatewayUtils.cloneRequestMessage(messageContext);
                if (inputStreams != null) {
                    Object messageProperty = messageContext.getProperty(APIMgtGatewayConstants.XML_VALIDATION);
                    if (messageProperty != null) {
                        xmlValidationStatus = Boolean.valueOf(messageProperty.toString());
                        if (xmlValidationStatus.equals(true)) {
                            apimThreatAnalyzer = AnalyzerHolder.getAnalyzer(contentType);
                            XMLConfig xmlConfig = configureSchemaProperties(messageContext);
                            ConfigurationHolder.addXmlConfig(xmlConfig);
                            inputStreamXml = inputStreams.get(ThreatProtectorConstants.XML);
                            apimThreatAnalyzer.analyze(inputStreamXml, apiContext);
                        }
                    }
                    messageProperty = messageContext.getProperty(APIMgtGatewayConstants.SCHEMA_VALIDATION);
                    if (messageProperty != null) {
                        schemaValidationStatus = Boolean.valueOf(messageProperty.toString());
                        if (schemaValidationStatus.equals(true)) {
                            inputStreamSchema = inputStreams.get(ThreatProtectorConstants.SCHEMA);
                            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStreamSchema);
                            validateSchema(messageContext, bufferedInputStream);
                        }

                    }
                }

            } catch (APIMThreatAnalyzerException e) {
                validInput = false;
                GatewayUtils.handleThreat(messageContext, ThreatProtectorConstants.HTTP_SC_CODE, e.getMessage());

            } catch (IOException e) {
                GatewayUtils.handleThreat(messageContext, ThreatProtectorConstants.HTTP_SC_CODE, e.getMessage());
            }

            //return analyzer to the pool
            AnalyzerHolder.returnObject(apimThreatAnalyzer);
        } else {
            GatewayUtils.handleThreat(messageContext, APIMgtGatewayConstants.HTTP_SC_CODE,
                    APIMgtGatewayConstants.CONTENT_TYPE_FAIL_MSG);
        }
        GatewayUtils.setOriginalInputStream(inputStreams, axis2MC);
        if (validInput) try {
            RelayUtils.buildMessage(axis2MC);
        } catch (IOException | XMLStreamException e) {
            GatewayUtils.handleThreat(messageContext, APIMgtGatewayConstants.HTTP_SC_CODE, e.getMessage());
        }
        return true;
    }

    /**
     * This configureSchemaProperties method bind the xml_validator sequence properties for the XMLConfig object.
     *
     * @param messageContext This message context contains the request message properties of the relevant API which was
     *                       enabled the XML_Validator message mediation in flow.
     * @return XMLConfig contains the xml schema properties need to be validated.
     */
    XMLConfig configureSchemaProperties(MessageContext messageContext) {
        Object messageProperty;
        boolean dtdEnabled = false;
        boolean externalEntitiesEnabled = false;
        int maxXMLDepth = 0;
        int elementCount = 0;
        int attributeLength = 0;
        int attributeCount = 0;
        int entityExpansionLimit = 0;
        int childrenPerElement = 0;

        messageProperty = messageContext.getProperty(ThreatProtectorConstants.DTD_ENABLED);
        if (messageProperty != null) {
            dtdEnabled = Boolean.valueOf(messageProperty.toString());
        } else {
            String message = "XML schema dtdEnabled property value is missing.";
            ThreatExceptionHandler.handleException(messageContext, message);
        }

        messageProperty = messageContext.getProperty(ThreatProtectorConstants.EXTERNAL_ENTITIES_ENABLED);
        if (messageProperty != null) {
            externalEntitiesEnabled = Boolean.valueOf(messageProperty.toString());
        } else {
            String message = "XML schema externalEntitiesEnabled property value is missing.";
            ThreatExceptionHandler.handleException(messageContext, message);
        }

        messageProperty = messageContext.getProperty(ThreatProtectorConstants.MAX_ELEMENT_COUNT);
        if (messageProperty != null) {
            elementCount = Integer.parseInt(messageProperty.toString());
        } else {
            String message = "XML schema elementCount property value is missing.";
            ThreatExceptionHandler.handleException(messageContext, message);
        }

        messageProperty = messageContext.getProperty(ThreatProtectorConstants.MAX_ATTRIBUTE_LENGTH);
        if (messageProperty != null) {
            attributeLength = Integer.parseInt(messageProperty.toString());
        } else {
            String message = "XML schema maxAttributeLength property value is missing.";
            ThreatExceptionHandler.handleException(messageContext, message);
        }

        messageProperty = messageContext.getProperty(ThreatProtectorConstants.MAX_XML_DEPTH);
        if (messageProperty != null) {
            maxXMLDepth = Integer.parseInt(messageProperty.toString());
        } else {
            String message = "XML schema xmlDepth property value is missing.";
            ThreatExceptionHandler.handleException(messageContext, message);
        }

        messageProperty = messageContext.getProperty(ThreatProtectorConstants.MAX_ATTRIBUTE_COUNT);
        if (messageProperty != null) {
            attributeCount = Integer.parseInt(messageProperty.toString());
        } else {
            String message = "XML schema attributeCount property value is missing.";
            ThreatExceptionHandler.handleException(messageContext, message);
        }

        messageProperty = messageContext.getProperty(ThreatProtectorConstants.ENTITY_EXPANSION_LIMIT);
        if (messageProperty != null) {
            entityExpansionLimit = Integer.parseInt(messageProperty.toString());

        } else {
            String message = "XML schema entityExpansionLimit property value is missing.";
            ThreatExceptionHandler.handleException(messageContext, message);
        }

        messageProperty = messageContext.getProperty(ThreatProtectorConstants.CHILDREN_PER_ELEMENT);
        if (messageProperty == null) {
            String message = "XML schema childrenElement property value is missing.";
            ThreatExceptionHandler.handleException(messageContext, message);
        } else {
            childrenPerElement = Integer.parseInt(messageProperty.toString());
        }

        if (log.isDebugEnabled()) {
            log.debug(("DTD enable:" + dtdEnabled) + ", " +
                    "External entities: " + externalEntitiesEnabled + ", " +
                    "Element Count:" + elementCount + ", " +
                    "Max AttributeLength:" + attributeLength + ", " +
                    "Max xml Depth:" + maxXMLDepth + ", " +
                    "Attribute count:" + attributeCount + ", " +
                    "Entity Expansion Limit" + attributeCount + ". " +
                    "childrenElement:" + attributeCount);
        }
        XMLConfig xmlConfig = new XMLConfig();
        xmlConfig.setDtdEnabled(dtdEnabled);
        xmlConfig.setExternalEntitiesEnabled(externalEntitiesEnabled);
        xmlConfig.setMaxDepth(maxXMLDepth);
        xmlConfig.setMaxElementCount(elementCount);
        xmlConfig.setMaxAttributeCount(attributeCount);
        xmlConfig.setMaxAttributeLength(attributeLength);
        xmlConfig.setEntityExpansionLimit(entityExpansionLimit);
        xmlConfig.setMaxChildrenPerElement(childrenPerElement);

        return xmlConfig;
    }

    /**
     * This method checks the status of the {enabledCheckBody} property which comes from the custom sequence.
     * If a client ask to check the message body,Method returns true else It will return false.
     * If the {isContentAware} method returns false, The request message payload wont be build.
     * Building a payload will directly affect to the performance.
     *
     * @return If enabledCheckBody is true,The method returns true else it returns false
     */
    public boolean isContentAware() {
        return false;
    }

    /**
     * This method validates the request payload xml with the relevant xsd.
     *
     * @param messageContext This message context contains the request message properties of the relevant API which was
     *                       enabled the XML_Validator message mediation in flow.
     * @return This method returns the success or not (true/false) status of the schema validation.
     */
    private boolean validateSchema(MessageContext messageContext, BufferedInputStream bufferedInputStream)
            throws APIMThreatAnalyzerException {
        String xsdURL;
        String xsdFilePath;
        Schema schema;
        InputStream inputStream;
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Object messageProperty = messageContext.getProperty(APIMgtGatewayConstants.XSD_FILEPATH);
            if (messageProperty == null) {
                messageProperty = messageContext.getProperty(APIMgtGatewayConstants.XSD_URL);
                if (messageProperty == null) {
                    return true;
                } else {
                    if (String.valueOf(messageProperty).isEmpty()) {
                        return true;
                    } else {
                        xsdURL = String.valueOf(messageProperty);
                        URL schemaFile = new URL(xsdURL);
                        schema = schemaFactory.newSchema(schemaFile);
                        Source xmlFile = new StreamSource(bufferedInputStream);
                        Validator validator = schema.newValidator();
                        validator.validate(xmlFile);
                    }
                }
            } else {

                if (String.valueOf(messageProperty).isEmpty()) {
                    return true;
                } else {
                    xsdFilePath = String.valueOf(messageProperty);
                    inputStream = new FileInputStream(xsdFilePath);
                    Source streamSource = new StreamSource(inputStream);
                    schema = schemaFactory.newSchema(streamSource);
                    CloseShieldInputStream closeShieldInputStream = new CloseShieldInputStream(bufferedInputStream);
                    Source xmlFile = new StreamSource(closeShieldInputStream);
                    Validator validator = schema.newValidator();
                    validator.validate(xmlFile);
                }
            }
        } catch (SAXException | IOException e) {
            throw new APIMThreatAnalyzerException("Error occurred while parsing XML payload : " + e);
        }
        return true;
    }
}

