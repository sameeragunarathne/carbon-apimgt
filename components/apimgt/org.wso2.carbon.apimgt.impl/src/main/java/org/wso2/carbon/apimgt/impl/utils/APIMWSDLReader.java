/*
*  Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.apimgt.impl.utils;

import com.ibm.wsdl.extensions.http.HTTPAddressImpl;
import com.ibm.wsdl.extensions.soap.SOAPAddressImpl;
import com.ibm.wsdl.extensions.soap12.SOAP12AddressImpl;
import org.apache.axiom.om.OMElement;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.woden.WSDLSource;
import org.apache.woden.wsdl20.Endpoint;
import org.apache.woden.wsdl20.xml.EndpointElement;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.util.SecurityManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.soaptorest.exceptions.APIMgtWSDLException;
import org.wso2.carbon.apimgt.impl.soaptorest.WSDLProcessor;
import org.wso2.carbon.apimgt.impl.soaptorest.WSDLSOAPOperationMapper;
import org.xml.sax.SAXException;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;




/**
 * This class is used to read the WSDL file using WSDL4J library.
 *
 */

public class APIMWSDLReader {

	private static WSDLFactory wsdlFactoryInstance;

	private String baseURI; //WSDL Original URL

	private static final String JAVAX_WSDL_VERBOSE_MODE = "javax.wsdl.verbose";

    private static final int ENTITY_EXPANSION_LIMIT = 0;

	private static final Log log = LogFactory.getLog(APIMWSDLReader.class);

	private static final String WSDL20_NAMESPACE = "http://www.w3.org/ns/wsdl";
	private static final String WSDL11_NAMESPACE = "http://schemas.xmlsoap.org/wsdl/";
	
	public APIMWSDLReader(String baseURI) {
		this.baseURI = baseURI;
	}

	private static WSDLFactory getWsdlFactoryInstance() throws WSDLException {
		if (null == wsdlFactoryInstance) {
			wsdlFactoryInstance = WSDLFactory.newInstance();
		}
		return wsdlFactoryInstance;
	}

	/**
	 * Read the wsdl and clean the actual service endpoint instead of that set
	 * the gateway endpoint.
	 *
	 * @return {@link OMElement} - the OMElemnt of the new WSDL content
	 * @throws APIManagementException
	 *
	 */

	public OMElement readAndCleanWsdl(API api) throws APIManagementException {

		try {
			Definition wsdlDefinition = readWSDLFile();
			setServiceDefinition(wsdlDefinition, api);
			WSDLWriter writer = getWsdlFactoryInstance().newWSDLWriter();
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			writer.writeWSDL(wsdlDefinition, byteArrayOutputStream);
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( byteArrayOutputStream.toByteArray());

			return APIUtil.buildOMElement(byteArrayInputStream);
		} catch (Exception e) {
			String msg = " Error occurs when change the addres URL of the WSDL";
			log.error(msg);
			throw new APIManagementException(msg, e);
		}

	}

    public OMElement readAndCleanWsdl2(API api) throws APIManagementException {

        try {
            org.apache.woden.wsdl20.Description wsdlDefinition = readWSDL2File();
            setServiceDefinitionForWSDL2(wsdlDefinition, api);
            org.apache.woden.WSDLWriter writer = org.apache.woden.WSDLFactory.newInstance().newWSDLWriter();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            writer.writeWSDL(wsdlDefinition.toElement(), byteArrayOutputStream);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( byteArrayOutputStream.toByteArray());
            return APIUtil.buildOMElement(byteArrayInputStream);
        } catch (Exception e) {
            String msg = " Error occurs when change the addres URL of the WSDL";
            log.error(msg);
            throw new APIManagementException(msg, e);
        }

    }

