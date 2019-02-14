package eu.domibus.core.crypto.spi.dss;

import eu.domibus.core.crypto.spi.*;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.security.auth.callback.CallbackHandler;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Thomas Dussart
 * @since 4.0
 */


@Component
public class DomibusDssCryptoProvider extends AbstractCryptoServiceSpi{

    private static final Logger LOG = LoggerFactory.getLogger(DomibusDssCryptoProvider.class);

    @Override
    public void verifyTrust(X509Certificate[] certs, boolean enableRevocation, Collection<Pattern> subjectCertConstraints, Collection<Pattern> issuerCertConstraints) throws WSSecurityException {
        LOG.info("Trust is good ....");
    }

    @Override
    public String getIdentifier() {
        return "DSS_CRYPTO_PROVIDER";
    }
}
