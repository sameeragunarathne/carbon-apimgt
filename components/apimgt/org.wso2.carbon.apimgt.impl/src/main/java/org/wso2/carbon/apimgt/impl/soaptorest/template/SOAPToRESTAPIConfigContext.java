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
import org.wso2.carbon.apimgt.impl.template.ConfigContext;
import org.wso2.carbon.apimgt.impl.template.ConfigContextDecorator;

/**
 * velocity config context to generate api sequence
 */
public class SOAPToRESTAPIConfigContext extends ConfigContextDecorator {

    private JSONObject sequences;

    public SOAPToRESTAPIConfigContext(ConfigContext context, JSONObject sequences) {
        super(context);
        this.sequences = sequences;
    }

    public VelocityContext getContext() {
        VelocityContext context = super.getContext();

        context.put("isSoapToRestMode", true);
        context.put("sequences", sequences);

        return context;
    }
}
