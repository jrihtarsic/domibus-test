package eu.domibus.messaging.jms;

import eu.domibus.api.jms.JMSDestination;
import eu.domibus.jms.spi.InternalJMSDestination;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Cosmin Baciu
 * @since 3.2
 */
@Component
public class JMSDestinationMapper {

    public Map<String, JMSDestination> convert(Map<String, InternalJMSDestination> destinations) {
        Map<String, JMSDestination> result = new HashMap<>();
        for (Map.Entry<String, InternalJMSDestination> spiEntry : destinations.entrySet()) {
            result.put(spiEntry.getKey(), convert(spiEntry.getValue()));
        }
        return result;
    }

    public JMSDestination convert(InternalJMSDestination internalJmsDestination) {
        JMSDestination result = new JMSDestination();
        result.setType(internalJmsDestination.getType());
        result.setName(internalJmsDestination.getName());
        result.setNumberOfMessages(internalJmsDestination.getNumberOfMessages());
        result.setInternal(internalJmsDestination.isInternal());
        result.setProperties(internalJmsDestination.getProperties());
        return result;
    }
}