    /**
     * Retrieves the WSDL located in the provided URI ({@code api}
     * @param api api object
     * @return Content bytes of the WSDL file
     * @throws APIManagementException If an error occurred while retrieving the WSDL file
     */
    public byte[] getWSDL(API api) throws APIManagementException {
        try {
            Definition wsdlDefinition = readWSDLFile();
            setServiceDefinition(wsdlDefinition, api);
            WSDLWriter writer = getWsdlFactoryInstance().newWSDLWriter();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            writer.writeWSDL(wsdlDefinition, byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            String msg = " Error occurs when change the addres URL of the WSDL";
            log.error(msg);
            throw new APIManagementException(msg, e);
        }
    }

    public byte[] getWSDL() throws APIManagementException {
        try {
            Definition wsdlDefinition = readWSDLFile();
            WSDLWriter writer = getWsdlFactoryInstance().newWSDLWriter();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            writer.writeWSDL(wsdlDefinition, byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            String msg = " Error occurs when change the addres URL of the WSDL";
            log.error(msg);
            throw new APIManagementException(msg, e);
        }
    }

    /**
     * Retrieves the WSDL located in the provided URI ({@code api}
     * @param api api object
     * @return Content bytes of the WSDL file
     * @throws APIManagementException If an error occurred while retrieving the WSDL file
     */
    public byte[] getWSDL2(API api) throws APIManagementException {
        try {
            org.apache.woden.wsdl20.Description wsdlDefinition = readWSDL2File();
            setServiceDefinitionForWSDL2(wsdlDefinition, api);
            org.apache.woden.WSDLWriter writer = org.apache.woden.WSDLFactory.newInstance().newWSDLWriter();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            writer.writeWSDL(wsdlDefinition.toElement(), byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            String msg = " Error occurs when change the addres URL of the WSDL";
            log.error(msg);
            throw new APIManagementException(msg, e);
        }
    }

    public WSDLProcessor getWSDLProcessor(byte[] content) throws APIManagementException {
        WSDLProcessor processor = new WSDLSOAPOperationMapper();
        try {
            boolean canProcess = processor.init(content);
            if(canProcess) {
                return processor;
            }
        } catch (APIMgtWSDLException e) {
            throw new APIManagementException("Error while instantiating wsdl processor class", e);
        }

        //no processors found if this line reaches
        throw new APIManagementException("No WSDL processor found to process WSDL content");
    }

    /**
     * Validate the base URI of the WSDL reader
     *
     * @throws APIManagementException When error occurred while parsing the content from the URL
     */
    public void validateBaseURI() throws APIManagementException {
        if (baseURI.startsWith(APIConstants.WSDL_REGISTRY_LOCATION_PREFIX)) {
            baseURI = APIUtil.getServerURL() + baseURI;
        }

        boolean isWsdl20 = false;
        boolean isWsdl11 = false;

        BufferedReader in = null;
        try {
            String inputLine;
            StringBuilder urlContent = new StringBuilder();
            URL wsdl = new URL(baseURI);
            in = new BufferedReader(new InputStreamReader(wsdl.openStream(), Charset.defaultCharset()));
            while ((inputLine = in.readLine()) != null) {
                urlContent.append(inputLine);
                isWsdl20 = urlContent.indexOf(WSDL20_NAMESPACE) > 0;
                isWsdl11 = urlContent.indexOf(WSDL11_NAMESPACE) > 0;
            }
        } catch (IOException e) {
            throw new APIManagementException("Error while reading WSDL from base URI " + baseURI, e);
        } finally {
            IOUtils.closeQuietly(in);
        }

        try {
            if (isWsdl11) {
                readAndValidateWSDL11();
            } else if (isWsdl20) {
                readAndValidateWSDL20();
            } else {
                throw new APIManagementException("URL is not in format of wsdl1.1 or wsdl2.0");
            }
        } catch (WSDLException e) {
            throw new APIManagementException("Error while parsing WSDL content", e);
        } catch (org.apache.woden.WSDLException e) {
            throw new APIManagementException("Error while parsing WSDL content", e);
        }
    }

    /**
     * Given a URL, this method checks if the underlying document is a WSDL2
     *
     * @return true if the underlying document is a WSDL2
     * @throws APIManagementException if error occurred while checking whether baseURI is WSDL2.0
     */
    public boolean isWSDL2BaseURI() throws APIManagementException {
        URL wsdl;
        boolean isWsdl2 = false;
        BufferedReader in = null;
        try {
            wsdl = new URL(baseURI);
            in = new BufferedReader(new InputStreamReader(wsdl.openStream(), Charset.defaultCharset()));

            String inputLine;
            StringBuilder urlContent = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                urlContent.append(inputLine);
                isWsdl2 = urlContent.indexOf(WSDL20_NAMESPACE) > 0;
            }
        } catch (MalformedURLException e) {
            throw new APIManagementException("Malformed URL encountered", e);
        } catch (IOException e) {
            throw new APIManagementException("Error Reading Input from Stream from " + baseURI, e);
        } finally {
            IOUtils.closeQuietly(in);
        }

        try {
            if (isWsdl2) {
                readAndValidateWSDL20();
            }
        } catch (org.apache.woden.WSDLException e) {
            throw new APIManagementException("Error while reading WSDL Document from " + baseURI, e);
        }
        return isWsdl2;
    }

    /**
     * Update WSDL 1.0 service definitions saved in registry
     *
     * @param wsdl 	byte array of registry content
	 * @param api 	API object
	 * @return 		the OMElemnt of the new WSDL content
	 * @throws APIManagementException
     */
	public OMElement updateWSDL(byte[] wsdl, API api) throws APIManagementException {

		try {
			// Generate wsdl document from registry data
			WSDLReader wsdlReader = getWsdlFactoryInstance().newWSDLReader();
			// switch off the verbose mode
			wsdlReader.setFeature(JAVAX_WSDL_VERBOSE_MODE, false);
			wsdlReader.setFeature("javax.wsdl.importDocuments", false);
			Definition wsdlDefinition = wsdlReader.readWSDL(null, getSecuredParsedDocumentFromContent(wsdl));

			// Update transports
			setServiceDefinition(wsdlDefinition, api);

			WSDLWriter writer = getWsdlFactoryInstance().newWSDLWriter();
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			writer.writeWSDL(wsdlDefinition, byteArrayOutputStream);
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( byteArrayOutputStream.toByteArray());
			return APIUtil.buildOMElement(byteArrayInputStream);

		} catch (Exception e) {
			String msg = " Error occurs when updating WSDL ";
			log.error(msg);
			throw new APIManagementException(msg, e);
		}
	}

	/**
	 * Update WSDL 2.0 service definitions saved in registry
	 *
	 * @param wsdl 	byte array of wsdl definition saved in registry
	 * @param api 	API object
	 * @return 		the OMElemnt of the new WSDL content
	 * @throws APIManagementException
	 */
	public OMElement updateWSDL2(byte[] wsdl, API api) throws APIManagementException {

		try {
			// Generate wsdl document from registry data
			DocumentBuilderFactory factory = getSecuredDocumentBuilder();
			DocumentBuilder builder = factory.newDocumentBuilder();
			org.apache.woden.WSDLFactory wsdlFactory = org.apache.woden.WSDLFactory.newInstance();
			org.apache.woden.WSDLReader reader = wsdlFactory.newWSDLReader();
			reader.setFeature(org.apache.woden.WSDLReader.FEATURE_VALIDATION, false);
			Document dom = builder.parse(new ByteArrayInputStream(wsdl));
			Element domElement = dom.getDocumentElement();
			WSDLSource wsdlSource = reader.createWSDLSource();
			wsdlSource.setSource(domElement);
			org.apache.woden.wsdl20.Description wsdlDefinition = reader.readWSDL(wsdlSource);

			// Update transports
			setServiceDefinitionForWSDL2(wsdlDefinition, api);

			org.apache.woden.WSDLWriter writer = org.apache.woden.WSDLFactory.newInstance().newWSDLWriter();
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			writer.writeWSDL(wsdlDefinition.toElement(), byteArrayOutputStream);
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
			return APIUtil.buildOMElement(byteArrayInputStream);

		} catch (Exception e) {
			String msg = " Error occurs when updating WSDL ";
			log.error(msg, e);
			throw new APIManagementException(msg, e);
		}
	}

    private static DocumentBuilderFactory getSecuredDocumentBuilder() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        try {
            dbf.setFeature(Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_GENERAL_ENTITIES_FEATURE, false);
            dbf.setFeature(Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
            dbf.setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.LOAD_EXTERNAL_DTD_FEATURE, false);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException e) {
            // Skip throwing the error as this exception doesn't break actual DocumentBuilderFactory creation
            log.error("Failed to load XML Processor Feature " + Constants.EXTERNAL_GENERAL_ENTITIES_FEATURE + " or "
                    + Constants.EXTERNAL_PARAMETER_ENTITIES_FEATURE + " or " + Constants.LOAD_EXTERNAL_DTD_FEATURE, e);
        }
        SecurityManager securityManager = new SecurityManager();
        securityManager.setEntityExpansionLimit(ENTITY_EXPANSION_LIMIT);
        dbf.setAttribute(Constants.XERCES_PROPERTY_PREFIX + Constants.SECURITY_MANAGER_PROPERTY, securityManager);
        return dbf;
    }

    private org.apache.woden.wsdl20.Description readWSDL2File() throws APIManagementException, WSDLException {
        WSDLReader reader = getWsdlFactoryInstance().newWSDLReader();
        reader.setFeature(JAVAX_WSDL_VERBOSE_MODE, false);
        reader.setFeature("javax.wsdl.importDocuments", false);
        try {
            org.apache.woden.WSDLFactory wFactory = org.apache.woden.WSDLFactory.newInstance();
            org.apache.woden.WSDLReader wReader = wFactory.newWSDLReader();
            wReader.setFeature(org.apache.woden.WSDLReader.FEATURE_VALIDATION, true);
            Document document = getSecuredParsedDocumentFromURL(baseURI);
            Element domElement = document.getDocumentElement();
            WSDLSource wsdlSource = wReader.createWSDLSource();
            wsdlSource.setSource(domElement);
            return wReader.readWSDL(wsdlSource);
        } catch (org.apache.woden.WSDLException e) {
            String error = "Error occurred reading wsdl document.";
            log.error(error, e);
        }
        if (log.isDebugEnabled()) {
            log.debug("Reading  the WSDL. Base uri is " + baseURI);
        }
        return null;
    }

    private void setServiceDefinitionForWSDL2(org.apache.woden.wsdl20.Description definition, API api)
            throws APIManagementException {
        org.apache.woden.wsdl20.Service[] serviceMap = definition.getServices();
        // URL addressURI;
        try {
            for (org.apache.woden.wsdl20.Service svc : serviceMap) {
                Endpoint[] portMap = svc.getEndpoints();
                for (Endpoint endpoint : portMap) {
                    EndpointElement element = endpoint.toElement();
                    // addressURI = endpoint.getAddress().toURL();
                    // if (addressURI == null) {
                    //    break;
                    // } else {
                    String endpointTransport = determineURLTransport(endpoint.getAddress().getScheme(),
                                                                     api.getTransports());
                    setAddressUrl(element, new URI(APIUtil.getGatewayendpoint(endpointTransport) +
                                                   api.getContext() + '/' + api.getId().getVersion()));
                    //}
                }
            }
        } catch (Exception e) {
            String errorMsg = "Error occurred while getting the wsdl address location";
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e);
        }
    }

