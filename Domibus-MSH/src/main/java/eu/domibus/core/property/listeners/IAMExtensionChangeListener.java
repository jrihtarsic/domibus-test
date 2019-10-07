package eu.domibus.core.property.listeners;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyChangeListener;
import eu.domibus.core.crypto.DomainCryptoServiceImpl;
import eu.domibus.core.crypto.MultiDomainCryptoServiceImpl;
import eu.domibus.core.payload.persistence.filesystem.PayloadFileStorage;
import eu.domibus.core.payload.persistence.filesystem.PayloadFileStorageProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static eu.domibus.api.property.DomibusPropertyMetadataManager.DOMIBUS_ATTACHMENT_STORAGE_LOCATION;
import static eu.domibus.api.property.DomibusPropertyMetadataManager.DOMIBUS_EXTENSION_IAM_AUTHENTICATION_IDENTIFIER;

/**
 * @author Ion Perpegel
 * @since 4.1.1
 * <p>
 * Handles the change of attachment storage location property
 */
@Service
public class IAMExtensionChangeListener implements DomibusPropertyChangeListener {

    @Autowired
    protected DomainService domainService;

    @Autowired
    MultiDomainCryptoServiceImpl multiDomainCryptoService;

    @Override
    public boolean handlesProperty(String propertyName) {
        return StringUtils.equalsIgnoreCase(propertyName, DOMIBUS_EXTENSION_IAM_AUTHENTICATION_IDENTIFIER);
    }

    @Override
    public void propertyValueChanged(String domainCode, String propertyName, String propertyValue) {
        final Domain domain = domainService.getDomain(domainCode);

        multiDomainCryptoService.reset(domain);
    }
}
