-- Fix Text Spacing in Astronomy Metadata Fields
UPDATE controlledvocabularyvalue cvv
SET strvalue =
  CASE
    WHEN cvv.strvalue = 'LightCurve' THEN 'Light Curve'
    WHEN cvv.strvalue = 'EventList' THEN 'Event List'
    WHEN cvv.strvalue = 'PrettyPicture' THEN 'Pretty Picture'
    WHEN cvv.strvalue = 'ValuePair' THEN 'Value Pair'
    ELSE cvv.strvalue
  END
FROM datasetfieldtype dft
WHERE dft.name = 'astroType'
 AND cvv.datasetfieldtype_id = dft.id;
