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

import com.google.gson.Gson;
import org.apache.axis2.transport.http.HTTPConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.impl.soaptorest.WSDLProcessor;
import org.wso2.carbon.apimgt.impl.soaptorest.exceptions.APIMgtWSDLException;
import org.wso2.carbon.apimgt.impl.soaptorest.model.WSDLOperationParam;
import org.wso2.carbon.apimgt.impl.soaptorest.model.WSDLSOAPOperation;
import org.wso2.carbon.apimgt.impl.utils.APIMWSDLReader;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * util class to get list of nodes from DOM Node list
 */
public class SOAPOperationBindingUtils {

    /**
     * gets soap operations to rest resources mapping
     *
     * @param url WSDL URL
     * @return json string with the soap operation mapping
     * @throws APIManagementException if an error occurs when getting soap operations from the wsdl
     */
    public static String getSoapOperationMapping(String url) throws APIManagementException {
        APIMWSDLReader wsdlReader = new APIMWSDLReader(url);
        byte[] wsdlContent = wsdlReader.getWSDL();
        WSDLProcessor processor = wsdlReader.getWSDLProcessor(wsdlContent);
        Set<WSDLSOAPOperation> operations;
        try {
            operations = processor.getWsdlInfo().getSoapBindingOperations();
            populateSoapOperationParameters(operations);
            return new Gson().toJson(operations);
        } catch (APIMgtWSDLException e) {
            throw new APIManagementException("Error in soap to rest conversion for wsdl url: " + url, e);
        }
    }

    /**
     * gets parameters from the soap operation and populates them in {@link WSDLSOAPOperation}
     *
     * @param soapOperations soap binding operations
     */
    private static void populateSoapOperationParameters(Set<WSDLSOAPOperation> soapOperations) {
        String[] primitiveTypes = {"string", "byte", "short", "int", "long", "float","double","boolean"};
        List primitiveTypeList = Arrays.asList(primitiveTypes);
        for (WSDLSOAPOperation op : soapOperations) {
            String resourcePath;
            String operationName = op.getName();
            op.setSoapBindingOpName(operationName);
            if(operationName.toLowerCase().startsWith("get")) {
                resourcePath = operationName.substring(3, operationName.length());
                op.setHttpVerb(HTTPConstants.HTTP_METHOD_GET);
            } else {
                resourcePath = operationName;
                op.setHttpVerb(HTTPConstants.HTTP_METHOD_POST);
            }
            resourcePath = resourcePath.substring(0,1).toLowerCase() + resourcePath.substring(1,resourcePath.length());
            op.setName(resourcePath);

            List<WSDLOperationParam> params = op.getParameters();
            for (WSDLOperationParam param : params) {
                if(param.getDataType() != null) {
                    String dataTypeWithNS = param.getDataType();
                    String dataType = dataTypeWithNS.substring(dataTypeWithNS.indexOf(":") + 1);
                    param.setDataType(dataType);
                    if(!primitiveTypeList.contains(dataType)) {
                        param.setComplexType(true);
                    }
                }
            }
        }
    }

    public static List<Node> list(final NodeList list) {
        return new AbstractList<Node>() {
            public int size() {
                return list.getLength();
            }

            public Node get(int index) {
                Node item = list.item(index);
                if (item == null)
                    throw new IndexOutOfBoundsException();
                return item;
            }
        };
    }

    public static List<Node> getElementsByTagName(Element e, String tag) {
        return list(e.getElementsByTagName(tag));
    }

}
