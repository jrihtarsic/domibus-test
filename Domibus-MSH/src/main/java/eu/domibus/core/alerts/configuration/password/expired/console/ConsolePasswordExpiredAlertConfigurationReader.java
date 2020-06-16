package eu.domibus.core.alerts.configuration.password.expired.console;

import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.core.alerts.configuration.password.PasswordExpirationAlertConfigurationReader;
import eu.domibus.core.alerts.model.common.AlertType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Reader of console user password alert configuration
 *
 * @author Ion Perpegel
 * @since 4.2
 */
@Service
public class ConsolePasswordExpiredAlertConfigurationReader extends PasswordExpirationAlertConfigurationReader {

    @Autowired
    private DomibusConfigurationService domibusConfigurationService;

    @Override
    public AlertType getAlertType() {
        return AlertType.PASSWORD_EXPIRED;
    }

    @Override
    public boolean shouldCheckExtAuthEnabled() {
        return domibusConfigurationService.isExtAuthProviderEnabled();
    }
}
