package edu.harvard.iq.dataverse.locality;

import edu.harvard.iq.dataverse.DvObject;
import java.io.Serializable;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/**
 * Future use, maybe. Once we're happy with the design we'll enable it as an
 * entity.
 *
 * TODO: Think more about what problems we're solving, what need to persist and
 * why.
 */
//@Entity
public class DvObjectStorageLocation implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(nullable = false)
    private DvObject dvObject;

    @OneToOne
    @JoinColumn(nullable = false)
    private StorageSite storageSite;

    private String storageLocationAddress;

    /**
     * See "primary" on the StorageSite object, which we are using instead.
     *
     * TODO: Consider deleting this field if we don't need it.
     */
    private Boolean primaryLocation;

}
