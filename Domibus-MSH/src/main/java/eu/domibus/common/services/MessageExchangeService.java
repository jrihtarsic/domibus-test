package eu.domibus.common.services;

import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.services.impl.PullContext;
import eu.domibus.ebms3.common.context.MessageExchangeContext;
import eu.domibus.ebms3.common.model.UserMessage;

/**
 * Created by dussath on 5/19/17.
 * Service returning information about the message exchange.
 */

public interface MessageExchangeService {

    /**
     * This method with analyse the messageExchange in order to find if the message should be pushed of pulled.
     * The status will be set in messsageExchangeContext.
     * @param messageExchangeContext the context of the messae.
     */
    void upgradeMessageExchangeStatus(final MessageExchangeContext messageExchangeContext);

    /**
     * Load pmode and find pull process in order to initialize pull request.
     */
    void initiatePullRequest();

    UserMessage retrieveUserReadyToPullMessages(String mpc, Party responder);

    /**
     * When a pull request comes in, there is very litle information.  From this information we retrieve
     * the initiator, the responder and the pull process leg configuration from wich we can retrieve security information
     * @param mpcQualifiedName the mpc attribute within the pull request.
     * @return a pullcontext with all the information needed to continue with the pull process.
     */
    PullContext extractProcessOnMpc(String mpcQualifiedName);
}
