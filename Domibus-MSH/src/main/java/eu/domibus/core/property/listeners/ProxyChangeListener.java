package eu.domibus.core.property.listeners;

import eu.domibus.plugin.property.PluginPropertyChangeListener;
import eu.domibus.proxy.DomibusProxyService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author Ion Perpegel
 * @since 4.1.1
 *
 * Handles the change of proxy property
 */
@Service
public class ProxyChangeListener implements PluginPropertyChangeListener {

    @Autowired
    @Qualifier("domibusProxyService")
    protected DomibusProxyService domibusProxyService;

    @Override
    public boolean handlesProperty(String propertyName) {
        return StringUtils.startsWith(propertyName, "domibus.proxy.");
    }

    @Override
    public void propertyValueChanged(String domainCode, String propertyName, String propertyValue) {
        domibusProxyService.resetProxy();
    }

}