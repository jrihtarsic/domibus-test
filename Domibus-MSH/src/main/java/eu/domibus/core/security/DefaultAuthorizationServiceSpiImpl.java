package eu.domibus.core.security;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.RegexUtil;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.services.MessageExchangeService;
import eu.domibus.common.services.impl.PullContext;
import eu.domibus.core.crypto.spi.AuthorizationServiceSpi;
import eu.domibus.core.crypto.spi.PullRequestPmodeData;
import eu.domibus.core.crypto.spi.model.AuthorizationError;
import eu.domibus.core.crypto.spi.model.AuthorizationException;
import eu.domibus.core.crypto.spi.model.UserMessagePmodeData;
import eu.domibus.core.pmode.PModeProvider;
import eu.domibus.ext.domain.PullRequestDTO;
import eu.domibus.ext.domain.UserMessageDTO;
import eu.domibus.pki.CertificateService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

/**
 * @author Thomas Dussart, Ioana Dragusanu
 * @since 4.1
 * <p>
 * Default authorization implementation.
 */
@Component
public class DefaultAuthorizationServiceSpiImpl implements AuthorizationServiceSpi {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAuthorizationServiceSpiImpl.class);

    protected static final String DEFAULT_IAM_AUTHORIZATION_IDENTIFIER = "DEFAULT_IAM_AUTHORIZATION_SPI";

    protected static final String DOMIBUS_SENDER_CERTIFICATE_SUBJECT_CHECK = "domibus.sender.certificate.subject.check";

    private static final String DOMIBUS_SENDER_TRUST_VALIDATION_EXPRESSION = "domibus.sender.trust.validation.expression";

    @Autowired
    private CertificateService certificateService;

    @Autowired
    MessageExchangeService messageExchangeService;

    @Autowired
    PModeProvider pModeProvider;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    RegexUtil regexUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public void authorize(List<X509Certificate> signingCertificateTrustChain, X509Certificate signingCertificate, UserMessageDTO userMessageDTO, UserMessagePmodeData userMessagePmodeData) throws AuthorizationException {
        doAuthorize(signingCertificate, userMessagePmodeData.getPartyName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void authorize(List<X509Certificate> signingCertificateTrustChain, X509Certificate signingCertificate, PullRequestDTO pullRequestDTO, PullRequestPmodeData pullRequestPmodeData) throws AuthorizationException {
        String mpc = pullRequestPmodeData.getMpcName();
        if (mpc == null) {
            LOG.error("Mpc is null, cannot authorize against a null mpc");
            throw new AuthorizationException(AuthorizationError.AUTHORIZATION_OTHER, "Mpc is null, cannot authorize against a null mpc");
        }

        String mpcQualified;
        try {
            mpcQualified = pModeProvider.findMpcUri(mpc);
        } catch (EbMS3Exception e) {
            LOG.error("Could not find mpc [{}]", mpc);
            throw new AuthorizationException(AuthorizationError.AUTHORIZATION_OTHER, "Mpc is null, cannot authorize against a null mpc");
        }

        PullContext pullContext = messageExchangeService.extractProcessOnMpc(mpcQualified);
        if (pullContext == null || pullContext.getProcess() == null) {
            LOG.warn("Pull context could not be extracted");
        }

        if (CollectionUtils.isEmpty(pullContext.getProcess().getInitiatorParties()) ||
                pullContext.getProcess().getInitiatorParties().size() > 1) {
            LOG.error("Default authorization of Pull Request requires one initiator per pull process");
            throw new AuthorizationException(AuthorizationError.AUTHORIZATION_REJECTED, "Default authorization of Pull Request requires one initiator per pull process");
        }
        Optional<Party> initiatorOptional = pullContext.getProcess().getInitiatorParties().stream().findFirst();

        String initiatorName = initiatorOptional.isPresent() ? initiatorOptional.get().getName() : null;

        doAuthorize(signingCertificate, initiatorName);
    }

    protected void doAuthorize(X509Certificate signingCertificate, String initiatorName) {
        authorizeAgainstTruststoreAlias(signingCertificate, initiatorName);
        authorizeAgainstCertificateSubjectExpression(signingCertificate);
        authorizeAgainstCertificateCNMatch(signingCertificate, initiatorName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentifier() {
        return DEFAULT_IAM_AUTHORIZATION_IDENTIFIER;
    }

    protected void authorizeAgainstTruststoreAlias(X509Certificate signingCertificate, String alias) {
        LOG.debug("Authorize against certificate extracted based on the alias [{}] from the truststore", alias);
        if (signingCertificate == null) {
            LOG.warn("Signing certificate is not provided.");
            return;
        }
        LOG.debug("Signing certificate: [{}]", signingCertificate.toString());
        try {
            X509Certificate cert = certificateService.getPartyX509CertificateFromTruststore(alias);
            if (cert == null) {
                LOG.warn("Failed to get the certificate based on the partyName [{}]. No further authorization against truststore is performed.", alias);
                return;
            }
            LOG.debug("Truststore certificate: [{}]", cert.toString());

            if (!signingCertificate.equals(cert)) {
                LOG.info("Signing certificate: [{}]", signingCertificate.toString());
                LOG.info("Truststore certificate: [{}]", cert.toString());
                LOG.error("Signing certificate and truststore certificate do not match.");
                throw new AuthorizationException(AuthorizationError.AUTHORIZATION_REJECTED, "Signing certificate and truststore certificate do not match.");
            }
        } catch (KeyStoreException e) {
            LOG.error("Failed to get certificate from truststore", e);
            throw new DomibusCoreException(DomibusCoreErrorCode.DOM_005, "Failed to get certificate from truststore", e);
        }
    }

    protected void authorizeAgainstCertificateSubjectExpression(X509Certificate signingCertificate) {
        String subject = signingCertificate.getSubjectDN().getName();
        String certSubjectExpression = domibusPropertyProvider.getDomainProperty(DOMIBUS_SENDER_TRUST_VALIDATION_EXPRESSION);
        if (StringUtils.isEmpty(certSubjectExpression)) {
            LOG.debug("[{}] is empty, verification is disabled.", DOMIBUS_SENDER_TRUST_VALIDATION_EXPRESSION);
            return;
        }
        LOG.debug("Property [{}], value [{}]", DOMIBUS_SENDER_TRUST_VALIDATION_EXPRESSION, certSubjectExpression);
        if (!regexUtil.matches(certSubjectExpression, subject)) {
            LOG.error("Certificate subject [{}] does not match the regullar expression configured [{}]", subject, certSubjectExpression);
            throw new AuthorizationException(AuthorizationError.AUTHORIZATION_REJECTED, "Certificate subject " + subject + " does not match the regullar expression configured " + certSubjectExpression);
        }
    }

    protected void authorizeAgainstCertificateCNMatch(X509Certificate signingCertificate, String alias) {
        if (!domibusPropertyProvider.getBooleanDomainProperty(DOMIBUS_SENDER_CERTIFICATE_SUBJECT_CHECK)) {
            LOG.debug("Sender alias verification is disabled");
            return;
        }

        LOG.info("Verifying sender trust");
        if (signingCertificate == null) {
            LOG.debug("SigningCertificate is null, sender alias verification is disabled");
            return;
        }

        if (StringUtils.containsIgnoreCase(signingCertificate.getSubjectDN().getName(), alias)) {
            LOG.info("Sender [" + alias + "] is trusted.");
            return;
        }
        throw new AuthorizationException(AuthorizationError.AUTHORIZATION_REJECTED, "Sender alias verification failed. " +
                "Signing certificate CN does not contain the alias " + signingCertificate.getSubjectDN().getName() + alias);
    }
}
