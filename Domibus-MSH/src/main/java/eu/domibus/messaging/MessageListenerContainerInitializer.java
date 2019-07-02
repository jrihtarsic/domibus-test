package eu.domibus.messaging;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.ext.delegate.converter.DomainExtConverter;
import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.listener.AbstractJmsListeningContainer;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ion Perpegel
 * @author Cosmin Baciu
 * @since 4.0
 */
@Service
public class MessageListenerContainerInitializer {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MessageListenerContainerInitializer.class);

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    protected MessageListenerContainerFactory messageListenerContainerFactory;

    @Autowired
    protected DomainService domainService;

    @Autowired
    protected DomainExtConverter domainConverter;

    protected List<MessageListenerContainer> instances = new ArrayList<>();

    @PostConstruct
    public void init() {
        final List<Domain> domains = domainService.getDomains();
        for (Domain domain : domains) {
            createSendMessageListenerContainer(domain);
            createSendLargeMessageListenerContainer(domain);
            createSplitAndJoinListenerContainer(domain);
            createPullReceiptListenerContainer(domain);
            createRetentionListenerContainer(domain);

            createMessageListenersForPlugins(domain);
        }
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        LOG.info("Shutting down MessageListenerContainer instances");

        for (MessageListenerContainer instance : instances) {
            try {
                ((AbstractJmsListeningContainer) instance).shutdown();
            } catch (Exception e) {
                LOG.error("Error while shutting down MessageListenerContainer", e);
            }
        }
    }

    /**
     * It will collect and instantiates all {@link PluginMessageListenerContainer} defined in plugins
     *
     * @param domain
     */
    public void createMessageListenersForPlugins(Domain domain) {
        DomainDTO domainDTO = domainConverter.convert(domain, DomainDTO.class);

        final Map<String, PluginMessageListenerContainer> beansOfType = applicationContext.getBeansOfType(PluginMessageListenerContainer.class);

        for (Map.Entry<String, PluginMessageListenerContainer> entry : beansOfType.entrySet()) {
            final String pluginMessageListenerContainerName = entry.getKey();
            final PluginMessageListenerContainer pluginMessageListenerContainer = entry.getValue();

            MessageListenerContainer instance = pluginMessageListenerContainer.createMessageListenerContainer(domainDTO);
            instance.start();
            instances.add(instance);
            LOG.info("{} initialized for domain [{}]", pluginMessageListenerContainerName, domain);
        }
    }
//
//    private synchronized void startInstance(MessageListenerContainer instance, Domain domain) {
//        instance.start();
//
//        List<MessageListenerContainer> list = instances.get(domain);
//        if (list == null) {
//            list = new ArrayList<>();
//            instances.put(domain, list);
//        }
//        list.add(instance);
//    }

    private void stopInstance(MessageListenerContainer instance, Domain domain) {
        try {
            ((AbstractJmsListeningContainer) instance).shutdown();
        } catch (Exception e) {
            LOG.error("Error while shutting down MessageListenerContainer for domain [{}]", domain, e);
            return;
        }
    }
//
//    private MessageListenerContainer findInstance(Domain domain, String beanName) {
//        List<MessageListenerContainer> list = instances.get(domain);
//
//        DefaultMessageListenerContainer a;
//
//    }


    //Map<Domain, MessageListenerContainer> instances1 = new HashMap<>();

    public void createSendMessageListenerContainer(Domain domain) {
//        if (instances1.containsKey(domain)) {
//            try {
//                ((AbstractJmsListeningContainer) instances1.get(domain)).shutdown();
//                instances1.remove(domain);
//            } catch (Exception e) {
//                LOG.error("Error while shutting down MessageListenerContainer for domain [{}]", domain, e);
//                return;
//            }
//        }

        MessageListenerContainer instance = messageListenerContainerFactory.createSendMessageListenerContainer(domain);
        removeOldInstanceIfAny((DomainMessageListenerContainer) instance);
        instance.start();
        instances.add(instance);
        LOG.info("MessageListenerContainer initialized for domain [{}]", domain);
    }

    private void removeOldInstanceIfAny(DomainMessageListenerContainer instance) {
        DomainMessageListenerContainer oldInstance = instances.stream()
                .map(i -> (DomainMessageListenerContainer) i)
                .filter(i -> i.getName().equals(instance.getName()) && i.getDomain().equals(instance.getDomain()))
                .findFirst().orElse(null);
        if (oldInstance != null) {
            try {
                oldInstance.shutdown();
            } catch (Exception e) {
                LOG.error("Error while shutting down MessageListenerContainer for domain [{}]", instance.getDomain(), e);
            }
            instances.remove(oldInstance);
        }
    }

    public void createSendLargeMessageListenerContainer(Domain domain) {
        MessageListenerContainer instance = messageListenerContainerFactory.createSendLargeMessageListenerContainer(domain);
        instance.start();
        instances.add(instance);
        LOG.info("LargeMessageListenerContainer initialized for domain [{}]", domain);
    }

    public void createPullReceiptListenerContainer(Domain domain) {
        MessageListenerContainer instance = messageListenerContainerFactory.createPullReceiptListenerContainer(domain);
        instance.start();
        instances.add(instance);
        LOG.info("PullReceiptListenerContainer initialized for domain [{}]", domain);
    }

    public void createSplitAndJoinListenerContainer(Domain domain) {
        MessageListenerContainer instance = messageListenerContainerFactory.createSplitAndJoinListenerContainer(domain);
        instance.start();
        instances.add(instance);
        LOG.info("SplitAndJoinListenerContainer initialized for domain [{}]", domain);
    }

    public void createRetentionListenerContainer(Domain domain) {
        MessageListenerContainer instance = messageListenerContainerFactory.createRetentionListenerContainer(domain);
        instance.start();
        instances.add(instance);
        LOG.info("RetentionListenerContainer initialized for domain [{}]", domain);
    }
}
