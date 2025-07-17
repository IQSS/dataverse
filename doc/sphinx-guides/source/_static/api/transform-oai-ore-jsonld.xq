declare option output:method "json";

let $parameters:={ 'method': 'json' }
for $record in /json
  let $metadata:=$record/ore_003adescribes
  

  let $json:=
    <json type="object">
      {$metadata/*}
      {$record/_0040context}
    </json>


  return if ($metadata) then
      file:write("converted.json",$json, $parameters)