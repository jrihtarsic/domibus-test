package eu.domibus.core.crypto;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.core.crypto.api.MultiDomainCryptoService;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@RunWith(JMockit.class)
public class ReloadTruststoreCommandTaskTest {

    @Tested
    ReloadTruststoreCommandTask reloadTruststoreCommandTask;

    @Injectable
    protected MultiDomainCryptoService multiDomainCryptoService;

    @Injectable
    protected DomainContextProvider domainContextProvider;

    @Test
    public void canHandle() {
    }

    @Test
    public void execute(@Injectable Map<String, String> properties,
                        @Injectable Domain domain) {
        new Expectations() {{
            domainContextProvider.getCurrentDomain();
            result = domain;
        }};

        reloadTruststoreCommandTask.execute(properties);

        new Verifications() {{
            multiDomainCryptoService.refreshTrustStore(domain);
        }};
    }

}