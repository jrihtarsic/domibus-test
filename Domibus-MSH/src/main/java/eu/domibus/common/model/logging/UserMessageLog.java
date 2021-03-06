package eu.domibus.common.model.logging;

import eu.domibus.ebms3.common.model.MessageInfo;
import eu.domibus.ebms3.common.model.MessageType;
import org.apache.commons.lang3.BooleanUtils;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Federico Martini
 * @since 3.2
 */
@Entity
@Table(name = "TB_MESSAGE_LOG")
@DiscriminatorValue("USER_MESSAGE")
@NamedQueries({
        @NamedQuery(name = "UserMessageLog.findRetryMessages",
                query = "select userMessageLog.messageId " +
                        "from UserMessageLog userMessageLog " +
                        "where userMessageLog.messageStatus = eu.domibus.common.MessageStatus.WAITING_FOR_RETRY " +
                        "and userMessageLog.nextAttempt < :CURRENT_TIMESTAMP " +
                        "and 1 <= userMessageLog.sendAttempts " +
                        "and userMessageLog.sendAttempts <= userMessageLog.sendAttemptsMax " +
                        "and (userMessageLog.sourceMessage is null or userMessageLog.sourceMessage=false)" +
                        "and (userMessageLog.scheduled is null or userMessageLog.scheduled=false)"),
        @NamedQuery(name = "UserMessageLog.findReadyToPullMessages", query = "SELECT mi.messageId,mi.timestamp FROM UserMessageLog as um ,MessageInfo mi where um.messageStatus=eu.domibus.common.MessageStatus.READY_TO_PULL and um.messageId=mi.messageId order by mi.timestamp desc"),
        @NamedQuery(name = "UserMessageLog.findByMessageId", query = "select userMessageLog from UserMessageLog userMessageLog where userMessageLog.messageId=:MESSAGE_ID"),
        @NamedQuery(name = "UserMessageLog.findByMessageIdAndRole", query = "select userMessageLog from UserMessageLog userMessageLog where userMessageLog.messageId=:MESSAGE_ID and userMessageLog.mshRole=:MSH_ROLE"),
        @NamedQuery(name = "UserMessageLog.findBackendForMessage", query = "select userMessageLog.backend from UserMessageLog userMessageLog where userMessageLog.messageId=:MESSAGE_ID"),
        @NamedQuery(name = "UserMessageLog.findEntries", query = "select userMessageLog from UserMessageLog userMessageLog"),
        @NamedQuery(name = "UserMessageLog.findUndownloadedUserMessagesOlderThan", query = "select userMessageLog.messageId from UserMessageLog userMessageLog where (userMessageLog.messageStatus = eu.domibus.common.MessageStatus.RECEIVED or userMessageLog.messageStatus = eu.domibus.common.MessageStatus.RECEIVED_WITH_WARNINGS) and userMessageLog.deleted is null and userMessageLog.mpc = :MPC and userMessageLog.received < :DATE"),
        @NamedQuery(name = "UserMessageLog.findDownloadedUserMessagesOlderThan", query = "select userMessageLog.messageId from UserMessageLog userMessageLog where (userMessageLog.messageStatus = eu.domibus.common.MessageStatus.DOWNLOADED) and userMessageLog.mpc = :MPC and userMessageLog.downloaded is not null and userMessageLog.downloaded < :DATE"),
        @NamedQuery(name = "UserMessageLog.setNotificationStatus", query = "update UserMessageLog userMessageLog set userMessageLog.notificationStatus=:NOTIFICATION_STATUS where userMessageLog.messageId=:MESSAGE_ID"),
        @NamedQuery(name = "UserMessageLog.countEntries", query = "select count(userMessageLog.messageId) from UserMessageLog userMessageLog"),
        @NamedQuery(name = "UserMessageLog.setMessageStatusAndNotificationStatus",
                query = "update UserMessageLog userMessageLog set userMessageLog.deleted=:TIMESTAMP, userMessageLog.messageStatus=:MESSAGE_STATUS, userMessageLog.notificationStatus=:NOTIFICATION_STATUS where userMessageLog.messageId=:MESSAGE_ID"),
        @NamedQuery(name = "UserMessageLog.findAllInfo", query = "select userMessageLog from UserMessageLog userMessageLog")
})
public class UserMessageLog extends MessageLog {

    @ManyToOne
    @JoinColumn(name = "MESSAGE_ID", referencedColumnName = "MESSAGE_ID", updatable = false, insertable = false)
    protected MessageInfo messageInfo;

    @Column(name = "SOURCE_MESSAGE")
    protected Boolean sourceMessage;

    @Column(name = "MESSAGE_FRAGMENT")
    protected Boolean messageFragment;

    @Column(name = "SCHEDULED")
    protected Boolean scheduled;

    @Version
    @Column(name = "VERSION")
    protected int version;

    public MessageInfo getMessageInfo() {
        return messageInfo;
    }

    public void setMessageInfo(MessageInfo messageInfo) {
        this.messageInfo = messageInfo;
    }

    public UserMessageLog() {
        setMessageType(MessageType.USER_MESSAGE);
        setReceived(new Date());
        setSendAttempts(0);
    }

    public Boolean getSourceMessage() {
        return BooleanUtils.toBoolean(sourceMessage);
    }

    public void setSourceMessage(Boolean sourceMessage) {
        this.sourceMessage = sourceMessage;
    }

    public Boolean getMessageFragment() {
        return BooleanUtils.toBoolean(messageFragment);
    }

    public void setMessageFragment(Boolean messageFragment) {
        this.messageFragment = messageFragment;
    }

    public Boolean getScheduled() {
        return scheduled;
    }

    public void setScheduled(Boolean scheduled) {
        this.scheduled = scheduled;
    }

    public Boolean isSplitAndJoin() {
        return getSourceMessage() || getMessageFragment();
    }
}
