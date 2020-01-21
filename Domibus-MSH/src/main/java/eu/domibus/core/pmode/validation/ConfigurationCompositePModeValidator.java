package eu.domibus.core.pmode.validation;

import eu.domibus.api.property.DomibusPropertyProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Set;

@Component
public class ConfigurationCompositePModeValidator extends CompositePModeValidator {

    @Autowired
    DomibusPropertyProvider domibusPropertyProvider;

    @PostConstruct
    public void Init() {
        Set<String> propNames = domibusPropertyProvider.getPropertyNames(s -> s.startsWith("domibus.pMode.xPathValidator."));
        propNames.forEach(propName -> {
            String propVal = domibusPropertyProvider.getProperty(propName);
            String targetExpression = propVal.split(";")[0];
            String acceptedValuesExpression = propVal.split(";")[1];
            this.getValidators().add(new XPathPModeValidator(targetExpression, acceptedValuesExpression, "Party [%s] not found in business process parties."));
        });
    }
}
