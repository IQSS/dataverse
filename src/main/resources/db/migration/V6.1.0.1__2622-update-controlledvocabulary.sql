UPDATE controlledvocabularyvalue
SET strvalue = 
  CASE
    WHEN strvalue = 'LightCurve' THEN 'Light Curve'
    WHEN strvalue = 'EventList' THEN 'Event List'
    WHEN strvalue = 'PrettyPicture' THEN 'Pretty Picture'
    WHEN strvalue = 'ValuePair' THEN 'Value Pair'
    ELSE strvalue
  END;
