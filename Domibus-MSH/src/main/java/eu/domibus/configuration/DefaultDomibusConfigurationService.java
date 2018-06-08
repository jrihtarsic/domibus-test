package eu.domibus.configuration;

import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@Component
public class DefaultDomibusConfigurationService implements DomibusConfigurationService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DefaultDomibusConfigurationService.class);


    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Override
    public String getConfigLocation() {
        return System.getProperty(DOMIBUS_CONFIG_LOCATION);
    }

    //TODO add caching
    @Override
    public boolean isMultiTenantAware() {
        return StringUtils.isNotBlank(domibusPropertyProvider.getProperty(DomainService.GENERAL_SCHEMA_PROPERTY));
    }

    @Override
    public boolean useProxy() {
        String useProxy = domibusPropertyProvider.getProperty(DomibusConfigurationService.DOMIBUS_PROXY_ENABLED, "false");
        if (StringUtils.isEmpty(useProxy)) {
            LOG.debug("Proxy not required. The property domibus.proxy.enabled is not configured");
            return false;
        }

        String httpProxyHost = domibusPropertyProvider.getProperty(DomibusConfigurationService.DOMIBUS_PROXY_HTTP_HOST);
        String httpProxyPort = domibusPropertyProvider.getProperty(DomibusConfigurationService.DOMIBUS_PROXY_HTTP_PORT);
        String httpProxyUser = domibusPropertyProvider.getProperty(DomibusConfigurationService.DOMIBUS_PROXY_USER);
        String httpProxyPassword = domibusPropertyProvider.getProperty(DomibusConfigurationService.DOMIBUS_PROXY_PASSWORD);

        if (StringUtils.isEmpty(httpProxyHost) || StringUtils.isEmpty(httpProxyPort)
                || StringUtils.isEmpty(httpProxyUser) || StringUtils.isEmpty(httpProxyPassword)) {

            LOG.error("Proxy is enabled but the configuration is invalid:" + httpProxyHost + " " + httpProxyPort + " " +
                    httpProxyUser);

            throw new DomibusCoreException(DomibusCoreErrorCode.DOM_006, "Proxy is enabled but the configuration is invalid.");
        }
        LOG.info("Proxy configured: " + httpProxyHost + " " + httpProxyPort + " " +
                httpProxyUser);

        return Boolean.parseBoolean(useProxy);
    }

}
