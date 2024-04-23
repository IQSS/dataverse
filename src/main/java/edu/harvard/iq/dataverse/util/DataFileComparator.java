package edu.harvard.iq.dataverse.util;


import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileCategory;
import edu.harvard.iq.dataverse.FileMetadata;

/**
 *
 * @author qqmyers
 */
public class DataFileComparator implements Comparator<FileMetadata> {

    boolean byFolder = false;
    boolean byCategory = false;
    String field = "name";
    boolean ascending = true;

    public Comparator<FileMetadata> compareBy(boolean byFolder, boolean byCategory, String field, boolean ascending) {
        this.byFolder = byFolder;
        this.byCategory = byCategory;
        if(StringUtil.nonEmpty(field)) {
            this.field = field;
        }
        this.ascending = ascending;
        return this;
    }
    
    public boolean getByFolder() {
        return this.byFolder;
    }
    public int getByCategory() {
        return FileMetadata.getCategorySortOrder().size();
    }
    
    public String getField() {
        return this.field;
    }
    
    public boolean getAsc() {
        return this.ascending;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public int compare(FileMetadata o1, FileMetadata o2) {
        if (byFolder) {
            // Compare folders first
            String folder1 = o1.getDirectoryLabel() == null ? "" : o1.getDirectoryLabel().toUpperCase();
            String folder2 = o2.getDirectoryLabel() == null ? "" : o2.getDirectoryLabel().toUpperCase();

            if ("".equals(folder1) && !"".equals(folder2)) {
                return -1;
            }

            if ("".equals(folder2) && !"".equals(folder1)) {
                return 1;
            }

            int comp = folder1.compareTo(folder2);
            if (comp != 0) {
                return comp;
            }
        }
        Map<String,Long> categoryMap = FileMetadata.getCategorySortOrder();
        
        if (byCategory) {
            // Then by category if set
            if (categoryMap != null) {
                long rank1 = Long.MAX_VALUE;
                for (DataFileCategory c : o1.getCategories()) {
                    Long rank = categoryMap.get(c.getName().toUpperCase());
                    if (rank != null) {
                        if (rank < rank1) {
                            rank1 = rank;
                        }
                    }
                }
                long rank2 = Long.MAX_VALUE;
                for (DataFileCategory c : o2.getCategories()) {
                    Long rank = categoryMap.get(c.getName().toUpperCase());
                    if (rank != null) {
                        if (rank < rank2) {
                            rank2 = rank;
                        }
                    }
                }
                if (rank1 != rank2) {
                    return rank1 < rank2 ? -1 : 1;
                }
            }
        }

        // Folders are equal, no categories or category score is equal, so compare
        // labels
        Comparable file1 = null;
        Comparable file2 = null;
        switch (field) {
        case "date":
            file1 = getFileDateToCompare(o1);
            file2 = getFileDateToCompare(o2);
            break;
        case "type":
            file1 = StringUtil.isEmpty(o1.getDataFile().getFriendlyType()) ? "" : o1.getDataFile().getContentType();
            file2 = StringUtil.isEmpty(o2.getDataFile().getFriendlyType()) ? "" : o2.getDataFile().getContentType();
            break;
        case "size":
            file1 = new Long(o1.getDataFile().getFilesize());
            file2 = new Long(o2.getDataFile().getFilesize());
            break;
        default: // "name" or not recognized
            file1 = o1.getLabel().toUpperCase();
            file2 = o2.getLabel().toUpperCase();

        }
        return (ascending ? file1.compareTo(file2) : file2.compareTo(file1));
    }

    private Date getFileDateToCompare(FileMetadata fileMetadata) {
        DataFile datafile = fileMetadata.getDataFile();
        if (datafile.isReleased()) {
            return datafile.getPublicationDate();
        }
        return datafile.getCreateDate();
    }
}