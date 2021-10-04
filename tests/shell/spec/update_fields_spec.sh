#shellcheck shell=sh

update_fields() {
  ../../conf/solr/8.8.1/update-fields.sh "$@"
}

Describe "Update fields command"
  Path schema-xml="../../conf/solr/8.8.1/schema.xml"
  It "needs upstream schema.xml"
    The path schema-xml should be exist
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
  End

  Describe "reading input"
    It "throws error when no source given"
      When run update_fields data/solr/minimal.xml
      The status should equal 2
      The error should include "provide source file or piped input"
    End

    It "throws error when given file not found"
      When run update_fields data/solr/minimal.xml foobar
      The status should equal 2
      The error should include "Cannot read"
      The error should include "foobar"
    End

    It "throws error when given file is empty"
      When run update_fields data/solr/minimal.xml data/solr/empty.xml
      The status should equal 2
      The error should include "Cannot read"
      The error should include "empty.xml"
    End

    It "throws error when given invalid source file"
      When run update_fields data/solr/minimal.xml data/solr/invalid-source.md
      The status should equal 2
      The error should include "No <field> or <copyField>"
    End

    It "throws error when given invalid stdin source"
      Data < data/solr/invalid-source.md
      When run update_fields data/solr/minimal.xml
      The status should equal 2
      The error should include "No <field> or <copyField>"
    End
  End
End