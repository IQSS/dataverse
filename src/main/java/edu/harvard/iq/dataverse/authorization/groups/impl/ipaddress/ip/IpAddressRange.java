package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroup;
import java.util.Objects;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

/**
 * A range of {@link IpAddress}es. Abstract class - to instantiate, you need to
 * use one of the concrete subclasses of either IPv4 or IPv6, or the static 
 * {@code make} methods.
 * @author michael
 */
@MappedSuperclass
public abstract class IpAddressRange {
    
    public static IpAddressRange make( IpAddress bottom, IpAddress top ) {
        if ( bottom instanceof IPv4Address && top instanceof IPv4Address ) {
            return new IPv4Range((IPv4Address)bottom, (IPv4Address)top);
        } else if ( bottom instanceof IPv6Address && top instanceof IPv6Address ) {
            return new IPv6Range((IPv6Address)bottom, (IPv6Address)top);
        } else {
            throw new IllegalArgumentException("Both addresses have to be of the same type (either IPv4 or IPv6)");
        }
    }
    
    public static IpAddressRange makeSingle( IpAddress ipa ) { 
        return make(ipa, ipa);
    }
    
    /**
     * Tests whether an IP address is within {@code this} range. Note that this
     * method returns a tri-state answer:
     * <ul>
     *  <li>Boolean.TRUE - the address is in the range</li>
     *  <li>Boolean.FALSE - the address in NOT in the range</li>
     *  <li>NULL - The address is of the wrong type, (e.g. IPv4 for an IPv6 range).</li>
     * </ul>
     * @param anAddress The address whose inclusion we test
     * @return {@code Boolean.TRUE},{@code Boolean.FALSE}, or {@code null}.
     */
    public abstract Boolean contains( IpAddress anAddress );
    
    public abstract IpAddress getTop();
    public abstract IpAddress getBottom();
    
    @ManyToOne
    private IpGroup owner;
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(getTop());
        hash = 17 * hash + Objects.hashCode(getBottom());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IpAddressRange other = (IpAddressRange) obj;
        return Objects.equals(getBottom(), other.getBottom()) 
                && Objects.equals(this.getTop(), other.getTop());
    }

    public boolean isSingleAddress() {
        return getTop().equals(getBottom());
    }
    
    @Override
    public String toString() {
        return "[IpAddressRange " + getTop() + "-" + getBottom() + ']';
    }

    public IpGroup getOwner() {
        return owner;
    }

    public void setOwner(IpGroup owner) {
        this.owner = owner;
    }
    
}
