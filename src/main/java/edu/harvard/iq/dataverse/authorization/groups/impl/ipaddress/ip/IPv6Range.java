package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * 
 * @author michael
 */
@Table(indexes = {@Index(columnList="owner_id")})
@NamedQueries({
    @NamedQuery( name="IPv6Range.findGroupsContainingABCD",
                query="SELECT DISTINCT r.owner FROM IPv6Range r "
                    + "WHERE "
                        + "(    (r.topA>:a) "
                          + "or (r.topA=:a and r.topB>:b) "
                          + "or (r.topA=:a and r.topB=:b and r.topC>:c) "
                          + "or (r.topA=:a and r.topB=:b and r.topC=:c and r.topD>=:d))"
                      + " and ( (r.bottomA<:a) "
                          + "or (r.bottomA=:a and r.bottomB<:b) " 
                          + "or (r.bottomA=:a and r.bottomB=:b and r.bottomC<:c) "
                          + "or (r.bottomA=:a and r.bottomB=:b and r.bottomC=:c and r.bottomD<=:d))"
                         )
})
@Entity
public class IPv6Range extends IpAddressRange implements Serializable {

    @Id
    @GeneratedValue
    Long id;
    
    // Low-level bit representation of the addresses.
    long topA, topB, topC, topD;
    long bottomA, bottomB, bottomC, bottomD;
    
    public IPv6Range(IPv6Address bottom, IPv6Address top) {
        setTop( top );
        setBottom( bottom );
    }
    
    public IPv6Range() {}
    
    @Override
    public Boolean contains(IpAddress anAddress) {
        if ( anAddress == null ) return null;
        if ( anAddress instanceof IPv6Address ) {
            IPv6Address adr = (IPv6Address) anAddress;
            return getBottom().compareTo(adr)<=0 && getTop().compareTo(adr)>=0;
        }
        return null;
    }

    @Override
    public IPv6Address getTop() {
        return new IPv6Address( new long[]{topA, topB, topC, topD} );
    }

    @Override
    public IPv6Address getBottom() {
        return new IPv6Address( new long[]{bottomA, bottomB, bottomC, bottomD} );
    }
    
    public final void setTop( IPv6Address t ) {
        long[] tArr = t.toLongArray();
        topA = tArr[0];
        topB = tArr[1];
        topC = tArr[2];
        topD = tArr[3];
    }
    
    public final void setBottom( IPv6Address b ) {
        long[] bArr = b.toLongArray();
        bottomA = bArr[0];
        bottomB = bArr[1];
        bottomC = bArr[2];
        bottomD = bArr[3];
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getTopA() {
        return topA;
    }

    public void setTopA(long topA) {
        this.topA = topA;
    }

    public long getTopB() {
        return topB;
    }

    public void setTopB(long topB) {
        this.topB = topB;
    }

    public long getTopC() {
        return topC;
    }

    public void setTopC(long topC) {
        this.topC = topC;
    }

    public long getTopD() {
        return topD;
    }

    public void setTopD(long topD) {
        this.topD = topD;
    }

    public long getBottomA() {
        return bottomA;
    }

    public void setBottomA(long bottomA) {
        this.bottomA = bottomA;
    }

    public long getBottomB() {
        return bottomB;
    }

    public void setBottomB(long bottomB) {
        this.bottomB = bottomB;
    }

    public long getBottomC() {
        return bottomC;
    }

    public void setBottomC(long bottomC) {
        this.bottomC = bottomC;
    }

    public long getBottomD() {
        return bottomD;
    }

    public void setBottomD(long bottomD) {
        this.bottomD = bottomD;
    }
    
    
}
