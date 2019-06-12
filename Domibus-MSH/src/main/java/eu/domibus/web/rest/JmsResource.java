package eu.domibus.web.rest;

import eu.domibus.api.csv.CsvException;
import eu.domibus.api.jms.JMSDestination;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.jms.JmsMessage;
import eu.domibus.core.csv.CsvCustomColumns;
import eu.domibus.core.csv.CsvExcludedItems;
import eu.domibus.core.csv.CsvService;
import eu.domibus.core.csv.CsvServiceImpl;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.web.rest.error.ErrorHandlerService;
import eu.domibus.web.rest.ro.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping(value = "/rest/jms")
@Validated
public class JmsResource {

    private static final DomibusLogger LOGGER = DomibusLoggerFactory.getLogger(JmsResource.class);

    private static final String APPLICATION_JSON = "application/json";

    @Autowired
    protected JMSManager jmsManager;

    @Autowired
    protected CsvServiceImpl csvServiceImpl;

    @RequestMapping(value = {"/destinations"}, method = GET)
    public ResponseEntity<DestinationsResponseRO> destinations() {

        final DestinationsResponseRO destinationsResponseRO = new DestinationsResponseRO();
        try {
            destinationsResponseRO.setJmsDestinations(jmsManager.getDestinations());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(APPLICATION_JSON))
                    .body(destinationsResponseRO);

        } catch (RuntimeException runEx) {
            LOGGER.error("Error finding the JMS messages sources", runEx);
            return ResponseEntity.badRequest()
                    .contentType(MediaType.parseMediaType(APPLICATION_JSON))
                    .body(destinationsResponseRO);
        }
    }

    @RequestMapping(value = {"/messages"}, method = POST)
    public ResponseEntity<MessagesResponseRO> messages(@RequestBody @Valid JmsFilterRequestRO request) {

        final MessagesResponseRO messagesResponseRO = new MessagesResponseRO();
        try {
            messagesResponseRO.setMessages(jmsManager.browseMessages(request.getSource(), request.getJmsType(), request.getFromDate(), request.getToDate(), request.getSelector()));
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(APPLICATION_JSON))
                    .body(messagesResponseRO);

        } catch (RuntimeException runEx) {
            LOGGER.error("Error browsing messages for source [" + request.getSource() + "]", runEx);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.parseMediaType(APPLICATION_JSON))
                    .body(messagesResponseRO);
        }
    }

    @RequestMapping(value = {"/messages/action"}, method = POST)
    public ResponseEntity<MessagesActionResponseRO> action(@RequestBody @Valid MessagesActionRequestRO request) {

        final MessagesActionResponseRO response = new MessagesActionResponseRO();
        response.setOutcome("Success");

        try {
            List<String> messageIds = request.getSelectedMessages();
            String[] ids = messageIds.toArray(new String[0]);

            if (request.getAction() == MessagesActionRequestRO.Action.MOVE) {
                Map<String, JMSDestination> destinations = jmsManager.getDestinations();
                String destName = request.getDestination();
                if (!destinations.values().stream().anyMatch(dest -> StringUtils.equals(destName, dest.getName()))) {
                    throw new IllegalArgumentException("Cannot find destination with the name [" + destName + "].");
                }
                jmsManager.moveMessages(request.getSource(), request.getDestination(), ids);
            } else if (request.getAction() == MessagesActionRequestRO.Action.REMOVE) {
                jmsManager.deleteMessages(request.getSource(), ids);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(APPLICATION_JSON))
                    .body(response);

        } catch (RuntimeException runEx) {
            LOGGER.error("Error performing action [" + request.getAction() + "]", runEx);
            response.setOutcome(runEx.getMessage());
            return ResponseEntity.badRequest()
                    .contentType(MediaType.parseMediaType(APPLICATION_JSON))
                    .body(response);
        }
    }

    /**
     * This method returns a CSV file with the contents of JMS Messages table
     *
     * @return CSV file with the contents of JMS Messages table
     */
    @RequestMapping(path = "/csv", method = RequestMethod.GET)
    public ResponseEntity<String> getCsv(@Valid JmsFilterRequestRO request) {
        String resultText;

        // get list of messages
        final List<JmsMessage> jmsMessageList = jmsManager.browseMessages(
                request.getSource(),
                request.getJmsType(),
                request.getFromDate(),
                request.getToDate(),
                request.getSelector())
                .stream().sorted(Comparator.comparing(JmsMessage::getTimestamp).reversed())
                .collect(Collectors.toList());

        customizeJMSProperties(jmsMessageList);

        try {
            resultText = csvServiceImpl.exportToCSV(jmsMessageList, JmsMessage.class,
                    CsvCustomColumns.JMS_RESOURCE.getCustomColumns(), CsvExcludedItems.JMS_RESOURCE.getExcludedItems());
        } catch (CsvException e) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(CsvService.APPLICATION_EXCEL_STR))
                .header("Content-Disposition", "attachment; filename=" + csvServiceImpl.getCsvFilename("jmsmonitoring"))
                .body(resultText);
    }

    private void customizeJMSProperties(List<JmsMessage> jmsMessageList) {
        for (JmsMessage message : jmsMessageList) {
            message.setCustomProperties(message.getCustomProperties());
            message.setProperties(message.getJMSProperties());
        }
    }


}
