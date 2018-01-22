/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.apimgt.impl.soaptorest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.carbon.apimgt.api.APIDefinition;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.definitions.APIDefinitionFromSwagger20;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.soaptorest.model.WSDLSOAPOperation;
import org.wso2.carbon.apimgt.impl.soaptorest.template.RESTToSOAPMsgTemplate;
import org.wso2.carbon.apimgt.impl.soaptorest.util.SequenceUtils;
import org.wso2.carbon.apimgt.impl.soaptorest.util.SOAPToRESTConstants;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.wso2.carbon.apimgt.impl.utils.APIUtil.handleException;

public class SequenceGenerator {

    private static APIDefinition definitionFromSwagger20 = new APIDefinitionFromSwagger20();

    public static void generateSequences(String apiDataStr, String soapOperationMapping) throws APIManagementException {
        JSONParser parser = new JSONParser();
        boolean isTenantFlowStarted = false;
        try {
            JSONObject apiData = (JSONObject) parser.parse(apiDataStr);
            String provider = (String) apiData.get("provider");
            String name = (String) apiData.get("name");
            String version = (String) apiData.get("version");

            if (provider != null) {
                provider = APIUtil.replaceEmailDomain(provider);
            }

            provider = (provider != null ? provider.trim() : null);
            name = (name != null ? name.trim() : null);
            version = (version != null ? version.trim() : null);
            APIIdentifier apiId = new APIIdentifier(provider, name, version);

            JSONObject apiJSON;

            String tenantDomain = MultitenantUtils.getTenantDomain(APIUtil.replaceEmailDomainBack(provider));
            if (tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
            }
            RegistryService registryService = ServiceReferenceHolder.getInstance().getRegistryService();
            int tenantId;
            UserRegistry registry;
            try {
                tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                        .getTenantId(tenantDomain);
                registry = registryService.getGovernanceSystemRegistry(tenantId);

                apiJSON = (JSONObject) parser.parse(definitionFromSwagger20.getAPIDefinition(apiId, registry));

                ObjectMapper mapper = new ObjectMapper()
                        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                TypeFactory typeFactory = mapper.getTypeFactory();
                List<WSDLSOAPOperation> soapOperations = mapper.readValue(soapOperationMapping,
                        typeFactory.constructCollectionType(List.class, WSDLSOAPOperation.class));

                if (apiJSON != null) {
                    Map pathMap = (HashMap) apiJSON.get(SOAPToRESTConstants.SWAGGER.PATHS);
                    for (Object resourceObj : pathMap.entrySet()) {
                        Map.Entry entry = (Map.Entry) resourceObj;
                        String resourcePath = (String) entry.getKey();
                        JSONObject resource = (JSONObject) entry.getValue();

                        Set methods = resource.keySet();
                        for (Object key1 : methods) {
                            String method = (String) key1;

                            List<JSONObject> mappingList = SequenceUtils
                                    .getResourceParametersFromSwagger(apiJSON, resource, method);
                            String inSequence = generateApiInSequence(mappingList, soapOperations, resourcePath,
                                    method);
                            String outSequence = generateApiOutSequence();
                            String resourceInPath = APIConstants.API_LOCATION + RegistryConstants.PATH_SEPARATOR +
                                    provider + RegistryConstants.PATH_SEPARATOR + name
                                    + RegistryConstants.PATH_SEPARATOR + version + RegistryConstants.PATH_SEPARATOR
                                    + SOAPToRESTConstants.SEQUENCE_GEN.SOAP_TO_REST_IN_RESOURCE + resourcePath
                                    + SOAPToRESTConstants.SEQUENCE_GEN.RESOURCE_METHOD_SEPERATOR + method
                                    + SOAPToRESTConstants.SEQUENCE_GEN.XML_FILE_EXTENSION;
                            String resourceOutPath = APIConstants.API_LOCATION + RegistryConstants.PATH_SEPARATOR +
                                    provider + RegistryConstants.PATH_SEPARATOR + name
                                    + RegistryConstants.PATH_SEPARATOR + version + RegistryConstants.PATH_SEPARATOR
                                    + SOAPToRESTConstants.SEQUENCE_GEN.SOAP_TO_REST_OUT_RESOURCE + resourcePath
                                    + SOAPToRESTConstants.SEQUENCE_GEN.RESOURCE_METHOD_SEPERATOR + method
                                    + SOAPToRESTConstants.SEQUENCE_GEN.XML_FILE_EXTENSION;
                            SequenceUtils.saveRestToSoapConvertedSequence(registry, inSequence, method, resourceInPath);
                            SequenceUtils.saveRestToSoapConvertedSequence(registry, outSequence, method, resourceOutPath);
                        }
                    }
                }
            } catch (RegistryException e) {
                handleException("Error when create registry instance ", e);
            } catch (UserStoreException e) {
                handleException("Error while reading tenant information ", e);
            } catch (ParseException e) {
                handleException("Error while parsing json content", e);
            } catch (IOException e) {
                handleException("Error occurred when parsing json string ", e);
            }
        } catch (ParseException e) {
            handleException("Error occurred when parsing api json string ", e);
        } finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
    }

