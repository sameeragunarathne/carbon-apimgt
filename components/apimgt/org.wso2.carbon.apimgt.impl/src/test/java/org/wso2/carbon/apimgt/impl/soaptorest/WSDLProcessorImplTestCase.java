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

import org.apache.axiom.om.OMElement;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationService;
import org.wso2.carbon.apimgt.impl.dto.Environment;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.soaptorest.model.WSDLSOAPOperation;
import org.wso2.carbon.apimgt.impl.utils.APIMWSDLReader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceReferenceHolder.class})
public class WSDLProcessorImplTestCase {
    private static ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
    private static APIManagerConfigurationService apimConfigService = Mockito
            .mock(APIManagerConfigurationService.class);
    private static APIManagerConfiguration apimConfig = Mockito.mock(APIManagerConfiguration.class);

    public static void doMockStatics() {
        Map<String, Environment> gatewayEnvironments = new HashMap<String, Environment>();
        Environment env1 = new Environment();
        env1.setType("hybrid");
        env1.setApiGatewayEndpoint("http://localhost:8280,https://localhost:8243");
        gatewayEnvironments.put("e1", env1);

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        PowerMockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        PowerMockito.when(ServiceReferenceHolder.getInstance()
                .getAPIManagerConfigurationService()).thenReturn(apimConfigService);
        PowerMockito.when(ServiceReferenceHolder.getInstance()
                .getAPIManagerConfigurationService()
                .getAPIManagerConfiguration()).thenReturn(apimConfig);

        PowerMockito.when(ServiceReferenceHolder.getInstance()
                .getAPIManagerConfigurationService()
                .getAPIManagerConfiguration()
                .getApiGatewayEnvironments()).thenReturn(gatewayEnvironments);
    }

    @Test
    public void testReadAndCleanWsdl() throws Exception {
        doMockStatics();

        APIMWSDLReader wsdlReader = new APIMWSDLReader(
                Thread.currentThread().getContextClassLoader()
                        .getResource("wsdls/stockQuote.wsdl").toExternalForm());
        WSDLProcessor wsdlProcessor = new WSDLSOAPOperationMapper();
        wsdlProcessor.getWsdlInfo().getSoapBindingOperations();
        wsdlReader.validateBaseURI();
        OMElement element = wsdlReader.readAndCleanWsdl(getAPIForTesting());
        Assert.assertFalse("Endpoints are not properly replaced",
                element.toString().contains("location=\"http://www.webservicex.net/stockquote.asmx\""));
        Assert.assertTrue("Endpoints does not include GW endpoint",
                element.toString().contains("https://localhost:8243/abc"));
    }

    @Test
    public void testReadSoapBindingOperations() throws Exception {
        doMockStatics();
        String url  = "http://ws.cdyne.com/phoneverify/phoneverify.asmx?wsdl";
        APIMWSDLReader wsdlReader = new APIMWSDLReader(url);
        byte[] wsdlContent = wsdlReader.getWSDL();
        WSDLProcessor processor = wsdlReader.getWSDLProcessor(wsdlContent);
        Set<WSDLSOAPOperation> operations = processor.getWsdlInfo().getSoapBindingOperations();
        Assert.assertTrue("WSDL operation processing failed", operations.iterator().hasNext());
        Assert.assertTrue("Incorrect wsdl namespace", operations.iterator().next().getTargetNamespace().equals("http://ws.cdyne.com/PhoneVerify/query"));
    }

    @Test
    public void testParseOperationInputParameters() throws Exception {
        doMockStatics();
        String url  = "http://ws.cdyne.com/phoneverify/phoneverify.asmx?wsdl";
        APIMWSDLReader wsdlReader = new APIMWSDLReader(url);
        byte[] wsdlContent = wsdlReader.getWSDL();
        WSDLProcessor processor = wsdlReader.getWSDLProcessor(wsdlContent);
        Set<WSDLSOAPOperation> operations = processor.getWsdlInfo().getSoapBindingOperations();
        Assert.assertTrue("WSDL operation processing failed", operations.iterator().hasNext());
        Assert.assertTrue("WSDL operation parameters are not set", operations.iterator().next().getParameters().iterator().hasNext());
    }

    @Test
    public void testParseOperationOutputParameters() throws Exception {
        doMockStatics();
        String url  = "http://ws.cdyne.com/phoneverify/phoneverify.asmx?wsdl";
        APIMWSDLReader wsdlReader = new APIMWSDLReader(url);
        byte[] wsdlContent = wsdlReader.getWSDL();
        WSDLProcessor processor = wsdlReader.getWSDLProcessor(wsdlContent);
        Set<WSDLSOAPOperation> operations = processor.getWsdlInfo().getSoapBindingOperations();
        Assert.assertTrue("WSDL operation processing failed", operations.iterator().hasNext());
        Assert.assertTrue("WSDL operation output parameters are not set", operations.iterator().next().getOutputParams().iterator().hasNext());
    }

    public static API getAPIForTesting() {
        API api = new API(new APIIdentifier("admin", "api1", "1.0.0"));
        api.setTransports("https");
        api.setContext("/weather");
        return api;
    }
}


