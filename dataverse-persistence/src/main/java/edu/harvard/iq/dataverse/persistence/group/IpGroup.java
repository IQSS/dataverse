package edu.harvard.iq.dataverse.persistence.group;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@NamedQueries({
    @NamedQuery(name = "IpGroup.findAll",
            query = "SELECT g FROM IpGroup g"),
    @NamedQuery(name = "IpGroup.findByPersistedGroupAlias",
            query = "SELECT g FROM IpGroup g WHERE g.persistedGroupAlias=:persistedGroupAlias")
})
@Entity
public class IpGroup extends PersistedGlobalGroup {

    public final static String GROUP_TYPE = "ip";
    
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<IPv6Range> ipv6Ranges;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<IPv4Range> ipv4Ranges;

    public IpGroup() {
        super(GROUP_TYPE);
    }

    public boolean containsAddress(IpAddress addr) {
        for (IpAddressRange r : ((addr instanceof IPv4Address) ? ipv4Ranges : ipv6Ranges)) {
            Boolean containment = r.contains(addr);
            if ((containment != null) && containment) {
                return true;
            }
        }
        return false;
    }

    public <T extends IpAddressRange> T add(T range) {
        if (ipv4Ranges == null) {
            ipv4Ranges = new HashSet<>();
        }
        if (ipv6Ranges == null) {
            ipv6Ranges = new HashSet<>();
        }

        range.setOwner(this);
        if (range instanceof IPv4Range) {
            ipv4Ranges.add((IPv4Range) range);
        } else {
            ipv6Ranges.add((IPv6Range) range);
        }
        return range;
    }

    @SuppressWarnings("element-type-mismatch")
    public void remove(IpAddressRange range) {
        ((range instanceof IPv4Range) ? ipv4Ranges : ipv6Ranges).remove(range);
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    /**
     * Returns a <strong>read only</strong> set of all the ranges  in the group,
     * both IPv6 and IPv4.
     *
     * @return
     */
    public Set<IpAddressRange> getRanges() {
        Set<IpAddressRange> ranges = new HashSet<>();
        ranges.addAll(getIpv4Ranges());
        ranges.addAll(getIpv6Ranges());
        return ranges;
    }

    /**
     * Low-level JPA accessor
     *
     * @return
     * @see #getRanges()
     */
    public Set<IPv6Range> getIpv6Ranges() {
        return ipv6Ranges;
    }

    /**
     * Low-level JPA accessor
     *
     * @param ipv6Ranges
     */
    public void setIpv6Ranges(Set<IPv6Range> ipv6Ranges) {
        this.ipv6Ranges = ipv6Ranges;
        updateRangeOwnership(ipv6Ranges);
    }

    /**
     * Low-level JPA accessor
     *
     * @return
     * @see #getRanges()
     */
    public Set<IPv4Range> getIpv4Ranges() {
        return ipv4Ranges;
    }

    public void setIpv4Ranges(Set<IPv4Range> ipv4Ranges) {
        this.ipv4Ranges = ipv4Ranges;
        updateRangeOwnership(ipv4Ranges);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof IpGroup)) {
            return false;
        }

        IpGroup other = (IpGroup) o;

        if (!Objects.equals(getId(), other.getId())) {
            return false;
        }
        if (!Objects.equals(getDescription(), other.getDescription())) {
            return false;
        }
        if (!Objects.equals(getDisplayName(), other.getDisplayName())) {
            return false;
        }
        return getRanges().equals(other.getRanges());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return "[IpGroup id:" + getId() + " ranges:" + getIpv4Ranges() + "," + getIpv6Ranges() + "]";
    }

    private void updateRangeOwnership(Collection<? extends IpAddressRange> ranges) {
        for (IpAddressRange rng : ranges) {
            rng.setOwner(this);
        }
    }
}