    /**
     * Generates api in sequence for api resource that needs to added to synapse api configs
     *
     * @return api in sequence
     */
    private static String generateApiInSequence(List<JSONObject> mappingList, List<WSDLSOAPOperation> soapOperations, String resourcePath, String method)
            throws APIManagementException {
        RESTToSOAPMsgTemplate template = new RESTToSOAPMsgTemplate();

        String soapAction = "";
        String namespace = "";
        String opName = "";
        for (WSDLSOAPOperation operationParam : soapOperations) {
            if (operationParam.getName().equals(resourcePath.substring(1)) && operationParam.getHttpVerb()
                    .equalsIgnoreCase(method)) {
                opName = operationParam.getSoapBindingOpName();
                soapAction = operationParam.getSoapAction();
                namespace = operationParam.getTargetNamespace();
                break;
            }
        }
        Map<String, String> sequenceMap = createXMLFromMapping(mappingList, opName, namespace);
        return template.getMappingInSequence(sequenceMap, opName, soapAction, namespace);
    }

    /**
     * Generates api out sequence for api resource that needs to added to synapse api configs
     *
     * @return api outsequence
     */
    private static String generateApiOutSequence() {
        RESTToSOAPMsgTemplate template = new RESTToSOAPMsgTemplate();
        return template.getMappingOutSequence();
    }

