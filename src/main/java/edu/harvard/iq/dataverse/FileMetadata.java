package edu.harvard.iq.dataverse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import edu.harvard.iq.dataverse.datasetutility.OptionalFileParams;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.persistence.*;

import edu.harvard.iq.dataverse.datavariable.CategoryMetadata;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VarGroup;
import edu.harvard.iq.dataverse.datavariable.VariableMetadata;
import edu.harvard.iq.dataverse.util.DateUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.util.HashSet;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.hibernate.validator.constraints.NotBlank;
import javax.validation.constraints.Pattern;


/**
 *
 * @author skraffmiller
 */
@Table(indexes = {@Index(columnList="datafile_id"), @Index(columnList="datasetversion_id")} )
@Entity
public class FileMetadata implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(FileMetadata.class.getCanonicalName());


    @Expose
    @Pattern(regexp="^[^:<>;#/\"\\*\\|\\?\\\\]*$", 
            message = "{filename.illegalCharacters}")
    @NotBlank(message = "{filename.blank}")
    @Column( nullable=false )
    private String label = "";
    
    
    @ValidateDataFileDirectoryName(message = "{directoryname.illegalCharacters}")
    @Expose
    @Column ( nullable=true )

    private String directoryLabel;
    @Expose
    @Column(columnDefinition = "TEXT")
    private String description = "";
    
    /**
     * At the FileMetadata level, "restricted" is a historical indication of the
     * data owner's intent for the file by version. Permissions are actually
     * enforced based on the "restricted" boolean at the *DataFile* level. On
     * publish, the latest intent is copied from the FileMetadata level to the
     * DataFile level.
     */
    @Expose
    private boolean restricted;

    @ManyToOne
    @JoinColumn(nullable=false)
    private DatasetVersion datasetVersion;
    
    @ManyToOne
    @JoinColumn(nullable=false)
    private DataFile dataFile;

    /**
     * There are two types of provenance types and this "free-form" type is
     * represented in the GUI as text box the user can type into. The other type
     * is based on PROV-JSON from the W3C.
     */
    @Expose
    @Column(columnDefinition = "TEXT", nullable = true, name="prov_freeform")
    private String provFreeForm;

    @OneToMany (mappedBy="fileMetadata", cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST})
    private Collection<VariableMetadata> variableMetadatas;
        
    /**
     * Creates a copy of {@code this}, with identical business logic fields.
     * E.g., {@link #label} would be duplicated; {@link #version} will not.
     * 
     * @return A copy of {@code this}, except for the DB-related data.
     */
    public FileMetadata createCopy() {
        FileMetadata fmd = new FileMetadata();
        fmd.setCategories(new LinkedList<>(getCategories()) );
        fmd.setDataFile( getDataFile() );
        fmd.setDatasetVersion( getDatasetVersion() );
        fmd.setDescription( getDescription() );
        fmd.setLabel( getLabel() );
        fmd.setRestricted( isRestricted() );
        
        return fmd;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }

    public FileMetadata() {
        variableMetadatas = new ArrayList<VariableMetadata>();
        varGroups = new ArrayList<VarGroup>();
    }

    public String getDirectoryLabel() {
        return directoryLabel;
    }

    public void setDirectoryLabel(String directoryLabel) {
        //Strip off beginning and ending \ // - .
        // and replace any sequences/combinations of / and \ with a single /
        if (directoryLabel != null) {
            directoryLabel = StringUtil.sanitizeFileDirectory(directoryLabel);
        }

        this.directoryLabel = directoryLabel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    @OneToMany(mappedBy="fileMetadata", cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST})
    private List<VarGroup> varGroups;

    public Collection<VariableMetadata> getVariableMetadatas() {
        return variableMetadatas;
    }

    public List<VarGroup> getVarGroups() {
        return varGroups;
    }

    public void setVariableMetadatas(Collection<VariableMetadata> variableMetadatas) {
        this.variableMetadatas = variableMetadatas;
    }

    public void setVarGroups(List<VarGroup> varGroups) {
        this.varGroups = varGroups;
    }

    /*
     * File Categories to which this version of the DataFile belongs: 
     */
    @SerializedName("categories") //Used for OptionalFileParams serialization
    @ManyToMany
    @JoinTable(indexes = {@Index(columnList="filecategories_id"),@Index(columnList="filemetadatas_id")})
    @OrderBy("name")
    private List<DataFileCategory> fileCategories;
    
    public List<DataFileCategory> getCategories() {
        return fileCategories;
    }
    
    public void setCategories(List<DataFileCategory> fileCategories) {
        this.fileCategories = fileCategories; 
    }
    
    public void addCategory(DataFileCategory category) {
        if (fileCategories == null) {
            fileCategories = new ArrayList<>();
        }
        fileCategories.add(category);
    }

    /**
     * Retrieve categories 
     * @return 
     */
    public List<String> getCategoriesByName() {
        ArrayList<String> ret = new ArrayList<>();
             
        if (fileCategories == null) {
            return ret;
        }
        
        for (DataFileCategory fileCategory : fileCategories) {
            ret.add(fileCategory.getName());
        }
        // fileCategories.stream()
        //              .map(x -> ret.add(x.getName()));
       
        return ret;
    }
    
    
    public JsonArrayBuilder getCategoryNamesAsJsonArrayBuilder() {

        JsonArrayBuilder builder = Json.createArrayBuilder();

        if (fileCategories == null) {
            return builder;
        }
        
        for (DataFileCategory fileCategory : fileCategories) {
            builder.add(fileCategory.getName());
        }

        //fileCategories.stream()
        //              .map(x -> builder.add(x.getName()));
        
        return builder;
        
    }
    
    
    // alternative, experimental method: 

    public void setCategoriesByName(List<String> newCategoryNames) {
        setCategories(null); // ?? TODO: investigate! 

        if (newCategoryNames != null) {

            for (String newCategoryName : newCategoryNames) {
                // Dataset.getCategoryByName() will check if such a category 
                // already exists for the parent dataset; it will be created 
                // if not. The method will return null if the supplied 
                // category name is null or empty. -- L.A. 4.0 beta 10
                DataFileCategory fileCategory;
                try {
                    // Using "try {}" to catch any null pointer exceptions, 
                    // just in case: 
                    fileCategory = this.getDatasetVersion().getDataset().getCategoryByName(newCategoryName);
                } catch (Exception ex) {
                    fileCategory = null;
                }
                if (fileCategory != null) {
                    this.addCategory(fileCategory);
                    fileCategory.addFileMetadata(this);
                }
            }
        }
    }
    
    /* 
        note that this version only *adds* new categories, but does not 
        remove the ones that has been unchecked!
    public void setCategoriesByName(List<String> newCategoryNames) {
        if (newCategoryNames != null) {
            Collection<String> oldCategoryNames = getCategoriesByName();
            
            
            for (int i = 0; i < newCategoryNames.size(); i++) {
                if (!oldCategoryNames.contains(newCategoryNames.get(i))) {
                    // Dataset.getCategoryByName() will check if such a category 
                    // already exists for the parent dataset; it will be created 
                    // if not. The method will return null if the supplied 
                    // category name is null or empty. -- L.A. 4.0 beta 10
                    DataFileCategory fileCategory = null; 
                    try { 
                        // Using "try {}" to catch any null pointer exceptions, 
                        // just in case: 
                        fileCategory = this.getDatasetVersion().getDataset().getCategoryByName(newCategoryNames.get(i));
                    } catch (Exception ex) {
                        fileCategory = null; 
                    }
                    if (fileCategory != null) { 
                        this.addCategory(fileCategory);
                        fileCategory.addFileMetadata(this);
                    }
                } 
            }
        }
    }
    */
    
    public void addCategoryByName(String newCategoryName) {
        if (newCategoryName != null && !newCategoryName.isEmpty()) {
            Collection<String> oldCategoryNames = getCategoriesByName();
            if (!oldCategoryNames.contains(newCategoryName)) {
                DataFileCategory fileCategory;
                // Dataset.getCategoryByName() will check if such a category 
                // already exists for the parent dataset; it will be created 
                // if not. The method will return null if the supplied 
                // category name is null or empty. -- L.A. 4.0 beta 10
                try {
                    // Using "try {}" to catch any null pointer exceptions, 
                    // just in case: 
                    fileCategory = this.getDatasetVersion().getDataset().getCategoryByName(newCategoryName);
                } catch (Exception ex) {
                    // If we failed to obtain an existing category, we'll create a new one:
                    fileCategory = new DataFileCategory();
                    fileCategory.setName(newCategoryName);
                }

                
                if (fileCategory != null) {
                    logger.log(Level.FINE, "Found file category for {0}", newCategoryName);

                    this.addCategory(fileCategory);
                    fileCategory.addFileMetadata(this);
                } else {
                    logger.log(Level.INFO, "Could not find file category for {0}", newCategoryName);
                }
            } else {
                // don't do anything - this file metadata already belongs to
                // this category.
            }
        }
    }
    
     public String getFileDateToDisplay() {
        Date fileDate = null;
        DataFile datafile = this.getDataFile();
        if (datafile != null) {
            boolean fileHasBeenReleased = datafile.isReleased();
            if (fileHasBeenReleased) {
                Timestamp filePublicationTimestamp = datafile.getPublicationDate();
                if (filePublicationTimestamp != null) {
                    fileDate = filePublicationTimestamp;
                }
            } else {
                Timestamp fileCreateTimestamp = datafile.getCreateDate();
                if (fileCreateTimestamp != null) {
                    fileDate = fileCreateTimestamp;
                }
            }
        }
        if (fileDate != null) {
            return DateUtil.formatDate(fileDate);
        }
        return "";
    }
     
    public String getFileCitation(){
         return getFileCitation(false);
     }
     

    
     
    public String getFileCitation(boolean html){
         return new DataCitation(this).toString(html);
     }
    
    public String getDirectFileCitation(boolean html){
    	return new DataCitation(this, true).toString(html);
    }
    
        
    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }



    public DataFile getDataFile() {
        return dataFile;
    }

    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile;
    }


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Getter for property id.
     * @return Value of property id.
     */
    public Long getId() {
        return this.id;
    }

    /**
     * Setter for property id.
     * @param id New value of property id.
     */
    public void setId(Long id) {
        this.id = id;
    }


    @Version
    private Long version;

    /**
     * Getter for property version.
     * @return Value of property version.
     */
    public Long getVersion() {
        return this.version;
    }

    /**
     * Setter for property version.
     * @param version New value of property version.
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    @Transient
    private boolean selected;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    @Transient
    private boolean restrictedUI;

    public boolean isRestrictedUI() {
        return restrictedUI;
    }

    public void setRestrictedUI(boolean restrictedUI) {
        this.restrictedUI = restrictedUI;
    }
    
    @Transient
    private FileVersionDifference fileVersionDifference ;

    public FileVersionDifference getFileVersionDifference() {
        return fileVersionDifference;
    }

    public void setFileVersionDifference(FileVersionDifference fileVersionDifference) {
        this.fileVersionDifference = fileVersionDifference;
    }
    
    @Transient
    private String contributorNames;

    public String getContributorNames() {
        return contributorNames;
    }

    public void setContributorNames(String contributorNames) {
        this.contributorNames = contributorNames;
    }
        

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof FileMetadata)) {
            return false;
        }
        FileMetadata other = (FileMetadata) object;
        
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    /* 
     * An experimental method for comparing 2 file metadatas *by content*; i.e., 
     * this would be for checking 2 metadatas from 2 different versions, to 
     * determine if any of the actual metadata fields have changed between 
     * versions. 
    */
    public boolean contentEquals(FileMetadata other) {
        if (other == null) {
            return false; 
        }
        
        if (this.getLabel() != null) {
            if (!this.getLabel().equals(other.getLabel())) {
                return false;
            }
        } else if (other.getLabel() != null) {
            return false;
        }

        if (this.getDirectoryLabel() != null) {
            if (!this.getDirectoryLabel().equals(other.getDirectoryLabel())) {
                return false;
            }
        } else if (other.getDirectoryLabel() != null) {
            return false;
        }
        
        if (this.getDescription() != null) {
            if (!this.getDescription().equals(other.getDescription())) {
                return false;
            }
        } else if (other.getDescription() != null) {
            return false;
        }
        
        return true;
    }
    
    
    @Override
    public String toString() {
        return "edu.harvard.iq.dvn.core.study.FileMetadata[id=" + id + "]";
    }
    
    public static final Comparator<FileMetadata> compareByLabel = new Comparator<FileMetadata>() {
        @Override
        public int compare(FileMetadata o1, FileMetadata o2) {
            return o1.getLabel().toUpperCase().compareTo(o2.getLabel().toUpperCase());
        }
    };
    
    public static final Comparator<FileMetadata> compareByLabelAndFolder = new Comparator<FileMetadata>() {
        @Override
        public int compare(FileMetadata o1, FileMetadata o2) {
            String folder1 = o1.getDirectoryLabel() == null ? "" : o1.getDirectoryLabel().toUpperCase();
            String folder2 = o2.getDirectoryLabel() == null ? "" : o2.getDirectoryLabel().toUpperCase();
            
            
            // We want to the files w/ no folders appear *after* all the folders
            // on the sorted list:
            if ("".equals(folder1) && !"".equals(folder2)) {
                return 1;
            }
            
            if ("".equals(folder2) && !"".equals(folder1)) {
                return -1;
            }
            
            int comp = folder1.compareTo(folder2); 
            if (comp != 0) {
                return comp;
            }
            return o1.getLabel().toUpperCase().compareTo(o2.getLabel().toUpperCase());
        }
    };
    
    
    public String toPrettyJSON(){
        
        return serializeAsJSON(true);
    }

    public String toJSON(){
        
        return serializeAsJSON(false);
    }
    
     /**
     * 
     * @param prettyPrint
     * @return 
     */
    private String serializeAsJSON(boolean prettyPrint){
        
        JsonObject jsonObj = asGsonObject(prettyPrint);
                
        return jsonObj.toString();
       
    }

    
    public JsonObject asGsonObject(boolean prettyPrint){
        
        GsonBuilder builder;
        if (prettyPrint){  // Add pretty printing
            builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting();
        } else {
            builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation();                        
        }
        
        Gson gson = builder.create();
        
        JsonElement jsonObj = gson.toJsonTree(this);
        
        //Add categories without the "name"
        List<String> cats = this.getCategoriesByName();
        JsonArray jsonCats = new JsonArray();
        for(String t : cats) {
            jsonCats.add(new JsonPrimitive(t));
        }
        if(jsonCats.size() > 0) {
            jsonObj.getAsJsonObject().add(OptionalFileParams.CATEGORIES_ATTR_NAME, jsonCats);
        }
        
        //Add tags without the "name"
        List<String> tags = this.getDataFile().getTagLabels();
        JsonArray jsonTags = new JsonArray();
        for(String t : tags) {
            jsonTags.add(new JsonPrimitive(t));
        }
        if(jsonTags.size() > 0) {
            jsonObj.getAsJsonObject().add(OptionalFileParams.FILE_DATA_TAGS_ATTR_NAME, jsonTags);
        }

        jsonObj.getAsJsonObject().addProperty("id", this.getId());
        
        return jsonObj.getAsJsonObject();
    }
    
    public String getProvFreeForm() {
        return provFreeForm;
    }

    public void setProvFreeForm(String provFreeForm) {
        this.provFreeForm = provFreeForm;
    }

    public void copyVariableMetadata(Collection<VariableMetadata> vml) {

        for (VariableMetadata vm : vml) {
            VariableMetadata vmNew = new VariableMetadata(vm.getDataVariable(), this);
            vmNew.setIsweightvar(vm.isIsweightvar());
            vmNew.setWeighted(vm.isWeighted());
            vmNew.setWeightvariable(vm.getWeightvariable());
            vmNew.setInterviewinstruction(vm.getInterviewinstruction());
            vmNew.setLabel(vm.getLabel());
            vmNew.setLiteralquestion(vm.getLiteralquestion());
            vmNew.setNotes(vm.getNotes());
            vmNew.setUniverse(vm.getUniverse());

            Collection<CategoryMetadata> cms = vm.getCategoriesMetadata();
            for (CategoryMetadata cm : cms) {
                CategoryMetadata cmNew = new CategoryMetadata(vmNew, cm.getCategory());
                cmNew.setWfreq(cm.getWfreq());
                vmNew.getCategoriesMetadata().add(cmNew);
            }
            if (variableMetadatas == null) {
                variableMetadatas = new ArrayList<VariableMetadata>();
            }
            variableMetadatas.add(vmNew);
        }
    }

    public void copyVarGroups(Collection<VarGroup> vgl) {

        for (VarGroup vg : vgl) {
            VarGroup vgNew = new VarGroup(this, vg.getVarsInGroup());
            vgNew.setLabel(vg.getLabel());
            if (varGroups == null) {
                varGroups = new ArrayList<VarGroup>();
            }
            varGroups.add(vgNew);
        }

    }
    
    public Set<ConstraintViolation> validate() {
        Set<ConstraintViolation> returnSet = new HashSet<>();

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<FileMetadata>> constraintViolations = validator.validate(this);
        if (constraintViolations.size() > 0) {
            // currently only support one message
            ConstraintViolation<FileMetadata> violation = constraintViolations.iterator().next();
            String message = "Constraint violation found in FileMetadata. "
                    + violation.getMessage() + " "
                    + "The invalid value is \"" + violation.getInvalidValue().toString() + "\".";
            logger.info(message);
            returnSet.add(violation);
        }

        return returnSet;
    }
    
}
