#shellcheck shell=sh

update_fields() {
  ../../conf/solr/9.3.0/update-fields.sh "$@"
}

Describe "Update fields command"

  Describe "can operate on upstream data"
    copyUpstreamSchema() { cp ../../conf/solr/9.3.0/schema.xml data/solr/upstream-schema.xml; }
    AfterAll 'copyUpstreamSchema'

    Path schema-xml="../../conf/solr/9.3.0/schema.xml"
    It "needs upstream schema.xml"
      The path schema-xml should be exist
    End
  End

  Describe "schema.xml validation"
    It "throws error when no schema.xml target given"
      When run update_fields
      The status should equal 2
      The error should include "Cannot find or write"
    End

    Describe "throws error when missing mark"
      Parameters
        "#1" "SCHEMA-FIELDS::BEGIN" data/solr/mark-missing-field-start.xml
        "#2" "SCHEMA-FIELDS::END" data/solr/mark-missing-field-end.xml
        "#3" "SCHEMA-COPY-FIELDS::BEGIN" data/solr/mark-missing-copyfield-start.xml
        "#4" "SCHEMA-COPY-FIELDS::END" data/solr/mark-missing-copyfield-end.xml
      End

      It "$2"
        When run update_fields "$3"
        The status should equal 2
        The error should include "$2"
      End
    End

    Describe "throws error when non-unique mark"
      Parameters
        "#1" "SCHEMA-FIELDS::BEGIN" data/solr/mark-nonunique-field-start.xml
        "#2" "SCHEMA-FIELDS::END" data/solr/mark-nonunique-field-end.xml
        "#3" "SCHEMA-COPY-FIELDS::BEGIN" data/solr/mark-nonunique-copyfield-start.xml
        "#4" "SCHEMA-COPY-FIELDS::END" data/solr/mark-nonunique-copyfield-end.xml
      End

      It "$2"
        When run update_fields "$3"
        The status should equal 2
        The error should include "guards are not unique"
      End
    End

    Describe "throws error when marks not in correct order"
      Parameters
        "#1" data/solr/mark-order-1.xml
        "#2" data/solr/mark-order-2.xml
        "#3" data/solr/mark-order-3.xml
      End

      It "$1"
        When run update_fields "$2"
        The status should equal 2
        The error should include "guards are not in correct order"
      End
    End

    It "throws error when marks not in exclusive line"
      When run update_fields data/solr/mark-nolinebreak.xml
      The status should equal 2
      The error should include "is not on an exclusive line"
    End
  End

  Describe "reading input"
    Describe "fails because"
      # Test if $CI is set (always true inside Github Workflow)
      detect_github_action() { ! test -z ${CI:+x}; }

      It "throws error when no source given"
        Skip if "running on Github Action" detect_github_action
        When run update_fields data/solr/minimal-schema.xml
        The status should equal 2
        The error should include "provide source file or piped input"
      End

      It "throws error when given file not found"
        When run update_fields data/solr/minimal-schema.xml foobar
        The status should equal 2
        The error should include "Cannot read"
        The error should include "foobar"
      End

      It "throws error when given file is empty"
        When run update_fields data/solr/minimal-schema.xml data/solr/empty-source.xml
        The status should equal 2
        The error should include "Cannot read"
        The error should include "empty-source.xml"
      End

      It "throws error when given invalid source file"
        When run update_fields data/solr/minimal-schema.xml data/solr/invalid-source.xml
        The status should equal 2
        The error should include "No <field> or <copyField>"
      End

      It "throws error when given invalid stdin source"
        Data < data/solr/invalid-source.xml
        When run update_fields data/solr/minimal-schema.xml
        The status should equal 2
        The error should include "No <field> or <copyField>"
      End
    End

    Describe "succeeds because"
      setup() { cp data/solr/minimal-schema.xml data/solr/minimal-schema-work.xml; }
      cleanup() { rm data/solr/minimal-schema-work.xml; }
      BeforeEach 'setup'
      AfterEach 'cleanup'

      deleteUpstreamSchema() { rm data/solr/upstream-schema.xml; }
      AfterAll 'deleteUpstreamSchema'

      match_content() {
        grep -q "$@" "${match_content}"
      }

      It "prints nothing when editing minimal schema"
        Data < data/solr/minimal-source.xml
        When run update_fields data/solr/minimal-schema-work.xml
        The status should equal 0
        The output should equal ""
        The path data/solr/minimal-schema-work.xml should be file
        The path data/solr/minimal-schema-work.xml should satisfy match_content "<field name=\"test\""
        The path data/solr/minimal-schema-work.xml should satisfy match_content "<copyField source=\"test\""
      End

      It "prints nothing when editing upstream schema"
        Data < data/solr/minimal-source.xml
        When run update_fields data/solr/upstream-schema.xml
        The status should equal 0
        The output should equal ""
        The path data/solr/upstream-schema.xml should be file
        The path data/solr/upstream-schema.xml should satisfy match_content "<field name=\"test\""
        The path data/solr/upstream-schema.xml should satisfy match_content "<copyField source=\"test\""
      End
    End
  End

  Describe "chaining data"
    setup() { cp data/solr/minimal-schema.xml data/solr/minimal-schema-work.xml; }
    cleanup() { rm data/solr/minimal-schema-work.xml; }
    BeforeEach 'setup'
    AfterEach 'cleanup'

    match_content() {
      echo "${match_content}" | diff "$1" -
    }

    It "prints after editing"
      Data < data/solr/minimal-source.xml
      When run update_fields -p data/solr/minimal-schema-work.xml
      The status should equal 0
      The output should satisfy match_content data/solr/chain-output.xml
    End
  End
End
