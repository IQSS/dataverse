### video subtitles (vtt files)

The `IQSS/dataverse` PR sets the content type for new(!) files with extension `vtt` to `text/vtt`
what is presented as "_Web Video Text Tracks_". The PR also enables full text indexing for these files,
if [configured](https://guides.dataverse.org/en/latest/installation/config.html#solrfulltextindexing).

The `gdcc/dataverse-previewer` PRs provide a new version of the video previewer. 
The new previewer version presents `vtt` files as subtitles for videos,
the naming convention is `<video-basename>.<language-tag>.vtt`.
The previewer does not rely on the content type.
A proper content type may hint users to ask permission for the subtitles together with a video.

Existing files with extension `vtt` will keep content type `application/octet-stream` presented as "_Unknown_".
The following query shows the number of files per extension with an "_Unknown_" content type:

      SELECT substring(m.label from (length(label) - strpos(reverse(m.label), '.') + 2)) AS extension, COUNT(*) as count
      FROM datafile f LEFT JOIN filemetadata m ON f.id = m.datafile_id
      WHERE f.contenttype = 'application/octet-stream'
      GROUP BY extension;

If `vtt` does not appear in the result, you are done. 
Otherwise, you may want to update the content type for existing files and reindex those datasets.

First figure out which datasets would need [reindexing](https://guides.dataverse.org/en/latest/admin/solr-search-index.html#manual-reindexing):

      select distinct
        o.protocol, o.authority, o.identifier,
        v.versionnumber, v.minorversionnumber, v.versionstate
        from      datafile       f
        left join filemetadata   m on f.id = m.datafile_id
        left join datasetversion v on v.id = m.datasetversion_id
        left join dvobject       o on o.id = v.dataset_id
        WHERE contenttype = 'application/octet-stream' 
        AND 'vtt' = substring(m.label from (length(label) - strpos(reverse(m.label), '.') + 2))
        ;

Then update the content type for the files:

      UPDATE datafile SET contenttype = 'text/vtt' WHERE id IN (
        SELECT datafile_id FROM filemetadata m
        WHERE contenttype = 'application/octet-stream' 
        AND 'vtt' = substring(m.label from (length(label) - strpos(reverse(m.label), '.') + 2))
      );