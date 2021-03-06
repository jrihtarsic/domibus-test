package eu.domibus.ebms3.sender;

import eu.domibus.api.message.UserMessageLogService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.MSHRole;
import eu.domibus.common.dao.ErrorLogDao;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Reliability;
import eu.domibus.common.model.logging.ErrorLogEntry;
import eu.domibus.core.pmode.PModeProvider;
import eu.domibus.ebms3.common.matcher.ReliabilityMatcher;
import eu.domibus.ebms3.common.model.*;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import org.apache.wss4j.dom.WSConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

import static eu.domibus.api.property.DomibusPropertyMetadataManager.DOMIBUS_DISPATCH_EBMS_ERROR_UNRECOVERABLE_RETRY;

/**
 * @author Christian Koch, Stefan Mueller
 */
@Service
public class ReliabilityChecker {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(ReliabilityChecker.class);

    @Autowired
    @Qualifier("jaxbContextEBMS")
    protected JAXBContext jaxbContext;

    @Autowired
    protected NonRepudiationChecker nonRepudiationChecker;

    @Autowired
    protected PModeProvider pModeProvider;

    @Autowired
    protected UserMessageLogService userMessageLogService;

    @Autowired
    protected ErrorLogDao errorLogDao;

    @Autowired
    protected ReliabilityMatcher pushMatcher;

    @Autowired
    protected TransformerFactory transformerFactory;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Transactional(rollbackFor = EbMS3Exception.class)
    public CheckResult check(final SOAPMessage request, final SOAPMessage response, final Reliability reliability) throws EbMS3Exception {
        return checkReliability(request, response, reliability, pushMatcher);
    }

    @Transactional(rollbackFor = EbMS3Exception.class)
    public CheckResult check(final SOAPMessage request, final SOAPMessage response, final String pmodeKey) throws EbMS3Exception {
        return check(request, response, pmodeKey, pushMatcher);
    }


    @Transactional(rollbackFor = EbMS3Exception.class)
    public CheckResult check(final SOAPMessage request, final SOAPMessage response, final String pmodeKey, final ReliabilityMatcher matcher) throws EbMS3Exception {
        final LegConfiguration legConfiguration = this.pModeProvider.getLegConfiguration(pmodeKey);
        return checkReliability(request, response, legConfiguration.getReliability(), matcher);
    }


    protected CheckResult checkReliability(final SOAPMessage request, final SOAPMessage response, Reliability reliability, final ReliabilityMatcher matcher) throws EbMS3Exception {
        String messageId = null;

        if (matcher.matchReliableCallBack(reliability)) {
            LOG.debug("Reply pattern is waiting for callback, setting message status to WAITING_FOR_CALLBACK.");
            return CheckResult.WAITING_FOR_CALLBACK;
        }

        if (matcher.matchReliableReceipt(reliability)) {
            LOG.debug("Checking reliability for outgoing message");
            final Messaging messaging;

            try {
                messaging = this.jaxbContext.createUnmarshaller().unmarshal((Node) response.getSOAPHeader().getChildElements(ObjectFactory._Messaging_QNAME).next(), Messaging.class).getValue();
            } catch (JAXBException | SOAPException e) {
                LOG.error(e.getMessage(), e);
                return matcher.fails();
            }

            final SignalMessage signalMessage = messaging.getSignalMessage();

            //ReceiptionAwareness or NRR found but not expected? report if configuration=true //TODO: make configurable in domibus.properties

            //SignalMessage with Receipt expected
            messageId = getMessageId(signalMessage);
            if (signalMessage.getReceipt() != null && signalMessage.getReceipt().getAny().size() == 1) {

                final String contentOfReceiptString = signalMessage.getReceipt().getAny().get(0);

                try {
                    if (!reliability.isNonRepudiation()) {
                        final UserMessage userMessage = this.jaxbContext.createUnmarshaller().unmarshal(new StreamSource(new ByteArrayInputStream(contentOfReceiptString.getBytes())), UserMessage.class).getValue();

                        final UserMessage userMessageInRequest = this.jaxbContext.createUnmarshaller().unmarshal((Node) request.getSOAPHeader().getChildElements(ObjectFactory._Messaging_QNAME).next(), Messaging.class).getValue().getUserMessage();
                        if (!userMessage.equals(userMessageInRequest)) {
                            ReliabilityChecker.LOG.warn("Reliability check failed, the user message in the request does not match the user message in the response.");
                            return matcher.fails();
                        }

                        return CheckResult.OK;
                    }

                    final Iterator<Element> elementIterator = response.getSOAPHeader().getChildElements(new QName(WSConstants.WSSE_NS, WSConstants.WSSE_LN));

                    if (!elementIterator.hasNext()) {
                        LOG.businessError(DomibusMessageCode.BUS_RELIABILITY_INVALID_WITH_NO_SECURITY_HEADER, messageId);
                        EbMS3Exception ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0302, "Invalid NonRepudiationInformation: No security header found", null, null);
                        ex.setMshRole(MSHRole.SENDING);
                        ex.setSignalMessageId(messageId);
                        throw ex;
                    }
                    final Element securityHeaderResponse = elementIterator.next();

                    if (elementIterator.hasNext()) {
                        LOG.businessError(DomibusMessageCode.BUS_RELIABILITY_INVALID_WITH_MULTIPLE_SECURITY_HEADERS, messageId);
                        EbMS3Exception ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0302, "Invalid NonRepudiationInformation: Multiple security headers found", null, null);
                        ex.setMshRole(MSHRole.SENDING);
                        ex.setSignalMessageId(messageId);
                        throw ex;
                    }

                    final String wsuIdOfMEssagingElement = messaging.getOtherAttributes().get(new QName(WSConstants.WSU_NS, "Id"));

                    ReliabilityChecker.LOG.debug(wsuIdOfMEssagingElement);

                    final NodeList nodeList = securityHeaderResponse.getElementsByTagNameNS(WSConstants.SIG_NS, WSConstants.REF_LN);
                    boolean signatureFound = false;
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        final Node node = nodeList.item(i);
                        if (this.compareReferenceIgnoreHashtag(node.getAttributes().getNamedItem("URI").getNodeValue(), wsuIdOfMEssagingElement)) {
                            signatureFound = true;
                            break;
                        }
                    }
                    if (!signatureFound) {
                        LOG.businessError(DomibusMessageCode.BUS_RELIABILITY_INVALID_WITH_MESSAGING_NOT_SIGNED, messageId);
                        LOG.error("Response message [{}]", soapPartToString(response));
                        EbMS3Exception ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0302, "Invalid NonRepudiationInformation: eb:Messaging not signed", null, null);
                        ex.setMshRole(MSHRole.SENDING);
                        ex.setSignalMessageId(messageId);
                        throw ex;
                    }