    /**
	 * Create the WSDL definition <javax.wsdl.Definition> from the baseURI of
	 * the WSDL
	 *
	 * @return {@link Definition} - WSDL4j definition constructed form the wsdl
	 *         original baseuri
	 * @throws APIManagementException
	 * @throws WSDLException
	 */

	private Definition readWSDLFile() throws APIManagementException, WSDLException {
		WSDLReader reader = getWsdlFactoryInstance().newWSDLReader();
		// switch off the verbose mode
		reader.setFeature(JAVAX_WSDL_VERBOSE_MODE, false);
		reader.setFeature("javax.wsdl.importDocuments", false);

		if (log.isDebugEnabled()) {
			log.debug("Reading  the WSDL. Base uri is " + baseURI);
		}
		return reader.readWSDL(null, getSecuredParsedDocumentFromURL(baseURI));
    }

    /**
     * Returns an "XXE safe" built DOM XML object by reading the content from the provided URL.
     *
     * @param url URL to fetch the content
     * @return an "XXE safe" built DOM XML object by reading the content from the provided URL
     * @throws APIManagementException When error occurred while reading from URL
     */
    private Document getSecuredParsedDocumentFromURL(String url) throws APIManagementException {
        URL wsdl;
        String errorMsg = "Error while reading WSDL document";
        InputStream inputStream = null;
        try {
            wsdl = new URL(url);
            DocumentBuilderFactory factory = getSecuredDocumentBuilder();
            DocumentBuilder builder = factory.newDocumentBuilder();
            inputStream = wsdl.openStream();
            return builder.parse(inputStream);
        } catch (ParserConfigurationException e) {
            throw new APIManagementException(errorMsg, e);
        } catch (IOException e) {
            throw new APIManagementException(errorMsg, e);
        } catch (SAXException e) {
            throw new APIManagementException(errorMsg, e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Returns an "XXE safe" built DOM XML object by reading the content from the byte array.
     *
     * @param content xml content
     * @return an "XXE safe" built DOM XML object by reading the content from the byte array
     * @throws APIManagementException When error occurred while reading from the byte array
     */
    private Document getSecuredParsedDocumentFromContent(byte[] content) throws APIManagementException {
        String errorMsg = "Error while reading WSDL document";
        InputStream inputStream = null;
        try {
            DocumentBuilderFactory factory = getSecuredDocumentBuilder();
            DocumentBuilder builder = factory.newDocumentBuilder();
            inputStream = new ByteArrayInputStream(content);
            return builder.parse(inputStream);
        } catch (ParserConfigurationException e) {
            throw new APIManagementException(errorMsg, e);
        } catch (IOException e) {
            throw new APIManagementException(errorMsg, e);
        } catch (SAXException e) {
            throw new APIManagementException(errorMsg, e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Reads baseURI and validate if it is WSDL 2.0 resource.
     * 
     * @throws org.apache.woden.WSDLException When error occurred while parsing/validating base URI
     * @throws APIManagementException When error occurred while parsing/validating base URI
     */
    private void readAndValidateWSDL20() throws org.apache.woden.WSDLException, APIManagementException {
        org.apache.woden.WSDLReader wsdlReader20 = org.apache.woden.WSDLFactory.newInstance().newWSDLReader();
        Document document = getSecuredParsedDocumentFromURL(baseURI);
        Element domElement = document.getDocumentElement();
        WSDLSource wsdlSource = wsdlReader20.createWSDLSource();
        wsdlSource.setSource(domElement);
        wsdlReader20.readWSDL(wsdlSource);
    }
    
    /**
     * Reads baseURI and validate if it is WSDL 1.1 resource.
     *
     * @throws WSDLException When error occurred while parsing/validating base URI
     * @throws APIManagementException When error occurred while parsing/validating base URI
     */
    private void readAndValidateWSDL11() throws WSDLException, APIManagementException {
        javax.wsdl.xml.WSDLReader wsdlReader11 = javax.wsdl.factory.WSDLFactory.newInstance().newWSDLReader();
        wsdlReader11.readWSDL(null, getSecuredParsedDocumentFromURL(baseURI));
    }

    /**
	 * Clear the actual service Endpoint and use Gateway Endpoint instead of the
	 * actual Endpoint.
	 *
	 * @param definition
	 *            - {@link Definition} - WSDL4j wsdl definition
	 * @throws APIManagementException
	 */

	private void setServiceDefinition(Definition definition, API api) throws APIManagementException {

		Map serviceMap = definition.getAllServices();
		Iterator serviceItr = serviceMap.entrySet().iterator();
		URL addressURI;
		try {
			while (serviceItr.hasNext()) {
				Map.Entry svcEntry = (Map.Entry) serviceItr.next();
				Service svc = (Service) svcEntry.getValue();
				Map portMap = svc.getPorts();
				for (Object o : portMap.entrySet()) {
					Map.Entry portEntry = (Map.Entry) o;
					Port port = (Port) portEntry.getValue();

					List<ExtensibilityElement> extensibilityElementList = port.getExtensibilityElements();
					for (ExtensibilityElement extensibilityElement : extensibilityElementList) {
						addressURI = new URL(getAddressUrl(extensibilityElement));
						String endpointTransport = determineURLTransport(addressURI.getProtocol(), api.getTransports());
						setAddressUrl(extensibilityElement, endpointTransport, api);
					}
				}
			}
		} catch (Exception e) {
			String errorMsg = "Error occurred while getting the wsdl address location";
			log.error(errorMsg, e);
			throw new APIManagementException(errorMsg, e);
		}
	}

	/**
	 * Get the addressURl from the Extensibility element
	 * @param exElement - {@link ExtensibilityElement}
	 * @return {@link String}
	 * @throws APIManagementException
	 */
	private String getAddressUrl(ExtensibilityElement exElement) throws APIManagementException {
		if (exElement instanceof SOAP12AddressImpl) {
			return ((SOAP12AddressImpl) exElement).getLocationURI();
		} else if (exElement instanceof SOAPAddressImpl) {
			return ((SOAPAddressImpl) exElement).getLocationURI();
		} else if (exElement instanceof HTTPAddressImpl) {
			return ((HTTPAddressImpl) exElement).getLocationURI();
		} else {
			String msg = "Unsupported WSDL errors!";
			log.error(msg);
			throw new APIManagementException(msg);
		}
	}

	/**
	 * Get the addressURl from the Extensibility element
	 * @param exElement - {@link ExtensibilityElement}
	 * @throws APIManagementException
	 */
	private void setAddressUrl(ExtensibilityElement exElement, String transports, API api) throws APIManagementException {

        if (exElement instanceof SOAP12AddressImpl) {
            ((SOAP12AddressImpl) exElement).setLocationURI(APIUtil.getGatewayendpoint(transports) + api.getContext());
        } else if (exElement instanceof SOAPAddressImpl) {
            ((SOAPAddressImpl) exElement).setLocationURI(APIUtil.getGatewayendpoint(transports) + api.getContext());
        } else if (exElement instanceof HTTPAddressImpl) {
            ((HTTPAddressImpl) exElement).setLocationURI(APIUtil.getGatewayendpoint(transports) + api.getContext());
        } else {
			String msg = "Unsupported WSDL errors!";
			log.error(msg);
			throw new APIManagementException(msg);
		}
	}

	private void setAddressUrl(EndpointElement endpoint,URI uri) throws APIManagementException {
        endpoint.setAddress(uri);
    }

    public static String toString(ByteArrayInputStream is) {
        int size = is.available();
        char[] theChars = new char[size];
        byte[] bytes    = new byte[size];

        is.read(bytes, 0, size);
        for (int i = 0; i < size;)
            theChars[i] = (char)(bytes[i++]&0xff);

        return new String(theChars);
    }

    private String determineURLTransport(String scheme, String transports) {
        // If transports is defined as "http,https" consider the actual transport
        // protocol of the url, else give priority to the transport defined at API level
        if ("http,https".equals(transports) || "https,http".equals(transports)) {
            if ("http".equals(scheme)) {
                return "http";
            }
            else if (scheme.startsWith("https")) {
                return "https";
            }
        }

        return transports;
    }

}
