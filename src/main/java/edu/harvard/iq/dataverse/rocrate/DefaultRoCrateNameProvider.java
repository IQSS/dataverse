package edu.harvard.iq.dataverse.rocrate;

import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;

import java.util.stream.Collectors;

/**
 * Helps to generate a value for the "name" field of RO-Crate objects for a Dataverse compound field. This
 * RoCrateNameProvider implementation genetates a string value similar to the Metadata tab of the Dataverse dataset
 * UI.
 *
 * This class has been extracted from the ARP project (https://science-research-data.hu/en) in the frame of
 * FAIR-IMPACT's 1st Open Call "Enabling FAIR Signposting and RO-Crate for content/metadata discovery and consumption".
 *
 * @author Balázs E. Pataki <balazs.pataki@sztaki.hu>, SZTAKI, Department of Distributed Systems, https://dsd.sztaki.hu
 * @author Norbert Finta <norbert.finta@sztaki.hu>, SZTAKI, Department of Distributed Systems, https://dsd.sztaki.hu
 * @version 1.0
 */
@Stateless
@Named
public class DefaultRoCrateNameProvider implements RoCrateNameProvider
{
    @Override
    public String generateRoCrateName(DatasetFieldCompoundValue compoundValue)
    {
        var nameFieldValue = compoundValue.getDisplayValueMap().entrySet().stream()
                .map(o -> o.getValue())
                .collect(Collectors.joining(" "));
        nameFieldValue = (nameFieldValue.length() > 80) ? nameFieldValue.substring(0, 77) + "..." : nameFieldValue;

        return nameFieldValue;
    }
}
