package eu.domibus.web.rest;

import eu.domibus.common.model.configuration.ConfigurationRaw;
import eu.domibus.ebms3.common.dao.PModeProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.web.rest.ro.PModeResponseRO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Mircea Musat
 * @since 3.3
 */
@RestController
@RequestMapping(value = "/rest/pmode")
public class PModeResource {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PModeResource.class);

    private PModeProvider pModeProvider;

    @Autowired
    public PModeResource(PModeProvider pModeProvider) {
        this.pModeProvider = pModeProvider;
    }

    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = "application/xml")
    public ResponseEntity<? extends Resource> downloadPmode(@PathVariable(value="id") int id) {

        final byte[] rawConfiguration = pModeProvider.getPModeFile(id);
        ByteArrayResource resource = new ByteArrayResource(new byte[0]);
        if (rawConfiguration != null) {
            resource = new ByteArrayResource(rawConfiguration);
        }

        HttpStatus status = HttpStatus.OK;
        if(resource.getByteArray().length == 0) {
            status = HttpStatus.NO_CONTENT;
        }

        return ResponseEntity.status(status)
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .header("content-disposition", "attachment; filename=Pmodes.xml")
                .body(resource);
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> uploadPmodes(@RequestPart("file") MultipartFile pmode, @RequestParam("description") String pModeDescription) {
        if (pmode.isEmpty()) {
            return ResponseEntity.badRequest().body("Failed to upload the PMode file since it was empty.");
        }
        try {
            byte[] bytes = pmode.getBytes();

            List<String> pmodeUpdateMessage = pModeProvider.updatePModes(bytes, pModeDescription);
            String message = "PMode file has been successfully uploaded";
            if (pmodeUpdateMessage != null && !pmodeUpdateMessage.isEmpty()) {
                message += " but some issues were detected: \n" + StringUtils.join(pmodeUpdateMessage, "\n");
            }
            return ResponseEntity.ok(message);
        } catch (XmlProcessingException e) {
            LOG.error("Error uploading the PMode", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload the PMode file due to: \n " + e.getMessage() + "\n" + StringUtils.join(e.getErrors(), "\n"));
        } catch (Exception e) {
            LOG.error("Error uploading the PMode", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload the PMode file due to: \n " + e.getMessage());
        }
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<String> deletePmodes(@RequestParam("ids") List<String> pmodesString) {
        if (pmodesString.isEmpty()) {
            return ResponseEntity.badRequest().body("Failed to delete PModes since the list of ids was empty.");
        }
        try {
            for (String pModeId : pmodesString) {
                pModeId = pModeId.replace("[", "").replace("]", "");
                pModeProvider.removePMode(Integer.parseInt(pModeId));
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Impossible to delete PModes due to \n" + ex.getMessage());
        }
        return ResponseEntity.ok("PModes were deleted\n");
    }

    @RequestMapping(value = {"/rollback/{id}"}, method = RequestMethod.PUT)
    public ResponseEntity<String> uploadPmode(@PathVariable(value="id") Integer id) {
        ConfigurationRaw rawConfiguration = pModeProvider.getRawConfiguration(id);
        rawConfiguration.setEntityId(0);

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ssZ");
        rawConfiguration.setDescription("Reverted to version of " + sdf.format(rawConfiguration.getConfigurationDate()));
        rawConfiguration.setConfigurationDate(new Date());

        String message = "PMode was successfully uploaded.";
        try {
            byte[] bytes = rawConfiguration.getXml();

            List<String> pmodeUpdateMessage = pModeProvider.updatePModes(bytes, rawConfiguration.getDescription());

            if (pmodeUpdateMessage != null && !pmodeUpdateMessage.isEmpty()) {
                message += " but some issues were detected: \n" + StringUtils.join(pmodeUpdateMessage, "\n");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message + e.getMessage());
        }
        return ResponseEntity.ok(message);
    }

    @RequestMapping(value = {"/list"}, method = RequestMethod.GET)
    public List<PModeResponseRO> pmodeList() {
        return convertRawConfigurationList(pModeProvider.getRawConfigurationList());
    }

    private List<PModeResponseRO> convertRawConfigurationList(List<ConfigurationRaw> rawConfigurationList) {
        List<PModeResponseRO> result = new ArrayList<>();
        for(ConfigurationRaw configurationRaw : rawConfigurationList) {
            PModeResponseRO pModeResponseRO = new PModeResponseRO();
            pModeResponseRO.setId(configurationRaw.getEntityId());
            pModeResponseRO.setConfigurationDate(configurationRaw.getConfigurationDate());
            pModeResponseRO.setDescription(configurationRaw.getDescription());
            pModeResponseRO.setUsername("admin"); //TODO: migueti: Missing username information
            result.add(pModeResponseRO);
        }
        return result;
    }
}
