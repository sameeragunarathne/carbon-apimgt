/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.apimgt.impl.soaptorest.template;

import org.apache.velocity.VelocityContext;
import org.json.simple.JSONObject;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.impl.template.APITemplateException;
import org.wso2.carbon.apimgt.impl.template.ConfigContext;

import java.util.Map;

/**
 * velocity config context for the soap to rest mapping
 */
class SOAPToRESTConfigContext extends ConfigContext {

    private String method;
    private String soapAction;
    private String namespace;
    private String resourcePath;
    private Map<String, String> mappingObj;

    SOAPToRESTConfigContext(Map<String, String> mapping, String method, String soapAction, String namespace) {
        this.mappingObj = mapping;
        this.method = method;
        this.soapAction = soapAction;
        this.namespace = namespace;
        init();
    }

    SOAPToRESTConfigContext() {

    }

    @Override
    public void validate() throws APITemplateException, APIManagementException {

    }

    @Override
    public VelocityContext getContext() {
        VelocityContext context = new VelocityContext();
        context.put("method", method);
        context.put("soapAction", soapAction);
        context.put("namespace", namespace);
        context.put("resourcePath", resourcePath);
        context.put("mapping", mappingObj);
        return context;
    }

    private void init() {
        resourcePath = soapAction.replaceAll(namespace, "");
        resourcePath = resourcePath.replaceAll("/","");
    }
}
