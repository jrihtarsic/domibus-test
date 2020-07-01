package eu.domibus.plugin.jms.property;

import eu.domibus.ext.domain.DomibusPropertyMetadataDTO;
import eu.domibus.ext.domain.Module;
import eu.domibus.ext.services.DomibusPropertyExtServiceDelegateAbstract;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static eu.domibus.plugin.jms.JMSMessageConstants.*;

/**
 * @author Ion Perpegel
 * @since 4.1.1
 * <p>
 * Property manager for the JmsPlugin properties.
 */
@Service
public class JmsPluginPropertyManager extends DomibusPropertyExtServiceDelegateAbstract {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(JmsPluginPropertyManager.class);

    private List<DomibusPropertyMetadataDTO> readOnlyGlobalProperties = Arrays.asList(
            new DomibusPropertyMetadataDTO(JMS_PLUGIN_PROPERTY_PREFIX + "." + CONNECTION_FACTORY, Module.JMS_PLUGIN, false, DomibusPropertyMetadataDTO.Usage.GLOBAL, true, false, false, false),
            new DomibusPropertyMetadataDTO(JMS_PLUGIN_PROPERTY_PREFIX + "." + QUEUE_NOTIFICATION, DomibusPropertyMetadataDTO.Type.QUEUE, Module.JMS_PLUGIN, false, DomibusPropertyMetadataDTO.Usage.GLOBAL, false, false, false, false),
            new DomibusPropertyMetadataDTO(JMS_PLUGIN_PROPERTY_PREFIX + "." + QUEUE_IN, DomibusPropertyMetadataDTO.Type.QUEUE, Module.JMS_PLUGIN, false, DomibusPropertyMetadataDTO.Usage.GLOBAL, false, false, false, false),
            new DomibusPropertyMetadataDTO(JMS_PLUGIN_PROPERTY_PREFIX + "." + QUEUE_IN_CONCURRENCY, DomibusPropertyMetadataDTO.Type.CONCURRENCY, Module.JMS_PLUGIN, false, DomibusPropertyMetadataDTO.Usage.GLOBAL, false, false, false, false)
            );


    private List<DomibusPropertyMetadataDTO> readOnlyDomainProperties = Arrays.stream(new String[]{
            JMSPLUGIN_QUEUE_OUT,
            JMSPLUGIN_QUEUE_REPLY,
            JMSPLUGIN_QUEUE_CONSUMER_NOTIFICATION_ERROR,
            JMSPLUGIN_QUEUE_PRODUCER_NOTIFICATION_ERROR
    })
            .map(name -> new DomibusPropertyMetadataDTO(name, Module.JMS_PLUGIN, false, DomibusPropertyMetadataDTO.Usage.DOMAIN, true, false, false, false))
            .collect(Collectors.toList());

    private List<DomibusPropertyMetadataDTO> readOnlyComposableDomainProperties = Arrays.stream(new String[]{
            JMSPLUGIN_QUEUE_OUT_ROUTING,
            JMSPLUGIN_QUEUE_REPLY_ROUTING,
            JMSPLUGIN_QUEUE_CONSUMER_NOTIFICATION_ERROR_ROUTING,
            JMSPLUGIN_QUEUE_PRODUCER_NOTIFICATION_ERROR_ROUTING
    })
            .map(name -> new DomibusPropertyMetadataDTO(name, Module.JMS_PLUGIN, false, DomibusPropertyMetadataDTO.Usage.DOMAIN, false, false, false, true))
            .collect(Collectors.toList());


    private List<DomibusPropertyMetadataDTO> writableProperties = Arrays.asList(
            new DomibusPropertyMetadataDTO(JMS_PLUGIN_PROPERTY_PREFIX + "." + FROM_PARTY_ID, Module.JMS_PLUGIN, DomibusPropertyMetadataDTO.Usage.DOMAIN, true),
            new DomibusPropertyMetadataDTO(JMS_PLUGIN_PROPERTY_PREFIX + "." + FROM_PARTY_TYPE, DomibusPropertyMetadataDTO.Type.URI, Module.JMS_PLUGIN, DomibusPropertyMetadataDTO.Usage.DOMAIN, true),
            new DomibusPropertyMetadataDTO(JMS_PLUGIN_PROPERTY_PREFIX + "." + FROM_ROLE, Module.JMS_PLUGIN, DomibusPropertyMetadataDTO.Usage.DOMAIN, true),
            new DomibusPropertyMetadataDTO(JMS_PLUGIN_PROPERTY_PREFIX + "." + TO_PARTY_ID, Module.JMS_PLUGIN, DomibusPropertyMetadataDTO.Usage.DOMAIN, true),
            new DomibusPropertyMetadataDTO(JMS_PLUGIN_PROPERTY_PREFIX + "." + TO_PARTY_TYPE, DomibusPropertyMetadataDTO.Type.URI, Module.JMS_PLUGIN, DomibusPropertyMetadataDTO.Usage.DOMAIN, true),
            new DomibusPropertyMetadataDTO(JMS_PLUGIN_PROPERTY_PREFIX + "." + TO_ROLE, Module.JMS_PLUGIN, DomibusPropertyMetadataDTO.Usage.DOMAIN, true),
            new DomibusPropertyMetadataDTO(JMS_PLUGIN_PROPERTY_PREFIX + "." + AGREEMENT_REF, Module.JMS_PLUGIN, DomibusPropertyMetadataDTO.Usage.DOMAIN, true),
            new DomibusPropertyMetadataDTO(JMS_PLUGIN_PROPERTY_PREFIX + "." + SERVICE, Module.JMS_PLUGIN, DomibusPropertyMetadataDTO.Usage.DOMAIN, true),
            new DomibusPropertyMetadataDTO(JMS_PLUGIN_PROPERTY_PREFIX + "." + SERVICE_TYPE, Module.JMS_PLUGIN, DomibusPropertyMetadataDTO.Usage.DOMAIN, true),
            new DomibusPropertyMetadataDTO(JMS_PLUGIN_PROPERTY_PREFIX + "." + ACTION, Module.JMS_PLUGIN, DomibusPropertyMetadataDTO.Usage.DOMAIN, true),
            new DomibusPropertyMetadataDTO(JMS_PLUGIN_PROPERTY_PREFIX + "." + PUT_ATTACHMENTS_IN_QUEUE, DomibusPropertyMetadataDTO.Type.BOOLEAN, Module.JMS_PLUGIN, DomibusPropertyMetadataDTO.Usage.DOMAIN, true)
    );

    @Override
    public Map<String, DomibusPropertyMetadataDTO> getKnownProperties() {
        List<DomibusPropertyMetadataDTO> allProperties = new ArrayList<>();
        allProperties.addAll(readOnlyGlobalProperties);
        allProperties.addAll(readOnlyDomainProperties);
        allProperties.addAll(readOnlyComposableDomainProperties);
        allProperties.addAll(writableProperties);

        return allProperties.stream().collect(Collectors.toMap(x -> x.getName(), x -> x));
    }
}
