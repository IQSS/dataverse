package edu.harvard.iq.ip;

/**
 *
 * @author michael
 */
public class IPv6Range extends IpAddressRange<IPv6Address> {

    public IPv6Range(IPv6Address bottom, IPv6Address top) {
        super(bottom, top);
    }
    
    @Override
    public Boolean contains(IpAddress anAddress) {
        if ( anAddress instanceof IPv6Address ) {
            IPv6Address adr = (IPv6Address) anAddress;
            return getBottom().compareTo(adr)<=0 && getTop().compareTo(adr)>=0;
        }
        return null;
    }
    
}
