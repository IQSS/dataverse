/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api.dto;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author ellenk
 */
public class DatasetVersionDTO {
    String archiveNote;
    String deacessionLink;
    String versionNumber;
    String minorVersionNumber;
    long id;
    String versionState;
    String releaseDate;
    String lastUpdateTime;
    String createTime;
    String archiveTime;
    Map<String,MetadataBlockDTO> metadataBlocks;

    public String getArchiveNote() {
        return archiveNote;
    }

    public void setArchiveNote(String archiveNote) {
        this.archiveNote = archiveNote;
    }

    public String getDeacessionLink() {
        return deacessionLink;
    }

    public void setDeacessionLink(String deacessionLink) {
        this.deacessionLink = deacessionLink;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getMinorVersionNumber() {
        return minorVersionNumber;
    }

    public void setMinorVersionNumber(String minorVersionNumber) {
        this.minorVersionNumber = minorVersionNumber;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVersionState() {
        return versionState;
    }

    public void setVersionState(String versionState) {
        this.versionState = versionState;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(String lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getArchiveTime() {
        return archiveTime;
    }

    public void setArchiveTime(String archiveTime) {
        this.archiveTime = archiveTime;
    }

    public Map<String, MetadataBlockDTO> getMetadataBlocks() {
        return metadataBlocks;
    }

    public void setMetadataBlocks(Map<String, MetadataBlockDTO> metadataBlocks) {
        this.metadataBlocks = metadataBlocks;
    }
    
    
    
     public static class FieldDTO {
        public  FieldDTO(){}
       
        String typeName;
        Boolean multiple;
        String typeClass;
        // The contents of value depend on the field attributes
        // if single/primitive, value is a String
        // if multiple, value is a JSonArray
        //      multiple/primitive: each JSonArray element will contain String
        //      multiple/compound: each JSonArray element will contain Set of FieldDTOs
        // 
        JsonElement value;
        
        String getSinglePrimitive() {
            return value.getAsString();
        }
        
        List<String> getMultiplePrimitive() {
            List<String> values = new ArrayList<>();
            Iterator<JsonElement> iter =  value.getAsJsonArray().iterator();
            while (iter.hasNext()) {
                values.add(iter.next().getAsString());
                
            }
            return values;
        }
        
         List<FieldDTO> getSingleCompound() {
             Gson gson = new Gson();
             JsonObject elem = (JsonObject) value;
             ArrayList<FieldDTO> elemFields = new ArrayList<FieldDTO>();

             Set<Entry<String, JsonElement>> set = elem.entrySet();

             Iterator<Entry<String, JsonElement>> setIter = set.iterator();
             while (setIter.hasNext()) {
                 Entry<String, JsonElement> entry = setIter.next();
                 FieldDTO field = gson.fromJson(entry.getValue(), FieldDTO.class);
                 elemFields.add(field);
             }
             return elemFields;
         }
        
        ArrayList<ArrayList<FieldDTO>> getMultipleCompound() {
            Gson gson = new Gson();
            ArrayList<ArrayList<FieldDTO>> fields = new ArrayList<ArrayList<FieldDTO>>();            
            JsonArray array = value.getAsJsonArray();
            
            Iterator<JsonElement> iter = array.iterator();
            while (iter.hasNext()) {
                JsonObject elem = (JsonObject)iter.next();
                ArrayList<FieldDTO> elemFields = new ArrayList<FieldDTO>();
                fields.add(elemFields);                
                Set<Entry<String, JsonElement>> set = elem.entrySet();
               
                Iterator<Entry<String, JsonElement>> setIter = set.iterator();
                while(setIter.hasNext()) {
                    Entry<String,JsonElement> entry = setIter.next();
                    FieldDTO field = gson.fromJson(entry.getValue(), FieldDTO.class);
                    elemFields.add(field);
                }
            }
            
            return fields;
        }
        
        Object getConvertedValue() {
            if (multiple) {
                if (typeClass.equals("compound")) {
                    return getMultipleCompound();
                } else {
                    return getMultiplePrimitive();
                } 
               
            } else {
                  if (typeClass.equals("compound")) {
                    return getSingleCompound();
                } else {
                    return getSinglePrimitive();
                } 
            }
        }

        @Override
        public String toString() {
            return "FieldDTO{" + "typeName=" + typeName + ", multiple=" + multiple + ", typeClass=" + typeClass + ", value=" + getConvertedValue() + '}';      
        }
        
     }
     
     public static class MetadataBlockDTO {
         String displayName;
         List<FieldDTO> fields;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public List<FieldDTO> getFields() {
            return fields;
        }

        public void setFields(List<FieldDTO> fields) {
            this.fields = fields;
        }

        @Override
        public String toString() {
            return "MetadataBlockDTO{" + "displayName=" + displayName + ", fields=" + fields + '}';
        }
     }

    @Override
    public String toString() {
        return "DataSetVersionDTO: {" + "archiveNote=" + archiveNote + ", deacessionLink=" + deacessionLink + ", versionNumber=" + versionNumber + ", minorVersionNumber=" + minorVersionNumber + ", id=" + id + ", versionState=" + versionState + ", releaseDate=" + releaseDate + ", lastUpdateTime=" + lastUpdateTime + ", createTime=" + createTime + ", archiveTime=" + archiveTime + ", metadataBlocks=" + metadataBlocks + '}';
    }
     
    
 
}
