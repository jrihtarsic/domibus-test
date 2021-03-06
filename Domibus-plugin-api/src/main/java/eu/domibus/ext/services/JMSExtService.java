package eu.domibus.ext.services;

import eu.domibus.ext.domain.JmsMessageDTO;

import javax.jms.Queue;

/**
 * Responsible for JMS operations like sending messages to queues or topics
 *
 * @author Cosmin Baciu
 * @since 4.0
 */
public interface JMSExtService {

    /**
     * Sends a message to a specific queue
     *
     * @param message The message to be sent
     * @param destination The JMS destination
     */
    void sendMessageToQueue(JmsMessageDTO message, String destination);

    /**
     * Sends a message to a specific queue
     *
     * @param message The message to be sent
     * @param destination The JMS destination
     */
    void sendMessageToQueue(JmsMessageDTO message, Queue destination);

    /**
     * Sends a Map message to a specific queue
     *
     * @param message The message to be sent
     * @param destination The JMS destination
     */
    void sendMapMessageToQueue(JmsMessageDTO message, String destination);

    /**
     * Sends a Map message to a specific queue
     *
     * @param message The message to be sent
     * @param destination The JMS destination
     */
    void sendMapMessageToQueue(JmsMessageDTO message, Queue destination);

}
