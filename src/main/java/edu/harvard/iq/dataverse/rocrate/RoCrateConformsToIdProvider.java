package edu.harvard.iq.dataverse.rocrate;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.kit.datamanager.ro_crate.entities.data.RootDataEntity;

import java.util.List;

/**
 * Manages RO-Crate conformsTo values.
 *
 * This class has been extracted from the ARP project (https://science-research-data.hu/en) in the frame of
 * FAIR-IMPACT's 1st Open Call "Enabling FAIR Signposting and RO-Crate for content/metadata discovery and consumption".
 *
 * @author Balázs E. Pataki <balazs.pataki@sztaki.hu>, SZTAKI, Department of Distributed Systems, https://dsd.sztaki.hu
 * @author Norbert Finta <norbert.finta@sztaki.hu>, SZTAKI, Department of Distributed Systems, https://dsd.sztaki.hu
 * @version 1.0
 */

public interface RoCrateConformsToIdProvider
{
    List<String> generateConformsToIds(Dataset dataset, RootDataEntity rootDataEntity);

    List<MetadataBlock> findMetadataBlockForConformsToIds(List<String> ids);
}
