package eu.domibus.core.alerts.model.service;

import eu.domibus.core.alerts.model.common.AlertLevel;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.logging.DomibusLoggerFactory;
import org.slf4j.Logger;

/**
 * @author Ion Perpegel
 * @since 4.1
 */
public class RepetitiveAlertModuleConfiguration extends AlertModuleConfigurationBase {

    private static final Logger LOG = DomibusLoggerFactory.getLogger(RepetitiveAlertModuleConfiguration.class);

    private Integer eventDelay;
    private Integer eventFrequency;

    public RepetitiveAlertModuleConfiguration(AlertType alertType) {
        super(alertType);
    }

    public RepetitiveAlertModuleConfiguration(AlertType alertType, Integer eventDelay, Integer eventFrequency, AlertLevel eventAlertLevel, String eventMailSubject) {
        super(alertType, eventAlertLevel, eventMailSubject);

        this.eventDelay = eventDelay;
        this.eventFrequency = eventFrequency;
    }

    public Integer getEventDelay() {
        return eventDelay;
    }

    public Integer getEventFrequency() {
        return eventFrequency;
    }

}
