/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.usage;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Continent;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Subdivision;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;

/**
 *
 * @author luopc
 */
@Singleton
public class GeoipServiceBean {

    private static final Logger logger = Logger.getLogger(GeoipServiceBean.class.getCanonicalName());
    
    private static DatabaseReader geoipReader;
    
    @PostConstruct
    public void init(){
        try{
            String filePath = GeoipServiceBean.class.getResource("GeoLite2-City.mmdb").getFile();
            geoipReader = new DatabaseReader.Builder(new File(filePath)).build();
        }catch(Exception e){
            close();
            logger.log(Level.SEVERE, "geoip reader init error", e);
        }
    }
    
    public void addGeoInfo(Event event){
        if(event.getIp() == null)return ;        
        CityResponse response = null;
        try {
            InetAddress ipAddress = InetAddress.getByName(event.getIp());
            response = geoipReader.city(ipAddress);
        } catch (UnknownHostException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (IOException | GeoIp2Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch(Exception ex){
            logger.log(Level.SEVERE, null, ex);
        }
        
        if(response == null)return;
        
        Continent continent = response.getContinent();
        event.setContinent(continent.getName());
        
        Country country = response.getCountry();
        event.setCountry(country.getName());
        
        Subdivision subdivision = response.getMostSpecificSubdivision();
        event.setSubdivision(subdivision.getName());
        
        City city = response.getCity();
        event.setCity(city.getName());
        
        Location location = response.getLocation();
        event.setLatitude(location.getLatitude());
        event.setLongitude(location.getLongitude());        
    }
    
    @PreDestroy
    public void close(){
        if(geoipReader != null){
            try {
                geoipReader.close();
                geoipReader = null;
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "close geoip reader error", ex);
            }
        }
    }
}
