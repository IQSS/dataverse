package edu.harvard.iq.dataverse.provenance;
/**
 *
 * @author madunlap
 */
public class ProvEntityFileData implements Comparable{
    String entityName;
    String fileName;
    String fileType;

     ProvEntityFileData(String entityName, String fileName, String fileType) {
        this.entityName = entityName;
        this.fileName = fileName;
        this.fileType = fileType;
    }
    
    public String getEntityName() {
        return entityName;
    }
    public String getFileName() {
        return fileName;
    }
    public String getFileType() {
        return fileType;
    }

    //This compareTo has been created with the specific purpose of sorting a UI element
    //This class will almost certainly not be used anywhere else
    @Override
    public int compareTo(Object o) {
    if (!(o instanceof ProvEntityFileData)) {
        throw new ClassCastException("A ProvEntityFileData object expected.");
    }
        
        return this.entityName.compareToIgnoreCase(((ProvEntityFileData) o).entityName)*-1; //the -1 is to flip the order of sorting
    }
}
