package org.wso2.carbon.apimgt.impl;

import com.ibm.wsdl.extensions.http.HTTPAddressImpl;
import com.ibm.wsdl.extensions.soap.SOAPAddressImpl;
import com.ibm.wsdl.extensions.soap12.SOAP12AddressImpl;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.wsdl.Binding;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.http.HTTPBinding;
import javax.wsdl.extensions.http.HTTPOperation;
import javax.wsdl.extensions.http.HTTPUrlReplacement;
import javax.wsdl.extensions.mime.MIMEContent;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.extensions.soap12.SOAP12Binding;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WSDLProcessorImpl implements WSDLProcessor {
    private static final Logger log = LoggerFactory.getLogger(WSDLProcessorImpl.class);
    private static final String JAVAX_WSDL_VERBOSE_MODE = "javax.wsdl.verbose";
    private static final String JAVAX_WSDL_IMPORT_DOCUMENTS = "javax.wsdl.importDocuments";
    private static final String TEXT_XML_MEDIA_TYPE = "text/xml";

    private static volatile WSDLFactory wsdlFactoryInstance;
    private boolean canProcess = false;

    //Fields required for processing a single wsdl
    protected Definition wsdlDefinition;

    //Fields required for processing WSDL archive
    protected Map<String, Definition> pathToDefinitionMap;
    protected String wsdlArchiveExtractedPath;

    private static WSDLFactory getWsdlFactoryInstance() throws APIMgtWSDLException {
        if (wsdlFactoryInstance == null) {
            try {
                synchronized (WSDLProcessorImpl.class) {
                    if (wsdlFactoryInstance == null) {
                        wsdlFactoryInstance = WSDLFactory.newInstance();
                    }
                }
            } catch (WSDLException e) {
                throw new APIMgtWSDLException("Error while instantiating WSDL 1.1 factory", e);
            }
        }
        return wsdlFactoryInstance;
    }

    @Override
    public boolean init(byte[] wsdlContent) throws APIMgtWSDLException {
        WSDLReader wsdlReader = getWsdlFactoryInstance().newWSDLReader();
        try {
            wsdlDefinition = wsdlReader.readWSDL(null, new InputSource(new ByteArrayInputStream(wsdlContent)));
            canProcess = true;
            if (log.isDebugEnabled()) {
                log.debug("Successfully initialized an instance of " + this.getClass().getSimpleName()
                        + " with a single WSDL.");
            }
        } catch (WSDLException e) {
            //This implementation class cannot process the WSDL.
            log.debug("Cannot process the WSDL by " + this.getClass().getName(), e);
            canProcess = false;
        }
        return canProcess;
    }

    @Override
    public byte[] getWSDL() {
        return new byte[0];
    }

    @Override
    public byte[] getUpdatedWSDL() {
        return new byte[0];
    }

    @Override
    public WSDLInfo getWsdlInfo() throws APIMgtWSDLException {
        WSDLInfo wsdlInfo = new WSDLInfo();
        Map<String, String> endpointsMap = null;
        if (wsdlDefinition != null) {
            endpointsMap = getEndpoints(wsdlDefinition);
            Set<WSDLOperation> operations = getHttpBindingOperations(wsdlDefinition);
            Set<WSDLSoapOperation> soapOperations = getSoapBindingOperations(wsdlDefinition);
            wsdlInfo.setEndpoints(endpointsMap);
            wsdlInfo.setVersion(APIConstants.WSDLConstants.WSDL_VERSION_11);

            if(!operations.isEmpty()) {
                wsdlInfo.setHasHttpBindingOperations(true);
                wsdlInfo.setHttpBindingOperations(operations);
            } else {
                wsdlInfo.setHasHttpBindingOperations(false);
            }

            if(!soapOperations.isEmpty()) {
                wsdlInfo.setHasSoapBindingOperations(true);
                wsdlInfo.setSoapBindingOperations(soapOperations);
            } else {
                wsdlInfo.setHasSoapBindingOperations(false);
            }
            wsdlInfo.setHasSoapBindingOperations(hasSoapBindingOperations());
        }
        return wsdlInfo;
    }

    /**
     * Get endpoints defined in the provided WSDL definition.
     *
     * @param definition WSDL Definition
     * @return a Map of endpoint names and their URIs.
     * @throws APIMgtWSDLException if error occurs while reading endpoints
     */
    private Map<String, String> getEndpoints(Definition definition) throws APIMgtWSDLException {
        Map serviceMap = definition.getAllServices();
        Iterator serviceItr = serviceMap.entrySet().iterator();
        Map<String, String> serviceEndpointMap = new HashMap<>();
        while (serviceItr.hasNext()) {
            Map.Entry svcEntry = (Map.Entry) serviceItr.next();
            Service svc = (Service) svcEntry.getValue();
            Map portMap = svc.getPorts();
            for (Object o : portMap.entrySet()) {
                Map.Entry portEntry = (Map.Entry) o;
                Port port = (Port) portEntry.getValue();
                List extensibilityElementList = port.getExtensibilityElements();
                for (Object extensibilityElement : extensibilityElementList) {
                    String addressURI = getAddressUrl(extensibilityElement);
                    serviceEndpointMap.put(port.getName(), addressURI);
                }
            }
        }
        return serviceEndpointMap;
    }

    private String getAddressUrl(Object exElement) throws APIMgtWSDLException {
        if (exElement instanceof SOAP12AddressImpl) {
            return ((SOAP12AddressImpl) exElement).getLocationURI();
        } else if (exElement instanceof SOAPAddressImpl) {
            return ((SOAPAddressImpl) exElement).getLocationURI();
        } else if (exElement instanceof HTTPAddressImpl) {
            return ((HTTPAddressImpl) exElement).getLocationURI();
        } else {
            throw new APIMgtWSDLException("Unsupported WSDL Extensibility element");
        }
    }

    private Set<WSDLSoapOperation> getSoapBindingOperations(Definition definition) {
        Set<WSDLSoapOperation> allOperations = new HashSet<>();
        for (Object bindingObj : definition.getAllBindings().values()) {
            if (bindingObj instanceof Binding) {
                Binding binding = (Binding) bindingObj;
                Set<WSDLSoapOperation> operations = getSOAPBindingOperations(binding);
                allOperations.addAll(operations);
            }
        }
        return allOperations;
    }

    /**
     * Retrieves all the operations defined in the provided WSDL definition.
     *
     * @param definition WSDL Definition
     * @return a set of {@link WSDLOperation} defined in the provided WSDL definition
     */
    private Set<WSDLOperation> getHttpBindingOperations(Definition definition) {
        Set<WSDLOperation> allOperations = new HashSet<>();
        for (Object bindingObj : definition.getAllBindings().values()) {
            if (bindingObj instanceof Binding) {
                Binding binding = (Binding) bindingObj;
                Set<WSDLOperation> operations = getHttpBindingOperations(binding);
                allOperations.addAll(operations);
            }
        }
        return allOperations;
    }

    /**
     * Retrieves all the operations defined in the provided Binding.
     *
     * @param binding WSDL binding
     * @return a set of {@link WSDLOperation} defined in the provided Binding
     */
    private Set<WSDLOperation> getHttpBindingOperations(Binding binding) {
        Set<WSDLOperation> allBindingOperations = new HashSet<>();
        if (binding.getExtensibilityElements() != null && binding.getExtensibilityElements().size() > 0) {
            if (binding.getExtensibilityElements().get(0) instanceof HTTPBinding) {
                HTTPBinding httpBinding = (HTTPBinding) binding.getExtensibilityElements().get(0);
                String verb = httpBinding.getVerb();
                for (Object opObj : binding.getBindingOperations()) {
                    if (opObj instanceof BindingOperation) {
                        BindingOperation bindingOperation = (BindingOperation) opObj;
                        WSDLOperation wsdlOperation = getOperation(bindingOperation, verb);
                        if (wsdlOperation != null) {
                            allBindingOperations.add(wsdlOperation);
                        }
                    }
                }
            }
        }
        return allBindingOperations;
    }

    /**
     * Retrieves all the operations defined in the provided Binding.
     *
     * @param binding WSDL binding
     * @return a set of {@link WSDLOperation} defined in the provided Binding
     */
    private Set<WSDLSoapOperation> getSOAPBindingOperations(Binding binding) {
        Set<WSDLSoapOperation> allBindingOperations = new HashSet<>();
        if (binding.getExtensibilityElements() != null && binding.getExtensibilityElements().size() > 0) {
            if (binding.getExtensibilityElements().get(0) instanceof SOAPBinding) {
                SOAPBinding soapBinding = (SOAPBinding) binding.getExtensibilityElements().get(0);
//                String transportURI = soapBinding.getTransportURI();
                for(Object opObj : binding.getBindingOperations()) {
                    BindingOperation bindingOperation = (BindingOperation) opObj;
                    WSDLSoapOperation wsdlSoapOperation = getSOAPOperation(bindingOperation);
                    if(wsdlSoapOperation != null) {
                        allBindingOperations.add(wsdlSoapOperation);
                    }
                }
            }
        }
        return allBindingOperations;
    }

    /**
     * Retrieves WSDL operation given the binding operation and http verb
     *
     * @param bindingOperation {@link BindingOperation} object
     * @param verb             HTTP verb
     * @return WSDL operation for the given binding operation and http verb
     */
    private WSDLOperation getOperation(BindingOperation bindingOperation, String verb) {
        WSDLOperation wsdlOperation = null;
        for (Object boExtElement : bindingOperation.getExtensibilityElements()) {
            if (boExtElement instanceof HTTPOperation) {
                HTTPOperation httpOperation = (HTTPOperation) boExtElement;
                if (!StringUtils.isBlank(httpOperation.getLocationURI())) {
                    wsdlOperation = new WSDLOperation();
                    wsdlOperation.setVerb(verb);
                    wsdlOperation.setURI(APIMWSDLUtils.replaceParentheses(httpOperation.getLocationURI()));
                    if (log.isDebugEnabled()) {
                        log.debug("Found HTTP Binding operation; name: " + bindingOperation.getName() + " ["
                                + wsdlOperation.getVerb() + " "
                                + wsdlOperation.getURI() + "]");
                    }
                    if (APIMWSDLUtils.canContainBody(verb)) {
                        String boContentType = getContentType(bindingOperation.getBindingInput());
                        wsdlOperation.setContentType(boContentType != null ? boContentType : TEXT_XML_MEDIA_TYPE);
                    }
                    List<WSDLOperationParam> paramList = getParameters(bindingOperation, verb,
                            wsdlOperation.getContentType());
                    wsdlOperation.setParameters(paramList);
                }
            }
        }
        return wsdlOperation;
    }

    private WSDLSoapOperation getSOAPOperation(BindingOperation bindingOperation) {
        WSDLSoapOperation wsdlOperation = null;
        for (Object boExtElement : bindingOperation.getExtensibilityElements()) {
            if(boExtElement instanceof SOAPOperation) {
                SOAPOperation soapOperation = (SOAPOperation) boExtElement;
                if(!StringUtils.isBlank(soapOperation.getSoapActionURI())) {
                    wsdlOperation = new WSDLSoapOperation();
                    wsdlOperation.setName(bindingOperation.getName());
                    wsdlOperation.setSoapAction(soapOperation.getSoapActionURI());
                    wsdlOperation.setStyle(soapOperation.getStyle());

                    List<WSDLOperationParam> params = getSoapParameters(bindingOperation);
                    wsdlOperation.setParameters(params);
                }
            }
        }
        return wsdlOperation;
    }

    /**
     * Returns the content-type of a provided {@link BindingInput} if it is available
     *
     * @param bindingInput Binding Input object
     * @return The content-type of the {@link BindingInput}
     */
    private String getContentType(BindingInput bindingInput) {
        List extensibilityElements = bindingInput.getExtensibilityElements();
        if (extensibilityElements != null) {
            for (Object ex : extensibilityElements) {
                if (ex instanceof MIMEContent) {
                    MIMEContent mimeContentElement = (MIMEContent) ex;
                    return mimeContentElement.getType();
                }
            }
        }
        return null;
    }

    /**
     * Returns parameters, given http binding operation, verb and content type
     *
     * @param bindingOperation {@link BindingOperation} object
     * @param verb             HTTP verb
     * @param contentType      Content type
     * @return parameters, given http binding operation, verb and content type
     */
    private List<WSDLOperationParam> getParameters(BindingOperation bindingOperation, String verb, String contentType) {
        List<WSDLOperationParam> params = new ArrayList<>();
        Operation operation = bindingOperation.getOperation();

        //Returns a single parameter called payload with body type if request can contain a body (PUT/POST) and
        // content type is not application/x-www-form-urlencoded OR multipart/form-data,
        // or content type is not provided
        if (APIMWSDLUtils.canContainBody(verb) && !APIMWSDLUtils.hasFormDataParams(contentType)) {
            WSDLOperationParam param = new WSDLOperationParam();
            param.setName("Payload");
            param.setParamType(WSDLOperationParam.ParamTypeEnum.BODY);
            params.add(param);
            if (log.isDebugEnabled()) {
                log.debug(
                        "Adding default Param for operation:" + operation.getName() + ", contentType: " + contentType);
            }
            return params;
        }

        if (operation != null) {
            Input input = operation.getInput();
            if (input != null) {
                Message message = input.getMessage();
                if (message != null) {
                    Map map = message.getParts();
                    Iterator iterator = map.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry entry = (Map.Entry)iterator.next();
                        String name = entry.getKey().toString();
                        Part part = (Part) entry.getValue();
                        WSDLOperationParam param = new WSDLOperationParam();
                        param.setName(name.toString());
                        if (log.isDebugEnabled()) {
                            log.debug("Identified param for operation: " + operation.getName() + " param: " + name);
                        }
                        if (APIMWSDLUtils.canContainBody(verb)) {
                            if (log.isDebugEnabled()) {
                                log.debug("Operation " + operation.getName() + " can contain a body.");
                            }
                            //In POST, PUT operations, parameters always in body according to HTTP Binding spec
                            if (APIMWSDLUtils.hasFormDataParams(contentType)) {
                                param.setParamType(WSDLOperationParam.ParamTypeEnum.FORM_DATA);
                                if (log.isDebugEnabled()) {
                                    log.debug("Param " + name + " type was set to formData.");
                                }
                            }
                            //no else block since if content type is not form-data related, there can be only one
                            // parameter which is payload body. This is handled in the first if block which is
                            // if (canContainBody(verb) && !hasFormDataParams(contentType)) { .. }
                        } else {
                            //In GET operations, parameters always query or path as per HTTP Binding spec
                            if (isUrlReplacement(bindingOperation)) {
                                param.setParamType(WSDLOperationParam.ParamTypeEnum.PATH);
                                if (log.isDebugEnabled()) {
                                    log.debug("Param " + name + " type was set to Path.");
                                }
                            } else {
                                param.setParamType(WSDLOperationParam.ParamTypeEnum.QUERY);
                                if (log.isDebugEnabled()) {
                                    log.debug("Param " + name + " type was set to Query.");
                                }
                            }
                        }
                        param.setDataType(part.getTypeName().getLocalPart());
                        if (log.isDebugEnabled()) {
                            log.debug("Param " + name + " data type was set to " + param.getDataType());
                        }
                        params.add(param);
                    }
                }
            }
        }
        return params;
    }

    private List<WSDLOperationParam> getSoapParameters(BindingOperation bindingOperation) {
        List<WSDLOperationParam> params = new ArrayList<>();
        Operation operation = bindingOperation.getOperation();

        if (operation != null) {
            Input input = operation.getInput();
            if (input != null) {
                Message message = input.getMessage();
                if (message != null) {
                    Map map = message.getParts();
                    Iterator iterator = map.entrySet().iterator();

                    while (iterator.hasNext()) {
                        Map.Entry entry = (Map.Entry)iterator.next();
                        String name = entry.getKey().toString();
                        Part part = (Part) entry.getValue();
                        String partElement = part.getElementName().getLocalPart();
                        Types types = wsdlDefinition.getTypes();
                        List typeList = types.getExtensibilityElements();
                        Schema schema = null;

                        if(typeList != null) {
                            for(Object ext : typeList) {
                                if(ext instanceof Schema) {
                                    schema = (Schema)ext;
                                    break;
                                }
                            }
                            if (schema != null) {
                                Element schemaElement = schema.getElement();
                                schemaElement.getNodeName();
                                schemaElement.getSchemaTypeInfo();
                                NodeList elemList = schemaElement.getElementsByTagName("s:complexType");
                                elemList.getLength();

                                if(elemList != null) {
                                    for (int i = 0; i < elemList.getLength(); i++) {
                                        Node parentElement = elemList.item(i).getParentNode();
                                        if(parentElement.getAttributes().getNamedItem("name").getNodeValue().equals(partElement)) {
                                            NodeList childNodes = elemList.item(i).getChildNodes().item(1).getChildNodes();
                                            for (int j = 0; j < childNodes.getLength(); j++) {
                                                if(childNodes.item(j).getLocalName() != null && childNodes.item(j).getLocalName().equals("element")) {
                                                    WSDLOperationParam param = new WSDLOperationParam();
                                                    NamedNodeMap attributes = childNodes.item(j).getAttributes();
                                                    param.setName(attributes.getNamedItem("name").getNodeValue());
                                                    param.setDataType(attributes.getNamedItem("type").getNodeValue());
                                                    params.add(param);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return params;
    }

    /**
     * Returns whether the provided binding operation is of URL Replacement type
     *
     * @param bindingOperation Binding operation
     * @return whether the provided binding operation is of URL Replacement type
     */
    private boolean isUrlReplacement(BindingOperation bindingOperation) {
        List extensibilityElements = bindingOperation.getExtensibilityElements();
        if (extensibilityElements != null) {
            for (Object e : extensibilityElements) {
                if (e instanceof HTTPUrlReplacement) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns if any of the WSDLs (initialized) contains SOAP binding operations
     *
     * @return whether the WSDLs (initialized) contains SOAP binding operations
     */
    private boolean hasSoapBindingOperations() {
        if (wsdlDefinition != null) {
            return hasSoapBindingOperations(wsdlDefinition);
        } else {
            for (Definition definition : pathToDefinitionMap.values()) {
                if (hasSoapBindingOperations(definition)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Returns if the provided WSDL definition contains SOAP binding operations
     *
     * @param definition WSDL definition
     * @return whether the provided WSDL definition contains SOAP binding operations
     */
    private boolean hasSoapBindingOperations(Definition definition) {
        for (Object bindingObj : definition.getAllBindings().values()) {
            if (bindingObj instanceof Binding) {
                Binding binding = (Binding) bindingObj;
                for (Object ex : binding.getExtensibilityElements()) {
                    if (ex instanceof SOAPBinding || ex instanceof SOAP12Binding) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
