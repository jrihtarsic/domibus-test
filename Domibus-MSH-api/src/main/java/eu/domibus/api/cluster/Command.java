package eu.domibus.api.cluster;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author kochc01
 * @author Cosmin Baciu
 */
public class Command {

    public static final String COMMAND = "COMMAND";
    public static final String RELOAD_PMODE = "RELOAD_PMODE";
    public static final String EVICT_CACHES = "EVICT_CACHES";
    public static final String RELOAD_TRUSTSTORE = "RELOAD_TRUSTSTORE";
    public static final String LOGGING_RESET = "LOGGING_RESET";
    public static final String LOGGING_SET_LEVEL = "LOGGING_SET_LEVEL";
    public static final String DOMIBUS_PROPERTY_CHANGE = "DOMIBUS_PROPERTY_CHANGE";

    private int entityId;
    protected String commandName;
    protected String serverName;
    protected String domain;
    protected Date creationTime;

    private Map<String, String> commandProperties = new HashMap<>();

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public Map<String, String> getCommandProperties() {
        return commandProperties;
    }

    public void setCommandProperties(Map<String, String> commandProperties) {
        this.commandProperties = commandProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Command command = (Command) o;

        return new EqualsBuilder()
                .append(commandName, command.commandName)
                .append(serverName, command.serverName)
                .append(domain, command.domain)
                .append(commandProperties, command.commandProperties)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(commandName)
                .append(serverName)
                .append(domain)
                .append(commandProperties)
                .toHashCode();
    }
}
