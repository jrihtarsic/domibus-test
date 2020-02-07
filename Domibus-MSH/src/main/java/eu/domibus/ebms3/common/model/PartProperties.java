package eu.domibus.ebms3.common.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import javax.xml.bind.annotation.*;
import java.util.HashSet;
import java.util.Set;

/**
 * This element has zero or more eb:Property child elements. An eb:Property element is of
 * xs:anySimpleType (e.g. string, URI) and has a REQUIRED @name attribute, the value of which
 * must be agreed between partners. Its actual semantics is beyond the scope of this specification.
 * The element is intended to be consumed outside the ebMS specified functions. It may contain
 * meta-data that qualifies or abstracts the payload data. A representation in the header of such
 * properties allows for more efficient monitoring, correlating, dispatching and validating functions
 * (even if these are out of scope of ebMS specification) that do not require payload access.
 *
 * @author Christian Koch
 * @version 1.0
 * @since 3.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PartProperties", propOrder = "property")
@Embeddable
public class PartProperties {

    @Transient
    @XmlElement(name = "Property", required = true)
    protected Set<Property> property;

    @XmlTransient
    @OneToMany(mappedBy = "partInfo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    protected Set<PartInfoProperty> partInfoProperties;

    public void setXmlProperties() {
        Set<PartInfoProperty> partInfoProperties = getPartInfoProperties();
        for (PartInfoProperty partInfoProperty : partInfoProperties) {
            Property property = new Property();
            property.setName(partInfoProperty.getName());
            property.setType(partInfoProperty.getType());
            property.setValue(partInfoProperty.getValue());
            getProperties().add(property);
        }
    }

    // used by the mapper
    public Set<Property> getProperty() {
        return property;
    }

    // used by the mapper
    public void setProperty(Set<Property> property) {
        this.property = property;
    }

    /**
     * Gets the value of the property property.
     *
     * <p>
     * This accessor method returns a reference to the live list, not a
     * snapshot. Therefore any modification you make to the returned list will
     * be present inside the JAXB object. This is why there is not a
     * <CODE>set</CODE> method for the property property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProperty().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Property }
     *
     * @return a reference to the live list of properties
     */
    public Set<Property> getProperties() {
        if (this.property == null) {
            this.property = new HashSet<>();
        }
        return this.property;
    }

    public Set<PartInfoProperty> getPartInfoProperties() {
        if (this.partInfoProperties == null) {
            this.partInfoProperties = new HashSet<>();
        }
        return this.partInfoProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        PartProperties that = (PartProperties) o;

        return new EqualsBuilder()
                .append(property, that.property)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(property)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("property", property)
                .toString();
    }

}
