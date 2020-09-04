package eu.domibus.core.ebms3.sender;

import eu.domibus.core.metrics.Counter;
import eu.domibus.core.metrics.Timer;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.MDCKey;
import org.springframework.stereotype.Service;

import javax.jms.Message;


/**
 * This class is responsible for the handling of outgoing messages.
 *
 * @author Christian Koch, Stefan Mueller
 * @author Cosmin Baciu
 * @since 3.0
 */
@Service
public class MessageSenderListener extends AbstractMessageSenderListener {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MessageSenderListener.class);

    @MDCKey(DomibusLogger.MDC_MESSAGE_ID)
    @Override
    @Timer(clazz = MessageSenderListener.class,value="onMessage")
    @Counter(clazz = MessageSenderListener.class,value="onMessage")
    public void onMessage(final Message message) {
        LOG.debug("Processing message [{}]", message);
        super.onMessage(message);
    }

    @Override
    public void scheduleSending(String messageId, Long delay) {
        super.userMessageService.scheduleSending(messageId, delay);
    }

    @Override
    public void sendUserMessage(String messageId, int retryCount) {
        super.messageSenderService.sendUserMessage(messageId, retryCount);
    }

}
