package eu.domibus.plugin;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.plugin.notification.AsyncNotificationListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.jms.config.JmsListenerContainerFactory;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@Configuration
public class AsyncNotificationListenerServiceConfiguration {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(AsyncNotificationListenerServiceConfiguration.class);

    protected JmsListenerContainerFactory jmsListenerContainerFactory;
    protected AuthUtils authUtils;
    protected DomainContextProvider domainContextProvider;
    protected PluginEventNotifierProvider pluginEventNotifierProvider;

    public AsyncNotificationListenerServiceConfiguration(@Qualifier("internalJmsListenerContainerFactory") JmsListenerContainerFactory jmsListenerContainerFactory,
                                                         AuthUtils authUtils,
                                                         DomainContextProvider domainContextProvider,
                                                         PluginEventNotifierProvider pluginEventNotifierProvider) {
        this.jmsListenerContainerFactory = jmsListenerContainerFactory;
        this.authUtils = authUtils;
        this.domainContextProvider = domainContextProvider;
        this.pluginEventNotifierProvider = pluginEventNotifierProvider;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public AsyncNotificationListenerService createAsyncNotificationListenerService(AsyncNotificationListener asyncNotificationListener) {
        AsyncNotificationListenerService notificationListenerServiceImpl = new AsyncNotificationListenerService(domainContextProvider, asyncNotificationListener, pluginEventNotifierProvider, authUtils);
        return notificationListenerServiceImpl;
    }
}