    private static Map<String, String> createXMLFromMapping(List<JSONObject> mappingList, String opName, String namespace)
            throws APIManagementException {

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        StringWriter stringWriter = new StringWriter();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Map<String, String> map = new HashMap<>();
        String argStr = "";
        String propertyStr = "";
        try {
            docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            doc.createElementNS(namespace, "web:" + opName);
            Transformer transformer = transformerFactory.newTransformer();
            String ns = "web";
            Element rootElement = doc.createElementNS(namespace, ns + SOAPToRESTConstants.SEQUENCE_GEN.NAMESPACE_SEPARATOR + opName);
            doc.appendChild(rootElement);
            int count = 1;
            for (JSONObject jsonObject : mappingList) {
                for (Object obj : jsonObject.keySet()) {
                    if(jsonObject.get(obj) instanceof JSONArray) {
                        JSONArray paramArr = (JSONArray) jsonObject.get(obj);
                        for (Object paramObj : paramArr) {
                            JSONObject param = (JSONObject) paramObj;
                            String paramName = (String) param.keySet().iterator().next();
                            JSONObject entry = (JSONObject) ((JSONObject) paramObj).get(paramName);
                            String xPath = (String) entry.get(SOAPToRESTConstants.SEQUENCE_GEN.XPATH);
                            String paramElements = createParameterElements(paramName, SOAPToRESTConstants.SWAGGER.BODY);
                            String[] params = paramElements.split(SOAPToRESTConstants.SEQUENCE_GEN.COMMA);
                            argStr += params[1] + SOAPToRESTConstants.SEQUENCE_GEN.NEW_LINE_CHAR;
                            propertyStr += params[0] + SOAPToRESTConstants.SEQUENCE_GEN.NEW_LINE_CHAR;
                            String[] xPathElements = xPath.split(SOAPToRESTConstants.SEQUENCE_GEN.X_PATH_SEPARATOR);

                            Element prevElement = rootElement;
                            int elemPos = 0;
                            for (String xPathElement : xPathElements) {
                                Element element = doc.createElementNS(namespace, "web:" + xPathElement);
                                if(doc.getElementsByTagName(element.getTagName()).getLength() > 0) {
                                    prevElement = (Element) doc.getElementsByTagName(element.getTagName()).item(0);
                                } else {
                                    if(elemPos == xPathElements.length - 1) {
                                        element.setTextContent("$" + count);
                                        count++;
                                    }
                                    prevElement.appendChild(element);
                                    prevElement = element;
                                }
                                elemPos++;
                            }
                        }
                    } else if(jsonObject.get(obj) instanceof JSONObject) {
                        JSONObject param = (JSONObject) jsonObject.get(obj);
                        Element element = doc.createElementNS(namespace, "web:" + param.get(SOAPToRESTConstants.SWAGGER.NAME).toString());
                        element.setTextContent("$" + count);
                        rootElement.appendChild(element);
                        String paramElements = createParameterElements(param.get(SOAPToRESTConstants.SWAGGER.NAME).toString(), SOAPToRESTConstants.PARAM_TYPES.QUERY);
                        String[] params = paramElements.split(SOAPToRESTConstants.SEQUENCE_GEN.COMMA);
                        argStr += params[1] + SOAPToRESTConstants.SEQUENCE_GEN.NEW_LINE_CHAR;
                        propertyStr += params[0] + SOAPToRESTConstants.SEQUENCE_GEN.NEW_LINE_CHAR;
                        count++;
                    }
                }
            }
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(SOAPToRESTConstants.SEQUENCE_GEN.INDENT_PROPERTY, SOAPToRESTConstants.SEQUENCE_GEN.INDENT_VALUE);
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(doc), new StreamResult(stringWriter));
        } catch (ParserConfigurationException e) {
            handleException("Error occurred when building in sequence xml ", e);
        } catch (TransformerConfigurationException e) {
            handleException("Error in transport configuration ", e);
        } catch (TransformerException e) {
            handleException("Error occurred when transforming in sequence xml ", e);
        }
        map.put("properties", propertyStr);
        map.put("args", argStr);
        map.put("sequence", stringWriter.toString());
        return map;
    }

    private static String createParameterElements(String jsonPathElement, String type) throws APIManagementException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        StringWriter stringWriter = new StringWriter();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        String property = "";
        String argument = "";
        try {
            Transformer transformer = transformerFactory.newTransformer();
            docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element argElement = doc.createElement("arg");
            Element propertyElement = doc.createElement("property");
            argElement.setAttribute("evaluator", "xml");
            String expressionAttr = "get-property('req.var." + jsonPathElement + "')";
            argElement.setAttribute("expression", expressionAttr);
            propertyElement.setAttribute("name", "req.var." + jsonPathElement);
            if(type.equals(SOAPToRESTConstants.PARAM_TYPES.QUERY)) {
                propertyElement.setAttribute("expression", "$url:" + jsonPathElement);
            } else {
                propertyElement.setAttribute("expression", "$." + jsonPathElement);
            }
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(propertyElement), new StreamResult(stringWriter));
            property = stringWriter.toString();
            stringWriter = new StringWriter();
            transformer.transform(new DOMSource(argElement), new StreamResult(stringWriter));
            argument = stringWriter.toString();
        } catch (ParserConfigurationException e) {
            handleException("Error occurred when building in arg elements ", e);
        } catch (TransformerConfigurationException e) {
            handleException("Error in transport configuration ", e);
        } catch (TransformerException e) {
            handleException("Error occurred when transforming in sequence xml ", e);
        }
        return property + "," + argument;
    }
}
