UPDATE termsofuseandaccess SET license_id = (SELECT license.id FROM license WHERE license.name = 'CC0 1.0'), termsofuse = NULL
WHERE termsofuse = 'This dataset is made available under a Creative Commons CC0 license with the following additional/modified terms and conditions: CC0 Waiver'
  AND license_id IS null
  AND confidentialitydeclaration IS null
  AND specialpermissions IS null
  AND restrictions IS null
  AND citationrequirements IS null
  AND depositorrequirements IS null
  AND conditions IS null
  AND disclaimer IS null;
