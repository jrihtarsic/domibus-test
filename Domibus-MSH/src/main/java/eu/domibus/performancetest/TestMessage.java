package eu.domibus.performancetest;

import eu.domibus.ebms3.common.model.AbstractBaseEntity;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.*;

@Entity
@Table(name = "TB_TESTMESSAGE")
public class TestMessage extends AbstractBaseEntity {

    @Transient
    protected int messageId;

    @Column(name = "RAWENVELOPE")
    protected String rawEnvelope;

    public TestMessage() {}

    public TestMessage(String rawEnvelope) {
        this.rawEnvelope = rawEnvelope;
    }

    public String getRawEnvelope() {
        return rawEnvelope;
    }

    public void setRawEnvelope(String rawEnvelope) {
        this.rawEnvelope = rawEnvelope;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        TestMessage rm = (TestMessage) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(rawEnvelope, rm.rawEnvelope)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .appendSuper(super.hashCode())
                .append(rawEnvelope)
                .toHashCode();
    }
}