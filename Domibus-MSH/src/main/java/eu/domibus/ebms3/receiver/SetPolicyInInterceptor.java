package eu.domibus.ebms3.receiver;

import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.MSHRole;
import eu.domibus.ebms3.common.dao.PModeProvider;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.api.message.ebms3.model.MessageInfo;
import eu.domibus.api.message.ebms3.model.Messaging;
import eu.domibus.api.message.ebms3.model.ObjectFactory;
import eu.domibus.ebms3.sender.MSHDispatcher;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import org.apache.cxf.attachment.AttachmentDataSource;
import org.apache.cxf.binding.soap.HeaderUtil;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.policy.PolicyInInterceptor;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.neethi.Policy;
import org.apache.neethi.builders.converters.StaxToDOMConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This interceptor is responsible for discovery and setup of WS-Security Policies for incoming messages.
 *
 * @author Christian Koch, Stefan Mueller
 * @since 3.0
 */
public class SetPolicyInInterceptor extends AbstractSoapInterceptor {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(SetPolicyInInterceptor.class);
    private JAXBContext jaxbContext;

    @Autowired
    private PModeProvider pModeProvider;

    @Autowired
    private DomibusConfigurationService domibusConfigurationService;

    public SetPolicyInInterceptor() {
        super(Phase.RECEIVE);
        this.addBefore(PolicyInInterceptor.class.getName());
        this.addAfter(AttachmentInInterceptor.class.getName());
    }

    public void setJaxbContext(final JAXBContext jaxbContext) {
        this.jaxbContext = jaxbContext;
    }

