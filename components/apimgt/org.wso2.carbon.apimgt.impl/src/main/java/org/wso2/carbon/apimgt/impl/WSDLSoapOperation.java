package org.wso2.carbon.apimgt.impl;

import java.util.List;

public class WSDLSoapOperation {
    private String name;
    private String soapAction;
    private String style;
    private String httpVerb;
    private List<WSDLOperationParam> parameters;

    public WSDLSoapOperation() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSoapAction() {
        return soapAction;
    }

    public void setSoapAction(String soapAction) {
        this.soapAction = soapAction;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getHttpVerb() {
        return httpVerb;
    }

    public void setHttpVerb(String httpVerb) {
        this.httpVerb = httpVerb;
    }

    public List<WSDLOperationParam> getParameters() {
        return parameters;
    }

    public void setParameters(List<WSDLOperationParam> parameters) {
        this.parameters = parameters;
    }
}
