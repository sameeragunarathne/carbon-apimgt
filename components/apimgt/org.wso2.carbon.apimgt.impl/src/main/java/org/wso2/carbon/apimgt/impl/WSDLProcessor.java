package org.wso2.carbon.apimgt.impl;

public interface WSDLProcessor {

    boolean init(byte[] wsdlContent) throws APIMgtWSDLException;

    byte[] getWSDL();

    byte[] getUpdatedWSDL();

    public WSDLInfo getWsdlInfo() throws APIMgtWSDLException;
}
