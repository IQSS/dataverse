package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * A range of IPv4 addresses. In order to make SQL querying efficient, the actual fields
 * are stored as {@code long} numbers. This is why we have the {@link #getTopAsLong()} and other
 * such methods. For most non-JPA uses, use the higher API of {@link #getTop()}
 * which returns the IP address object.
 * 
 * @author michael
 */
@NamedQueries({
    @NamedQuery( name="IPv4Range.findAllContainingAddressAsLong",
            query="SELECT r FROM IPv4Range r WHERE r.bottomAsLong<=:addressAsLong AND r.topAsLong>=:addressAsLong"),
    @NamedQuery( name="IPv4Range.findGroupsContainingAddressAsLong", 
                query="SELECT DISTINCT r.owner from IPv4Range r WHERE r.bottomAsLong<=:addressAsLong AND r.topAsLong>=:addressAsLong")
})
@Entity
public class IPv4Range extends IpAddressRange implements java.io.Serializable {
    
    @Id
    @GeneratedValue
    Long id;
    
    /** The most significant bits of {@code this} range's top addre, i.e the first two numbers of the IP address */
    long topAsLong;
    
    /** The least significant bits, i.e the last tow numbers of the IP address */
    long bottomAsLong;
    
    public IPv4Range(){}
    
    public IPv4Range(IPv4Address bottom, IPv4Address top) {
        topAsLong = top.toLong();
        bottomAsLong = bottom.toLong(); 
    }
    
    @Override
    public IPv4Address getTop() {
        return new IPv4Address(getTopAsLong());
    }
    
    public void setTop( IPv4Address aNewTop ) {
        setTopAsLong( aNewTop.toLong() );
    }
    
    @Override
    public IPv4Address getBottom() {
        return new IPv4Address(getBottomAsLong());
    }
    
    public void setBottom( IPv4Address aNewBottom ) {
        setTopAsLong( aNewBottom.toLong() );
    }
    
    public long getTopAsLong() {
        return topAsLong;
    }

    public void setTopAsLong(long topAsLong) {
        this.topAsLong = topAsLong;
    }

    public long getBottomAsLong() {
        return bottomAsLong;
    }

    public void setBottomAsLong(long bottomAsLong) {
        this.bottomAsLong = bottomAsLong;
    }

    @Override
    public Boolean contains(IpAddress anAddress) {
        if ( anAddress instanceof IPv4Address ) {
            IPv4Address adr = (IPv4Address) anAddress;
            return getBottom().compareTo(adr)<=0 && getTop().compareTo(adr)>=0;
        }
        return null;
    }
    
}
