# This is the main manifest.
#
# Each of the installed components can run on their own server as well. We just put them all here so we can run a
# complete development server:
# virtual local host: 'iqss::database', 'iqss::dataverse', 'iqss::solr', 'iqss::rserve' and 'iqss::tworavens':
#
# In production you can deploy per server per component. E.g.:
# Server A: 'iqss::dataverse'
# Server B: 'iqss::database'
# Server C: 'iqss::solr'
# Server D: 'iqss::tworavens'
# Server E: 'iqss::rserve'
#
#
# Global packages
# ---------------
# These are dependencies the puppet modules need and just assume there there.

package {
  ['unzip']:
    ensure => installed,
}

class {
# The global settings
  'iqss::globals':
    require => Package['unzip'],
    ensure  => present;
}->class {
  [
    'iqss::database',
    'iqss::dataverse',
    'iqss::solr',
    'iqss::tworavens'
  ]:
}

