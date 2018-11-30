package eu.domibus.ebms3.sender;

import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Cosmin Baciu
 * @since 4.1
 */
@Service
public class SourceMessageSender implements MessageSender {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(SourceMessageSender.class);

    @Override
    public void sendMessage(final UserMessage userMessage) {
        LOG.info("-------------SourceMessageSender");
    }
}