    /**
     * Intercepts a message.
     * Interceptors should NOT invoke handleMessage or handleFault
     * on the next interceptor - the interceptor chain will
     * take care of this.
     *
     * @param message the incoming message to handle
     */
    @Override
    public void handleMessage(final SoapMessage message) throws Fault {
        final InputStream inputStream = message.getContent(InputStream.class);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Messaging messaging = null;
        String policyName = null;
        String messageId = null;

        try {
            IOUtils.copy(inputStream, byteArrayOutputStream); //FIXME: do not copy the whole byte[], use SequenceInputstream instead
            final byte[] data = byteArrayOutputStream.toByteArray();
            message.setContent(InputStream.class, new ByteArrayInputStream(data));
            new StaxInInterceptor().handleMessage(message);
            final XMLStreamReader xmlStreamReader = message.getContent(XMLStreamReader.class);
            final Element soapEnvelope = new StaxToDOMConverter().convert(xmlStreamReader);
            message.removeContent(XMLStreamReader.class);
            message.setContent(InputStream.class, new ByteArrayInputStream(data));
            //message.setContent(XMLStreamReader.class, XMLInputFactory.newInstance().createXMLStreamReader(message.getContent(InputStream.class)));
            final Node messagingNode = soapEnvelope.getElementsByTagNameNS(ObjectFactory._Messaging_QNAME.getNamespaceURI(), ObjectFactory._Messaging_QNAME.getLocalPart()).item(0);
            messaging = ((JAXBElement<Messaging>) this.jaxbContext.createUnmarshaller().unmarshal(messagingNode)).getValue();
            messageId = messaging.getUserMessage().getMessageInfo().getMessageId();

            //set the messageId in the MDC context
            LOG.putMDC(DomibusLogger.MDC_MESSAGE_ID, messageId);

            final String pmodeKey = this.pModeProvider.findPModeKeyForUserMessage(messaging.getUserMessage(), MSHRole.RECEIVING); // FIXME: This does not work for signalmessages
            final LegConfiguration legConfiguration = this.pModeProvider.getLegConfiguration(pmodeKey);
            final PolicyBuilder builder = message.getExchange().getBus().getExtension(PolicyBuilder.class);
            policyName = legConfiguration.getSecurity().getPolicy();
            final Policy policy = builder.getPolicy(new FileInputStream(new File(domibusConfigurationService.getConfigLocation() + File.separator + "policies", policyName)));
            LOG.businessInfo(DomibusMessageCode.BUS_SECURITY_POLICY_INCOMING_USE, policyName);

            message.put(MSHDispatcher.PMODE_KEY_CONTEXT_PROPERTY, pmodeKey);
            //FIXME: Test!!!!
            message.getExchange().put(MSHDispatcher.PMODE_KEY_CONTEXT_PROPERTY, pmodeKey);
            //FIXME: Consistent way! If properties are added to the exchange you will have access via PhaseInterceptorChain

            message.getExchange().put(MessageInfo.MESSAGE_ID_CONTEXT_PROPERTY, messageId);

            //FIXME: the exchange is shared by both the request and the response. This would result in a situation where the policy for an incoming request would be used for the response. I think this is what we want
            message.getExchange().put(PolicyConstants.POLICY_OVERRIDE, policy);
            message.put(PolicyConstants.POLICY_OVERRIDE, policy);
            message.getInterceptorChain().add(new SetPolicyInInterceptor.CheckEBMSHeaderInterceptor());
            message.getInterceptorChain().add(new SetPolicyInInterceptor.SOAPMessageBuilderInterceptor());
            final String securityAlgorithm = legConfiguration.getSecurity().getSignatureMethod().getAlgorithm();
            message.put(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM, securityAlgorithm);
            message.getExchange().put(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM, securityAlgorithm);
            LOG.businessInfo(DomibusMessageCode.BUS_SECURITY_ALGORITHM_INCOMING_USE, securityAlgorithm);

        } catch (EbMS3Exception e) {
            SetPolicyInInterceptor.LOG.debug("", e); // Those errors are expected (no PMode found, therefore DEBUG)
            throw new Fault(e);
        } catch (IOException | ParserConfigurationException | SAXException | JAXBException e) {
            LOG.businessError(DomibusMessageCode.BUS_SECURITY_POLICY_INCOMING_NOT_FOUND, e, policyName); // Those errors are not expected
            EbMS3Exception ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0010, "no valid security policy found", messaging != null ? messageId : "unknown", e);
            ex.setMshRole(MSHRole.RECEIVING);
            throw new Fault(ex);
        }

    }


    public static class CheckEBMSHeaderInterceptor extends AbstractSoapInterceptor {

        public CheckEBMSHeaderInterceptor() {
            super(Phase.PRE_LOGICAL);
            this.addBefore(MustUnderstandInterceptor.MustUnderstandEndingInterceptor.class.getName());

        }


        @Override
        public void handleMessage(final SoapMessage message) {
            HeaderUtil.getHeaderQNameInOperationParam(message).add(ObjectFactory._Messaging_QNAME);
        }

        @Override
        public Set<QName> getUnderstoodHeaders() {
            final Set<QName> understood = new HashSet<>();
            understood.add(ObjectFactory._Messaging_QNAME);
            return understood;
        }
    }

    public static class SOAPMessageBuilderInterceptor extends AbstractSoapInterceptor {

        public SOAPMessageBuilderInterceptor() {
            super(Phase.PRE_LOGICAL);
            this.addAfter(MustUnderstandInterceptor.MustUnderstandEndingInterceptor.class.getName());
        }


        @Override
        public void handleMessage(final SoapMessage message) throws Fault {
            final SOAPMessage result = message.getContent(SOAPMessage.class);
            try {
                SAAJInInterceptor.replaceHeaders(result, message);


                result.removeAllAttachments();
                final Collection<Attachment> atts = message.getAttachments();
                if (atts != null) {
                    for (final Attachment a : atts) {
                        if (a.getDataHandler().getDataSource() instanceof AttachmentDataSource) {
                            try {
                                ((AttachmentDataSource) a.getDataHandler().getDataSource()).cache(message);
                            } catch (final IOException e) {
                                throw new Fault(e);
                            }
                        }
                        final AttachmentPart ap = result.createAttachmentPart(a.getDataHandler());
                        final Iterator<String> i = a.getHeaderNames();
                        while (i != null && i.hasNext()) {
                            final String h = i.next();
                            final String val = a.getHeader(h);
                            ap.addMimeHeader(h, val);
                        }
                        if (StringUtils.isEmpty(ap.getContentId())) {
                            ap.setContentId(a.getId());
                        }
                        result.addAttachmentPart(ap);
                    }
                }

            } catch (final SOAPException soapEx) {
                LOG.error("Could not replace headers for incoming Message", soapEx);
            }

        }
    }
}


