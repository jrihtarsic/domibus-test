package eu.domibus.messaging;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.listener.AbstractJmsListeningContainer;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ion Perpegel
 * @since 4.0
 */
@Service
public class MessageListenerContainerInitializer {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MessageListenerContainerInitializer.class);

    @Autowired
    protected MessageListenerContainerFactory messageListenerContainerFactory;

    @Autowired
    protected DomainService domainService;

    protected Map<Domain, MessageListenerContainer> messageListenerInstances = new HashMap<>();
    protected Map<Domain, MessageListenerContainer> pullReceiptListenerInstances = new HashMap<>();

    @PostConstruct
    public void init() {
        final List<Domain> domains = domainService.getDomains();
        for (Domain domain : domains) {
            createMessageListenerContainer(domain);
            createPullReceiptListenerContainer(domain);
        }
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        destroy(messageListenerInstances);
        destroy(pullReceiptListenerInstances);
    }

    public void destroy(Map<Domain, MessageListenerContainer> instances) throws InterruptedException {
        LOG.info("Shutting down MessageListenerContainer instances");

        for (MessageListenerContainer instance: instances.values()) {
            try {
                ((AbstractJmsListeningContainer) instance).shutdown();
            }catch(Exception e) {
                LOG.error("Error while shutting down MessageListenerContainer", e);
            }
        }
    }

    public void createMessageListenerContainer(Domain domain) {
        MessageListenerContainer instance = messageListenerContainerFactory.createMessageListenerContainer(domain);
        instance.start();
        messageListenerInstances.put(domain, instance);
        LOG.info("MessageListenerContainer initialized for domain [{}]", domain);
    }

    public void createPullReceiptListenerContainer(Domain domain) {
        MessageListenerContainer instance = messageListenerContainerFactory.createPullReceiptListenerContainer(domain);
        instance.start();
        pullReceiptListenerInstances.put(domain, instance);
        LOG.info("PullReceiptListenerContainer initialized for domain [{}]", domain);
    }


}
