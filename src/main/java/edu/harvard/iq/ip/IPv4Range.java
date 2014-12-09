package edu.harvard.iq.ip;

/**
 *
 * @author michael
 */
public class IPv4Range extends IpAddressRange<IPv4Address> {

    public IPv4Range(IPv4Address bottom, IPv4Address top) {
        super(bottom, top);
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
