
/**
 * Author:  skraffmi
 * Created: Jan 3, 2020
 per 6345 add commas between geographic metadata items
 */

update datasetfieldtype set displayformat = '#VALUE, '
where name in ('country', 'state', 'city');