package eu.domibus.jms.weblogic;

import eu.domibus.api.jms.JMSDestinationHelper;
import eu.domibus.jms.spi.JMSDestinationSPI;
import eu.domibus.jms.spi.JMSManagerSPI;
import eu.domibus.jms.spi.JmsMessageSPI;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsOperations;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import weblogic.messaging.runtime.MessageInfo;

import javax.annotation.Resource;
import javax.jms.Destination;
import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Created by Cosmin Baciu on 17-Aug-16.
 */
@Component
public class JMSManagerWeblogic implements JMSManagerSPI {

    private static final Log LOG = LogFactory.getLog(JMSManagerWeblogic.class);

    private static final String PROPERTY_OBJECT_NAME = "ObjectName";
    private static final String PROPERTY_JNDI_NAME = "Jndi";

    protected Map<String, ObjectName> queueMap;
    protected Map<String, ObjectName> topicMap;

    @Autowired
    JMXHelper jmxHelper;

    @Resource(name = "jmsSender")
    private JmsOperations jmsOperations;

    @Autowired
    JMSDestinationHelper jmsDestinationHelper;

    @Override
    public Map<String, JMSDestinationSPI> getDestinations() {
        Map<String, JMSDestinationSPI> destinationMap = new TreeMap<String, JMSDestinationSPI>();
        try {
            MBeanServerConnection mbsc = jmxHelper.getDomainRuntimeMBeanServerConnection();
            ObjectName drs = jmxHelper.getDomainRuntimeService();
            ObjectName config = (ObjectName) mbsc.getAttribute(drs, "DomainConfiguration");
            ObjectName[] servers = (ObjectName[]) mbsc.getAttribute(drs, "ServerRuntimes");
            for (ObjectName server : servers) {
                LOG.debug("Server " + server);
                String serverAddress = (String) mbsc.getAttribute(server, "ListenAddress");
                if (serverAddress.contains("/")) {
                    serverAddress = serverAddress.substring(0, serverAddress.indexOf("/"));
                }
                Integer serverPort = (Integer) mbsc.getAttribute(server, "ListenPort");
                ObjectName jms = (ObjectName) mbsc.getAttribute(server, "JMSRuntime");
                ObjectName[] jmsServers = (ObjectName[]) mbsc.getAttribute(jms, "JMSServers");
                for (ObjectName jmsServer : jmsServers) {
                    LOG.debug("JMS Server " + jmsServer);
                    ObjectName[] jmsDestinations = (ObjectName[]) mbsc.getAttribute(jmsServer, "Destinations");
                    for (ObjectName jmsDestination : jmsDestinations) {
                        LOG.debug("JMS Destination " + jmsDestination);
                        JMSDestinationSPI destination = new JMSDestinationSPI();
                        String destinationFQName = (String) mbsc.getAttribute(jmsDestination, "Name");
                        String moduleName = destinationFQName.substring(0, destinationFQName.indexOf("!"));
                        String destinationName = destinationFQName.substring(destinationFQName.indexOf("!") + 1);
                        if (destinationName.contains(".")) {
                            String jmsServerName = destinationName.substring(0, destinationName.lastIndexOf("."));
                            destinationName = destinationName.substring(destinationName.lastIndexOf(".") + 1);
                        }
                        if (destinationName.contains("@")) {
                            String jmsServerName = destinationName.substring(0, destinationName.lastIndexOf("@"));
                            destinationName = destinationName.substring(destinationName.lastIndexOf("@") + 1);
                        }
                        destination.setName(destinationName);

//                        destination.setServerAddress(serverAddress);
//                        destination.setServerPort(serverPort);
                        ObjectName configQueue = getQueueMap(mbsc).get(destinationName);
                        if (configQueue != null) {
                            destination.setType("Queue");
                            destination.setProperty(PROPERTY_OBJECT_NAME, jmsDestination);
//                            destination.setObjectName(jmsDestination);
                            String configQueueJndiName = (String) mbsc.getAttribute(configQueue, "JNDIName");
//                            destination.setJndiName(configQueueJndiName);
                            destination.setProperty(PROPERTY_JNDI_NAME, configQueueJndiName);
                            destination.setInternal(jmsDestinationHelper.isInternal(configQueueJndiName));
                        }
                        ObjectName configTopic = getTopicMap(mbsc).get(destinationName);
                        if (configTopic != null) {
                            destination.setProperty(PROPERTY_OBJECT_NAME, jmsDestination);
                            destination.setType("Topic");
                            String configTopicJndiName = (String) mbsc.getAttribute(configTopic, "JNDIName");
//                            destination.setJndiName(configTopicJndiName);
                            destination.setProperty(PROPERTY_JNDI_NAME, configTopicJndiName);
                            ObjectName[] jmsDurableSubscribers = (ObjectName[]) mbsc.getAttribute(jmsDestination, "DurableSubscribers");
                            for (ObjectName jmsDurableSubscriber : jmsDurableSubscribers) {
                                JMSDestinationSPI subscriberDestination = new JMSDestinationSPI();
                                subscriberDestination.setProperty(PROPERTY_OBJECT_NAME, jmsDurableSubscriber);
//                                subscriberDestination.setObjectName(jmsDurableSubscriber);
                                String subscriptionName = (String) mbsc.getAttribute(jmsDurableSubscriber, "SubscriptionName");
                                subscriberDestination.setName(destination.getName() + "_" + subscriptionName);
                                subscriberDestination.setType("Queue");
                                subscriberDestination.setProperty(PROPERTY_JNDI_NAME, null);
//                                subscriberDestination.setJndiName(null);
                                destinationMap.put(subscriberDestination.getName(), subscriberDestination);
                            }
                        }
                        Long numberOfMessages = (Long) mbsc.getAttribute(jmsDestination, "MessagesCurrentCount");
//                        Long numberOfMessagesPending = (Long) mbsc.getAttribute(jmsDestination, "MessagesPendingCount");

                        destination.setNumberOfMessages(numberOfMessages);
//                        destination.setNumberOfMessagesPending(numberOfMessagesPending);
                        destinationMap.put(destination.getName(), destination);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to build JMS destination map", e);
        }
        return destinationMap;
    }

    protected Map<String, ObjectName> getQueueMap(MBeanServerConnection mbsc) throws IOException, AttributeNotFoundException, InstanceNotFoundException, MBeanException,
            ReflectionException {
        if (queueMap != null) {
            return queueMap;
        }
        ObjectName drs = jmxHelper.getDomainRuntimeService();
        ObjectName config = (ObjectName) mbsc.getAttribute(drs, "DomainConfiguration");
        queueMap = new HashMap<String, ObjectName>();
        ObjectName[] configJmsSystemResources = (ObjectName[]) mbsc.getAttribute(config, "JMSSystemResources");
        for (ObjectName configJmsSystemResource : configJmsSystemResources) {
            ObjectName configJmsResource = (ObjectName) mbsc.getAttribute(configJmsSystemResource, "JMSResource");
            ObjectName[] configQueues = (ObjectName[]) mbsc.getAttribute(configJmsResource, "Queues");
            for (ObjectName configQueue : configQueues) {
                String configQueueName = (String) mbsc.getAttribute(configQueue, "Name");
                queueMap.put(configQueueName, configQueue);
            }
            ObjectName[] configDDQueues = (ObjectName[]) mbsc.getAttribute(configJmsResource, "DistributedQueues");
            for (ObjectName configQueue : configDDQueues) {
                String configQueueName = (String) mbsc.getAttribute(configQueue, "Name");
                queueMap.put(configQueueName, configQueue);
            }
            ObjectName[] configUDDQueues = (ObjectName[]) mbsc.getAttribute(configJmsResource, "UniformDistributedQueues");
            for (ObjectName configQueue : configUDDQueues) {
                String configQueueName = (String) mbsc.getAttribute(configQueue, "Name");
                queueMap.put(configQueueName, configQueue);
            }
        }
        return queueMap;
    }

    protected Map<String, ObjectName> getTopicMap(MBeanServerConnection mbsc) throws AttributeNotFoundException, InstanceNotFoundException, MBeanException,
            ReflectionException, IOException {
        if (topicMap != null) {
            return topicMap;
        }
        topicMap = new HashMap<String, ObjectName>();
        ObjectName drs = jmxHelper.getDomainRuntimeService();
        ObjectName config = (ObjectName) mbsc.getAttribute(drs, "DomainConfiguration");
        ObjectName[] configJmsSystemResources = (ObjectName[]) mbsc.getAttribute(config, "JMSSystemResources");
        for (ObjectName configJmsSystemResource : configJmsSystemResources) {
            ObjectName configJmsResource = (ObjectName) mbsc.getAttribute(configJmsSystemResource, "JMSResource");
            ObjectName[] configTopics = (ObjectName[]) mbsc.getAttribute(configJmsResource, "Topics");
            for (ObjectName configTopic : configTopics) {
                String configTopicName = (String) mbsc.getAttribute(configTopic, "Name");
                topicMap.put(configTopicName, configTopic);
            }
        }
        return topicMap;
    }

    @Override
    public boolean sendMessage(JmsMessageSPI message, String connectionFactory, String destination, String destinationType) {
        JMSDestinationSPI jmsDestinationSPI = getDestinations().get(destination);
        if (jmsDestinationSPI == null) {
            LOG.warn("Destination [" + destination + "] does not exists");
            return false;
        }

        Destination jmsDestination = null;
        try {
            String destinationJndi = jmsDestinationSPI.getProperty(PROPERTY_JNDI_NAME);
            LOG.debug("Found JNDI [" + destinationJndi + "] for destination [" + destination + "]");
            jmsDestination = InitialContext.doLookup(destinationJndi);
        } catch (NamingException e) {
            LOG.error("Error performing lookup for [" + destination + "]");
            return false;
        }
        jmsOperations.send(jmsDestination, new JmsMessageCreator(message));
        return true;
    }

    @Override
    public boolean deleteMessages(String source, String[] messageIds) {
        JMSDestinationSPI selectedDestination = getDestinations().get(source);
        try {
            ObjectName destination = selectedDestination.getProperty(PROPERTY_OBJECT_NAME);
            int deleted = deleteMessages(destination, getSelector(messageIds));
            return deleted == messageIds.length;
        } catch (Exception e) {
            LOG.error("Failed to delete messages from source [" + source + "]:" + messageIds , e);
            return false;
        }
    }

    @Override
    public JmsMessageSPI getMessage(String source, String messageId) {
        JMSDestinationSPI selectedDestination = getDestinations().get(source);
        if (selectedDestination != null) {
            String destinationType = selectedDestination.getType();
            if ("Queue".equals(destinationType)) {
                try {
                    ObjectName destination = selectedDestination.getProperty(PROPERTY_OBJECT_NAME);
                    return getMessageFromDestination(destination, messageId);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
        return null;
    }

    private JmsMessageSPI getMessageFromDestination(ObjectName destination, String messageId) throws Exception {
        List<JmsMessageSPI> messages = getMessagesFromDestination(destination, getSelector(messageId));
        if (!messages.isEmpty()) {
            return messages.get(0);
        }
        return null;
    }

    private List<JmsMessageSPI> getMessagesFromDestination(ObjectName destination, String selector) throws Exception {
        List<JmsMessageSPI> messages = new ArrayList<JmsMessageSPI>();
        MBeanServerConnection mbsc = jmxHelper.getDomainRuntimeMBeanServerConnection();
        if (selector == null) {
            selector = "true";
        }
        Integer timeout = new Integer(0);
        Integer stateMask = new Integer( // Show messages in destination in all possible states
                MessageInfo.STATE_VISIBLE | // Visible and available for consumption
                        MessageInfo.STATE_DELAYED | // Pending delayed delivery
                        MessageInfo.STATE_PAUSED | // Pending pause operation
                        MessageInfo.STATE_RECEIVE | // Pending receive operation
                        MessageInfo.STATE_SEND | // Pending send operation
                        MessageInfo.STATE_TRANSACTION // Pending send or receive operation as part of global transaction
        );
        String messageCursor = (String) mbsc.invoke(destination, "getMessages", new Object[]{selector, timeout, stateMask},
                new String[]{String.class.getName(), Integer.class.getName(), Integer.class.getName()});
        Long totalAmountOfMessages = (Long) mbsc.invoke(destination, "getCursorSize", new Object[]{messageCursor}, new String[]{String.class.getName()});
        CompositeData[] allMessageMetaData = (CompositeData[]) mbsc.invoke(destination, "getItems", new Object[]{messageCursor, new Long(0),
                new Integer(totalAmountOfMessages.intValue())}, new String[]{String.class.getName(), Long.class.getName(), Integer.class.getName()});


        if (allMessageMetaData != null) {
            for (CompositeData messageMetaData : allMessageMetaData) {
                JmsMessageSPI message = convertMessage(messageMetaData);
                String messageId = message.getId();
                CompositeData messageDataDetails = (CompositeData) mbsc.invoke(destination, "getMessage", new Object[]{messageCursor, messageId}, new String[]{
                        String.class.getName(), String.class.getName()});
                message = convertMessage(messageDataDetails);
                messages.add(message);
            }
        }
        return messages;
    }

    @Override
    public List<JmsMessageSPI> getMessages(String source, String jmsType, Date fromDate, Date toDate, String selectorClause) {
        List<JmsMessageSPI> messages = new ArrayList<>();
        if (source == null) {
            return messages;
        }

        JMSDestinationSPI selectedDestination = getDestinations().get(source);
        if (selectedDestination != null) {
            String destinationType = selectedDestination.getType();
            if ("Queue".equals(destinationType)) {
                Map<String, Object> criteria = new HashMap<String, Object>();
                if (jmsType != null) {
                    criteria.put("JMSType", jmsType);
                }
                if (fromDate != null) {
                    criteria.put("JMSTimestamp_from", fromDate.getTime());
                }
                if (toDate != null) {
                    criteria.put("JMSTimestamp_to", toDate.getTime());
                }
                if (selectorClause != null) {
                    criteria.put("selectorClause", selectorClause);
                }
                String selector = getSelector(criteria);
                try {
                    ObjectName destination = selectedDestination.getProperty(PROPERTY_OBJECT_NAME);
                    messages = getMessagesFromDestination(destination, selector);
                } catch (Exception e) {
                    LOG.error("Error getting messages for [" + source + "] with selector [" + selector + "]", e);
                }
            }
        }
        return messages;
    }

    private int deleteMessages(ObjectName destination, String selector) throws Exception {
        MBeanServerConnection mbsc = jmxHelper.getDomainRuntimeMBeanServerConnection();
        Integer deleted = (Integer) mbsc.invoke(destination, "deleteMessages", new Object[]{selector}, new String[]{String.class.getName()});
        return deleted;
    }


    private String getSelector(String messageId) {
        StringBuffer selector = new StringBuffer("JMSMessageID = '").append(messageId).append("'");
        return selector.toString();
    }

    private String getSelector(String[] messageIds) {
        if (messageIds.length == 1) {
            return getSelector(messageIds[0]);
        }
        StringBuffer selector = new StringBuffer("JMSMessageID IN (");
        for (int i = 0; i < messageIds.length; i++) {
            String messageId = messageIds[i];
            if (i > 0) {
                selector.append(", ");
            }
            selector.append("'").append(messageId).append("'");
        }
        selector.append(")");
        return selector.toString();
    }

    public String getSelector(Map<String, Object> criteria) {
        StringBuffer selector = new StringBuffer();
        // JMSType
        String jmsType = (String) criteria.get("JMSType");
        if (!StringUtils.isEmpty(jmsType)) {
            selector.append(selector.length() > 0 ? " and " : "");
            selector.append("JMSType='").append(jmsType).append("'");
        }
        // JMSTimestamp
        Long jmsTimestampFrom = (Long) criteria.get("JMSTimestamp_from");
        if (jmsTimestampFrom != null) {
            selector.append(selector.length() > 0 ? " and " : "");
            selector.append("JMSTimestamp>=").append(jmsTimestampFrom);
        }
        Long jmsTimestampTo = (Long) criteria.get("JMSTimestamp_to");
        if (jmsTimestampTo != null) {
            selector.append(selector.length() > 0 ? " and " : "");
            selector.append("JMSTimestamp<=").append(jmsTimestampTo);
        }
        String selectorClause = (String) criteria.get("selectorClause");
        if (!StringUtils.isEmpty(selectorClause)) {
            selector.append(selector.length() > 0 ? " and " : "");
            selector.append(selectorClause);
        }
        if (selector.length() == 0) {
            selector.append("true");
        }
        return selector.toString().trim();
    }


    @Override
    public boolean moveMessages(String source, String destination, String[] messageIds) {
        JMSDestinationSPI from = getDestinations().get(source);
        JMSDestinationSPI to = getDestinations().get(destination);
        try {
            ObjectName fromDestination = from.getProperty(PROPERTY_OBJECT_NAME);
            ObjectName toDestination = to.getProperty(PROPERTY_OBJECT_NAME);
            int moved = moveMessages(fromDestination, toDestination, getSelector(messageIds));
            return moved == messageIds.length;
        } catch (Exception e) {
            LOG.error("Failed to move messages from source [" + source + "] to destination [" + destination + "]:" + messageIds , e);
            return false;
        }
    }

    private int moveMessages(ObjectName from, ObjectName to, String selector) throws Exception {
        MBeanServerConnection mbsc = jmxHelper.getDomainRuntimeMBeanServerConnection();
        CompositeData toDestinationInfo = (CompositeData) mbsc.getAttribute(to, "DestinationInfo");
        Integer moved = (Integer) mbsc.invoke(from, "moveMessages", new Object[]{selector, toDestinationInfo}, new String[]{String.class.getName(),
                CompositeData.class.getName()});
        return moved;
    }

    public JmsMessageSPI convertMessage(CompositeData messageData) throws Exception {
        JmsMessageSPI message = new JmsMessageSPI();
        String xmlMessage = String.valueOf(messageData.get("MessageXMLText"));
        Document xmlDocument = parseXML(xmlMessage);
        String ns = "http://www.bea.com/WLS/JMS/Message";
        Element root = xmlDocument.getDocumentElement();
        Element header = getChildElement(root, "Header");
        Element jmsMessageId = getChildElement(header, "JMSMessageID");
        String id = jmsMessageId.getTextContent();
        message.setId(id);
        message.getProperties().put("JMSMessageID", id);
        Element jmsTimestamp = getChildElement(header, "JMSTimestamp");
        String timestamp = jmsTimestamp.getTextContent();
        message.setTimestamp(new Date(Long.parseLong(timestamp)));
        message.getProperties().put("JMSTimestamp", timestamp);
        Element jmsType = getChildElement(header, "JMSType");
        if (jmsType != null) {
            String type = jmsType.getTextContent();
            message.setType(type);
            message.getProperties().put("JMSType", type);
        }
        Element propertiesRoot = getChildElement(header, "Properties");
        List<Element> properties = getChildElements(propertiesRoot, "property");
        for (Element property : properties) {
            String key = property.getAttribute("name");
            String value = getFirstChildElement(property).getTextContent();
            message.getProperties().put(key, value);
        }
        Element jmsBody = getChildElement(root, "Body");
        if (jmsBody != null) {
            message.setContent(jmsBody.getTextContent());
        }
        return message;
    }

    public Document parseXML(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        factory.setNamespaceAware(true);
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    public Element getChildElement(Element parent, String name) {
        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
            if (parent.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) parent.getChildNodes().item(i);
                String localName = child.getLocalName();
                if (localName == null) {
                    localName = child.getNodeName();
                    if (localName != null && localName.contains(":")) {
                        localName = localName.substring(localName.indexOf(":") + 1);
                    }
                }
                String ns = child.getNamespaceURI();
                if (localName.equals(name)) {
                    return child;
                }
            }
        }
        return null;
    }

    public List<Element> getChildElements(Element parent, String name) {
        List<Element> childElements = new ArrayList<Element>();
        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
            if (parent.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) parent.getChildNodes().item(i);
                String localName = child.getLocalName();
                if (localName == null) {
                    localName = child.getNodeName();
                    if (localName != null && localName.contains(":")) {
                        localName = localName.substring(localName.indexOf(":") + 1);
                    }
                }
                String ns = child.getNamespaceURI();
                if (localName.equals(name)) {
                    childElements.add(child);
                }
            }
        }
        return childElements;
    }

    public Element getFirstChildElement(Element parent) {
        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
            if (parent.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) parent.getChildNodes().item(i);
                return child;
            }
        }
        return null;
    }
}