                    final NodeList referencesFromSecurityHeader = nonRepudiationChecker.getNonRepudiationNodeList(request.getSOAPHeader().getElementsByTagNameNS(WSConstants.SIG_NS, WSConstants.SIG_INFO_LN).item(0));
                    final NodeList referencesFromNonRepudiationInformation = nonRepudiationChecker.getNonRepudiationNodeList(response.getSOAPHeader().getElementsByTagNameNS(NonRepudiationConstants.NS_NRR, NonRepudiationConstants.NRR_LN).item(0));

                    if (!nonRepudiationChecker.compareUnorderedReferenceNodeLists(referencesFromSecurityHeader, referencesFromNonRepudiationInformation)) {
                        LOG.businessError(DomibusMessageCode.BUS_RELIABILITY_INVALID_NOT_MATCHING_THE_MESSAGE, soapPartToString(response), soapPartToString(request));
                        EbMS3Exception ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0302, "Invalid NonRepudiationInformation: non repudiation information and request message do not match", null, null);
                        ex.setMshRole(MSHRole.SENDING);
                        ex.setSignalMessageId(messageId);
                        throw ex;
                    }

                    LOG.businessInfo(DomibusMessageCode.BUS_RELIABILITY_SUCCESSFUL, messageId);
                    return CheckResult.OK;
                } catch (final JAXBException e) {
                    ReliabilityChecker.LOG.error("", e);
                } catch (final SOAPException e) {
                    ReliabilityChecker.LOG.error("", e);
                }

            } else {
                LOG.businessError(DomibusMessageCode.BUS_RELIABILITY_RECEIPT_INVALID_EMPTY, messageId);
                EbMS3Exception ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0302, "There is no content inside the receipt element received by the responding gateway", messageId, null);
                ex.setMshRole(MSHRole.SENDING);
                ex.setSignalMessageId(messageId);
                throw ex;
            }

        }
        LOG.businessError(DomibusMessageCode.BUS_RELIABILITY_GENERAL_ERROR, messageId);
        return matcher.fails();

    }


    protected String soapPartToString(SOAPMessage soapMessage) {
        if(soapMessage == null) {
            return null;
        }
        try (StringWriter stringWriter = new StringWriter()) {
            transformerFactory.newTransformer().transform(new DOMSource(soapMessage.getSOAPPart()), new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch (IOException | TransformerException e) {
            LOG.warn("Couldn't get soap part", e);
        }
        return null;
    }

    protected String getMessageId(SignalMessage signalMessage) {
        if(signalMessage == null || signalMessage.getMessageInfo() == null) {
            return null;
        }
        return signalMessage.getMessageInfo().getMessageId();
    }

    /**
     * Compares two contentIds but ignores hashtags that were used for referencing inside a document
     *
     * @param referenceId the id with an hashtag
     * @param contentId   the id of the content to match
     * @return {@code true} in case both values are equal (ignoring the hashtag), else {@link false}
     */
    private boolean compareReferenceIgnoreHashtag(final String referenceId, final String contentId) {
        return referenceId.substring(1).equals(contentId);
    }

    public enum CheckResult {
        OK, SEND_FAIL, PULL_FAILED, WAITING_FOR_CALLBACK, ABORT
    }


    /**
     * This method is responsible for the ebMS3 error handling (creation of errorlogs and marking message as sent)
     *
     * @param exceptionToHandle the exception {@link eu.domibus.common.exception.EbMS3Exception} that needs to be handled
     * @param messageId         id of the message the exception belongs to
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleEbms3Exception(final EbMS3Exception exceptionToHandle, final String messageId) {
        exceptionToHandle.setRefToMessageId(messageId);
        exceptionToHandle.setMshRole(MSHRole.SENDING);
        this.errorLogDao.create(new ErrorLogEntry(exceptionToHandle));
        // The backends are notified that an error occurred in the UpdateRetryLoggingService
    }
}
