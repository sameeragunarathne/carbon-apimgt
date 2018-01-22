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

/**
 *  Constants used for wsdl processing in soap to rest mapping
 */
public class SOAPToRESTConstants {

    public static final String COMPLEX_TYPE_NODE_NAME = ":complexType";
    public static final String TYPE_ATTRIBUTE = "type";
    public static final String NAME_ATTRIBUTE = "name";
    public static final String MAX_OCCURS_ATTRIBUTE = "maxOccurs";
    public static final String UNBOUNDED = "unbounded";
    public static final String METHOD = "method";
    public static final String PARAM_TYPE = "type";
    public static final String CONTENT = "content";

    public final class SWAGGER {
        public static final String DEFINITIONS = "definitions";
        public static final String DEFINITIONS_ROOT = "#/definitions/";
        public static final String SCHEMA = "schema";
        public static final String REF = "$ref";
        public static final String BODY = "body";
        public static final String PROPERTIES =  "properties";
        public static final String PARAMETERS = "parameters";
        public static final String IN = "in";
        public static final String NAME = "name";
        public static final String TYPE = "type";
        public static final String PATHS = "paths";
    }

    public final class PARAM_TYPES {
        public static final String QUERY = "query";
        public static final String OBJECT = "object";
        public static final String ARRAY = "array";
    }

    public final class SEQUENCE_GEN {
        public static final String XPATH = "x-path";
        public static final String INDENT_PROPERTY = "{http://xml.apache.org/xslt}indent-amount";
        public static final String INDENT_VALUE = "2";

        public static final String SOAP_TO_REST_IN_RESOURCE = "/soap_to_rest/in";
        public static final String SOAP_TO_REST_OUT_RESOURCE = "/soap_to_rest/out";
        public static final String XML_FILE_EXTENSION = ".xml";
        public static final String RESOURCE_METHOD_SEPERATOR = "_";
        public static final String NEW_LINE_CHAR = "\n";
        public static final String NAMESPACE_SEPARATOR = ":";
        public static final String COMMA = ",";
        public static final String X_PATH_SEPARATOR = "/";

    }
}
