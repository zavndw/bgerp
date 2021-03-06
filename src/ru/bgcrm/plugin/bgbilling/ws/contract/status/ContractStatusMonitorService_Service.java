
package ru.bgcrm.plugin.bgbilling.ws.contract.status;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.4-b01
 * Generated source version: 2.2
 * 
 * Переделать на JSON-RPC.
 */
@Deprecated
@WebServiceClient(name = "ContractStatusMonitorService", targetNamespace = "http://service.common.status.contract.kernel.bgbilling.bitel.ru/", wsdlLocation = "http://billing:8081/executer/ru.bitel.bgbilling.kernel.contract.status/ContractStatusMonitorService?wsdl")
public class ContractStatusMonitorService_Service
    extends Service
{

    private final static URL CONTRACTSTATUSMONITORSERVICE_WSDL_LOCATION;
    private final static WebServiceException CONTRACTSTATUSMONITORSERVICE_EXCEPTION;
    private final static QName CONTRACTSTATUSMONITORSERVICE_QNAME = new QName("http://service.common.status.contract.kernel.bgbilling.bitel.ru/", "ContractStatusMonitorService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://billing:8081/executer/ru.bitel.bgbilling.kernel.contract.status/ContractStatusMonitorService?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        CONTRACTSTATUSMONITORSERVICE_WSDL_LOCATION = url;
        CONTRACTSTATUSMONITORSERVICE_EXCEPTION = e;
    }

    public ContractStatusMonitorService_Service() {
        super(__getWsdlLocation(), CONTRACTSTATUSMONITORSERVICE_QNAME);
    }

    public ContractStatusMonitorService_Service(WebServiceFeature... features) {
        super(__getWsdlLocation(), CONTRACTSTATUSMONITORSERVICE_QNAME, features);
    }

    public ContractStatusMonitorService_Service(URL wsdlLocation) {
        super(wsdlLocation, CONTRACTSTATUSMONITORSERVICE_QNAME);
    }

    public ContractStatusMonitorService_Service(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, CONTRACTSTATUSMONITORSERVICE_QNAME, features);
    }

    public ContractStatusMonitorService_Service(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public ContractStatusMonitorService_Service(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns ContractStatusMonitorService
     */
    @WebEndpoint(name = "ContractStatusMonitorService")
    public ContractStatusMonitorService getContractStatusMonitorService() {
        return super.getPort(new QName("http://service.common.status.contract.kernel.bgbilling.bitel.ru/", "ContractStatusMonitorService"), ContractStatusMonitorService.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns ContractStatusMonitorService
     */
    @WebEndpoint(name = "ContractStatusMonitorService")
    public ContractStatusMonitorService getContractStatusMonitorService(WebServiceFeature... features) {
        return super.getPort(new QName("http://service.common.status.contract.kernel.bgbilling.bitel.ru/", "ContractStatusMonitorService"), ContractStatusMonitorService.class, features);
    }

    private static URL __getWsdlLocation() {
        if (CONTRACTSTATUSMONITORSERVICE_EXCEPTION!= null) {
            throw CONTRACTSTATUSMONITORSERVICE_EXCEPTION;
        }
        return CONTRACTSTATUSMONITORSERVICE_WSDL_LOCATION;
    }

}
