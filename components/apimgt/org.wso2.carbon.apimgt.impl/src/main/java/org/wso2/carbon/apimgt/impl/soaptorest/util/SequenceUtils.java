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
package org.wso2.carbon.apimgt.impl.soaptorest.util;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.soaptorest.model.WSDLComplexType;
import org.wso2.carbon.apimgt.impl.soaptorest.model.WSDLOperationParam;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.registry.api.Resource;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.ResourceImpl;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.wso2.carbon.apimgt.impl.utils.APIUtil.handleException;

public class SequenceUtils {

    private static final String SOAP_TO_REST_RESOURCE = "soap_to_rest";

    public static void saveRestToSoapConvertedSequence(UserRegistry registry, String sequence, String method,
            String resourcePath) throws APIManagementException {
        try {
            Resource regResource;
            if (!registry.resourceExists(resourcePath)) {
                regResource = registry.newResource();
            } else {
                regResource = registry.get(resourcePath);
            }
            regResource.setContent(sequence);
            regResource.addProperty(SOAPToRESTConstants.METHOD, method);
            regResource.setMediaType("text/xml");
            registry.put(resourcePath, regResource);
        } catch (RegistryException e) {
            handleException("Error occurred while accessing the registry to save api sequence ", e);
        } catch (org.wso2.carbon.registry.api.RegistryException e) {
            handleException("Error occurred while saving api sequence ", e);
        }
    }

