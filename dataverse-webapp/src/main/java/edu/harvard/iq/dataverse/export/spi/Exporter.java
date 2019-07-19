/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.export.spi;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.export.ExportException;

import javax.ws.rs.core.MediaType;

/**
 * @author skraffmi
 */
public interface Exporter {

    String exportDataset(DatasetVersion version) throws ExportException;

    String getProviderName();

    String getDisplayName();

    Boolean isXMLFormat();

    Boolean isHarvestable();

    Boolean isAvailableToUsers();

    String getXMLNameSpace();

    String getXMLSchemaLocation();

    String getXMLSchemaVersion();

    void setParam(String name, Object value);

    default String getMediaType() {
        return MediaType.APPLICATION_XML;
    }

}
