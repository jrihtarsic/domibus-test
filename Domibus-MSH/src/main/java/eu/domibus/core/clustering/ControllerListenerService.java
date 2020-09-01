package eu.domibus.core.clustering;

import eu.domibus.api.cluster.Command;
import eu.domibus.api.cluster.CommandExecutorService;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


/**
 * @author kochc01
 * @author Cosmin Baciu
 */
@Service
public class ControllerListenerService implements MessageListener {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(ControllerListenerService.class);

    @Autowired
    protected CommandExecutorService commandExecutorService;

    @Autowired
    protected DomainService domainService;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Override
    @Transactional
    public void onMessage(Message message) {
        String command;
        try {
            command = message.getStringProperty(Command.COMMAND);
        } catch (JMSException e) {
            LOG.error("Could not parse command", e);
            return;
        }
        if (command == null) {
            LOG.error("Received null command");
            return;
        }

        try {
            String domainCode = message.getStringProperty(MessageConstants.DOMAIN);
            if (StringUtils.isEmpty(domainCode)) {
                domainContextProvider.clearCurrentDomain();
            } else {
                Domain domain = domainService.getDomain(domainCode);
                if (domain == null) {
                    LOG.error("Invalid domain received in command: [{}]", domainCode);
                    return;
                }
                domainContextProvider.setCurrentDomain(domainCode);
            }
        } catch (JMSException e) {
            LOG.error("Could not get the domain", e);
            return;
        }

        commandExecutorService.executeCommand(command, getCommandProperties(message));
    }

    /**
     * just extract all message properties (of type {@code String}) excepting Command and Domain
     *
     * @param msg JMS Message
     * @return map of properties
     */
    protected Map<String, String> getCommandProperties(Message msg) {
        HashMap<String, String> properties = new HashMap<>();
        try {
            Enumeration srcProperties = msg.getPropertyNames();
            while (srcProperties.hasMoreElements()) {
                String propertyName = (String) srcProperties.nextElement();
                if (!Command.COMMAND.equalsIgnoreCase(propertyName) && !MessageConstants.DOMAIN.equalsIgnoreCase(propertyName)
                        && msg.getObjectProperty(propertyName) instanceof String) {
                    properties.put(propertyName, msg.getStringProperty(propertyName));
                }
            }
        } catch (JMSException e) {
            LOG.error("An error occurred while trying to extract message properties: ", e);
        }
        return properties;
    }

}