    public static String getRestToSoapConvertedSequence(String name, String version, String provider,
            String seqType) throws APIManagementException {
        JSONObject resultJson = new JSONObject();

        provider = (provider != null ? provider.trim() : null);
        name = (name != null ? name.trim() : null);
        version = (version != null ? version.trim() : null);

        boolean isTenantFlowStarted = false;

        try {
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
                String resourcePath = APIConstants.API_LOCATION + RegistryConstants.PATH_SEPARATOR +
                        provider + RegistryConstants.PATH_SEPARATOR + name + RegistryConstants.PATH_SEPARATOR + version
                        + RegistryConstants.PATH_SEPARATOR + SOAP_TO_REST_RESOURCE + RegistryConstants.PATH_SEPARATOR
                        + seqType;

                Collection collection = registry.get(resourcePath, 0, Integer.MAX_VALUE);
                String[] resources = collection.getChildren();

                for (String path : resources) {
                    Resource resource = registry.get(path);
                    String content = new String((byte[]) resource.getContent(), Charset.defaultCharset());
                    String resourceName = ((ResourceImpl) resource).getName();
                    resourceName = resourceName.replaceAll("\\.xml", "");
                    String httpMethod = resource.getProperty(SOAPToRESTConstants.METHOD);
                    Map<String, String> resourceMap = new HashMap<>();
                    resourceMap.put(SOAPToRESTConstants.METHOD, httpMethod);
                    resourceMap.put(SOAPToRESTConstants.CONTENT, content);
                    resultJson.put(resourceName, resourceMap);
                }

            } catch (RegistryException e) {
                handleException("Error when create registry instance ", e);
            } catch (UserStoreException e) {
                handleException("Error while reading tenant information ", e);
            } catch (org.wso2.carbon.registry.api.RegistryException e) {
                handleException("Error while creating registry resource", e);
            }
        } finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
        return resultJson.toJSONString();
    }

    public static JSONObject generateSoapToRestParameterMapping(List<WSDLOperationParam> params,
            List<JSONObject> mappingList) {
        JSONObject soapToRestParamMapping = new JSONObject();
        int i = mappingList.size() - 1;

        for (WSDLOperationParam param : params) {
            JSONObject paramObj = new JSONObject();
            String parameter = param.getName();
            String dataType = param.getDataType();
            if (dataType != null && !dataType.equals(SOAPToRESTConstants.PARAM_TYPES.ARRAY)) {
                paramObj.put(SOAPToRESTConstants.PARAM_TYPE, SOAPToRESTConstants.PARAM_TYPES.QUERY);
            }
            JSONObject mappingObj = mappingList.get(i);
            WSDLComplexType complexType = param.getWsdlComplexType();
            Iterator paramKeyIterator = mappingObj.keySet().iterator();
            String paramKey = "";
            if (paramKeyIterator.hasNext()) {
                paramKey = paramKeyIterator.next().toString();
            }
            JSONObject complexTypeObj = new JSONObject();
            if (complexType != null) {
                String complexTypeName = complexType.getName();
                List<WSDLOperationParam> complexTypes = complexType.getParamList();
                List<JSONObject> complexTypeList = new ArrayList<JSONObject>();
                Iterator complexObjIterator = ((JSONObject) mappingObj.get(paramKey)).keySet().iterator();
                for (WSDLOperationParam operation : complexTypes) {
                    JSONObject innerParam = new JSONObject();
                    if (operation.isArray()) {
                        String jsonPath = parameter;
                        Map paramMap = new LinkedHashMap();
                        paramMap.put(SOAPToRESTConstants.PARAM_TYPE, SOAPToRESTConstants.PARAM_TYPES.ARRAY);
                        paramMap.put(operation.getName(), jsonPath);
                        paramObj.putAll(paramMap);
                    } else if (complexObjIterator.hasNext()) {
                        String complexParam = complexObjIterator.next().toString();
                        String jsonPath;
                        if(complexTypeName != null) {
                            jsonPath = parameter + "." + complexTypeName + "." + operation.getName();
                        } else {
                            jsonPath = parameter + "." + operation.getName();
                        }
                        innerParam.put(complexParam, jsonPath);
                        complexTypeList.add(innerParam);
                    }
                }
                complexTypeObj.put(SOAPToRESTConstants.PARAM_TYPE, SOAPToRESTConstants.PARAM_TYPES.OBJECT);
                complexTypeObj.put(SOAPToRESTConstants.PARAM_TYPE, SOAPToRESTConstants.PARAM_TYPES.OBJECT);
                complexTypeObj.put(complexTypeName, complexTypeList);
            }
            if (SOAPToRESTConstants.PARAM_TYPES.OBJECT.equals(complexTypeObj.get(SOAPToRESTConstants.PARAM_TYPE)) && dataType == null) {
                soapToRestParamMapping.put(parameter, complexTypeObj);
            } else {
                soapToRestParamMapping.put(parameter, paramObj);
            }
            i--;
        }
        return soapToRestParamMapping;
    }

    public static List<JSONObject> getResourceParametersFromSwagger(JSONObject swaggerObj, JSONObject resource,
            String method) {
        Map content = (HashMap) resource.get(method);
        JSONArray parameters = (JSONArray) content.get(SOAPToRESTConstants.SWAGGER.PARAMETERS);
        List<JSONObject> mappingList = new ArrayList<>();
        for (Object param : parameters) {
            String inputType = String.valueOf(((JSONObject) param).get(SOAPToRESTConstants.SWAGGER.IN));
            if (inputType.equals(SOAPToRESTConstants.SWAGGER.BODY)) {
                JSONObject schema = (JSONObject) ((JSONObject) param).get(SOAPToRESTConstants.SWAGGER.SCHEMA);
                String definitionPath = String.valueOf(schema.get(SOAPToRESTConstants.SWAGGER.REF));
                String definition = definitionPath.replaceAll(SOAPToRESTConstants.SWAGGER.DEFINITIONS_ROOT, "");
                JSONObject definitions = (JSONObject) ((JSONObject) swaggerObj.get(SOAPToRESTConstants.SWAGGER.DEFINITIONS)).get(definition);
                JSONObject properties = (JSONObject) definitions.get(SOAPToRESTConstants.SWAGGER.PROPERTIES);

                for (Object property : properties.entrySet()) {
                    Map.Entry entry = (Map.Entry) property;
                    String paramName = String.valueOf(entry.getKey());
                    JSONObject value = (JSONObject) entry.getValue();
                    JSONArray propArray = new JSONArray();
                    if (value.get(SOAPToRESTConstants.SWAGGER.REF) != null) {
                        String propDefinitionRef = String.valueOf(value.get(SOAPToRESTConstants.SWAGGER.REF)).replaceAll(
                                SOAPToRESTConstants.SWAGGER.DEFINITIONS_ROOT, "");
                        getNestedDefinitionsFromSwagger((JSONObject) swaggerObj.get(SOAPToRESTConstants.SWAGGER.DEFINITIONS), propDefinitionRef, propDefinitionRef, propArray);
                        JSONObject refObj = new JSONObject();
                        refObj.put(paramName, propArray);
                        mappingList.add(refObj);
                    } else if (String.valueOf(value.get(SOAPToRESTConstants.SWAGGER.TYPE)).equals(SOAPToRESTConstants.PARAM_TYPES.ARRAY)) {
                        JSONObject arrObj = new JSONObject();
                        arrObj.put(((Map.Entry) property).getKey(), ((Map.Entry) property).getValue());
                        mappingList.add(arrObj);
                    }
                }
            } else {
                JSONObject queryObj = new JSONObject();
                queryObj.put(((JSONObject) param).get(SOAPToRESTConstants.SWAGGER.NAME), param);
                mappingList.add(queryObj);
            }
        }
        return mappingList;
    }

    private static void getNestedDefinitionsFromSwagger(JSONObject definitions, String definition, String jsonPath, JSONArray propArray) {
        JSONObject propDefinitions = (JSONObject) (definitions).get(definition);
        JSONObject props;
        if(SOAPToRESTConstants.PARAM_TYPES.ARRAY.equals(propDefinitions.get("type"))) {
            props = (JSONObject) propDefinitions.get("items");
            if(props.get("$ref") == null) {
                JSONObject arrayProp = new JSONObject();
                String key = jsonPath + "." + props.get("type");
                arrayProp.put(key, props.get("type"));
                return;
            }
        } else {
            props = (JSONObject) propDefinitions.get(SOAPToRESTConstants.SWAGGER.PROPERTIES);
        }
        for (Object property : props.entrySet()) {
            Map.Entry entry = (Map.Entry) property;
            String paramName = String.valueOf(entry.getKey());
            JSONObject value = (JSONObject) entry.getValue();
            if (value.get(SOAPToRESTConstants.SWAGGER.REF) != null) {
                String propDefinitionRef = String.valueOf(value.get(SOAPToRESTConstants.SWAGGER.REF)).replaceAll(
                        SOAPToRESTConstants.SWAGGER.DEFINITIONS_ROOT, "");
                jsonPath = definition + "." + propDefinitionRef;
                getNestedDefinitionsFromSwagger(definitions, propDefinitionRef, jsonPath, propArray);
            } else {
                JSONObject nestedProp = new JSONObject();
                String key;
                if(jsonPath.endsWith(definition)) {
                    key = jsonPath + "." + paramName;
                } else {
                    key = definition + "." + paramName;
                }
                nestedProp.put(key, value);
                propArray.add(nestedProp);
            }
        }
    }
}
