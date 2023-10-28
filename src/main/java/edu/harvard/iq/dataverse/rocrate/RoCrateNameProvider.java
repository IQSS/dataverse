package edu.harvard.iq.dataverse.rocrate;

import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;

/**
 * Helps to generate a value for the "name" field of RO-Crate objects for a Dataverse compound field.
 *
 * This class has been extracted from the ARP project (https://science-research-data.hu/en) in the frame of
 * FAIR-IMPACT's 1st Open Call "Enabling FAIR Signposting and RO-Crate for content/metadata discovery and consumption".
 *
 * @author Balázs E. Pataki <balazs.pataki@sztaki.hu>, SZTAKI, Department of Distributed Systems, https://dsd.sztaki.hu
 * @author Norbert Finta <norbert.finta@sztaki.hu>, SZTAKI, Department of Distributed Systems, https://dsd.sztaki.hu
 * @version 1.0
 */
public interface RoCrateNameProvider
{
    String generateRoCrateName(DatasetFieldCompoundValue compoundField);
}
