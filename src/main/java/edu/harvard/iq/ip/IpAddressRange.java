package edu.harvard.iq.ip;

import java.util.Objects;

/**
 * A range of {@link IpAddress}es. Abstract class - to instantiate, you need to
 * use one of the concrete subclasses of either IPv4 or IPv6.
 * @author michael
 * @param <T> The actual type of the IP address.
 */
public abstract class IpAddressRange<T extends IpAddress> {
    
    private final T top, bottom;

    IpAddressRange(T bottom, T top) {
        this.top = top;
        this.bottom = bottom;
    }
    
    public T getBottom() {
        return bottom;
    }

    public T getTop() {
        return top;
    }
    
    /**
     * Tests whether an IP address is within {@code this} range. Note that this
     * method returns a tri-state answer:
     * <ul>
     *  <li>Boolean.TRUE - the address is in the range</li>
     *  <li>Boolean.FALSE - the address in NOT in the range</li>
     *  <li>NULL - The address is of the wrong type, (e.g. IPv4 for an IPv6 range).
     * </ul>
     * @param anAddress The address whose inclusion we test
     * @return {@code Boolean.TRUE},{@code Boolean.FALSE}, or {@code null}.
     */
    public abstract Boolean contains( IpAddress anAddress );

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.top);
        hash = 17 * hash + Objects.hashCode(this.bottom);
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
        final IpAddressRange<?> other = (IpAddressRange<?>) obj;
        return Objects.equals(this.bottom, other.bottom) 
                && Objects.equals(this.top, other.top);
    }

    @Override
    public String toString() {
        return "[IpAddressRange " + top + "-" + bottom + ']';
    }
    
    
}
