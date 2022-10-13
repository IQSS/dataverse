Fix the year displayed in citation for harvested dataset, specialy for oai_dc format.

For normal datasets, the date used is the "citation date" which is by default the publication date (the first release date) (https://guides.dataverse.org/en/latest/api/native-api.html?highlight=citationdate#set-citation-date-field-type-for-a-dataset).

But for a harvested dataset, the distribution date is used instead and this date is not always present in the harvested metadata. With oai_dc format the date tag if used as production date.

Now, the production date is used for harvested dataset in addition to distribution date.