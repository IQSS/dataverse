package edu.harvard.iq.dataverse.datavariable;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Index;
import javax.persistence.UniqueConstraint;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;

import edu.harvard.iq.dataverse.FileMetadata;

@Entity
@Table(indexes = {@Index(columnList="datavariable_id"), @Index(columnList="filemetadata_id"),
                  @Index(columnList="datavariable_id,filemetadata_id")},
        uniqueConstraints={@UniqueConstraint(columnNames={"datavariable_id", "filemetadata_id"})})
public class VariableMetadata implements Serializable  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * dataVariable: DataVariable to which this metadata belongs.
     */

    @ManyToOne
    @JoinColumn(nullable=false)
    private DataVariable dataVariable;

    /*
     * fileMetadta: FileMetadata to which this metadata belongs.
     */

    @ManyToOne
    @JoinColumn(nullable=false)
    private FileMetadata fileMetadata;

    /*
     * label: variable label.
     */

    @Column(columnDefinition="TEXT")
    private String label;

    /*
     * literalquestion: literal question, metadata variable field.
     */

    @Column(columnDefinition="TEXT")
    private String literalquestion;

    /*
     * interviewinstruction: Interview Instruction, metadata variable field.
     */

    @Column(columnDefinition="TEXT")
    private String interviewinstruction;

    /*
     * universe: metadata variable field.
     */

    private String universe;

    /*
     * notes: notes, metadata variable field (CDATA).
     */

    @Column(columnDefinition="TEXT")
    private String notes;

    /*
     * isweightvar: It defines if variable is a weight variable
     */

    private boolean isweightvar = false;

    /*
     * weighted: It defines if variable is weighted
     */

    private boolean weighted = false;

    /*
     * dataVariable: DataVariable with which this variable is weighted.
     */

    private DataVariable weightvariable;

    public VariableMetadata () {

    }

    public VariableMetadata (DataVariable dataVariable, FileMetadata fileMetadata) {
        this.dataVariable = dataVariable;
        this.fileMetadata = fileMetadata;
    }

    /*
     * Getter and Setter functions:
     */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FileMetadata getFileMetadata() {
        return fileMetadata;
    }

    public void setFileMetadata(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
    }

    public void setDataVariable(DataVariable dataVariable) {
        this.dataVariable = dataVariable;
    }

    public DataVariable getDataVariable() {
        return dataVariable;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLiteralquestion() {
        return this.literalquestion;
    }

    public void setLiteralquestion(String literalquestion) {
        this.literalquestion = literalquestion;
    }

    public String getInterviewinstruction() {
        return this.interviewinstruction;
    }

    public void setInterviewinstruction(String interviewinstruction) {
        this.interviewinstruction = interviewinstruction;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getNotes() { return notes; }

    public String getUniverse() { return this.universe; }

    public void setUniverse(String universe) {
        this.universe = universe;
    }

    public void setIsweightvar(boolean isweightvar) {
        this.isweightvar = isweightvar;
    }

    public boolean isIsweightvar() {
        return isweightvar;
    }

    public DataVariable getWeightvariable() { return this.weightvariable; }

    public void setWeightvariable(DataVariable weightvariable) { this.weightvariable = weightvariable; }

    public void setWeighted(boolean weighted) {
        this.weighted = weighted;
    }

    public boolean isWeighted() {
        return weighted;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof VariableMetadata)) {
            return false;
        }

        VariableMetadata other = (VariableMetadata)object;
        if (this.id != other.id ) {
            if (this.id == null || !this.id.equals(other.id)) {
                return false;
            }
        }
        return true;
    }
}
