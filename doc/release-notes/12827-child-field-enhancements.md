### Child Field Display Enhancements

The display of metadata child fields (subfields within compound fields) has been significantly improved:

- **Multiple child field values** are now properly displayed, supporting repeatable child fields.
- **Child field formatting** now allows the field name to be displayed once followed by multiple values, using the `#NAME` and `#VALUE` displayFormat variables (e.g., `<b>#NAME:</b> #VALUE`).
- **Bold formatting** is now available for child fields of any type (text, string, int, float, email, etc.) using the `<b>` HTML tag in displayFormat.
- **Line breaks** can now be used in displayFormat via the `<br/>` tag to display child fields on separate lines.
- **TEXTBOX fields** with multiple values (such as the citation "Related Materials" field) now display each value on a separate line rather than separated by semicolons.
- **Internationalization** is now supported for child controlled-vocabulary fields.
- **Email fields** in child fields can now be displayed with obfuscated addresses (e.g., `support:[at]dataverse[dot]org`) when the displayFormat includes `#EMAIL` with other text (e.g., `#NAME: #EMAIL`). When `#EMAIL` is used alone, the field remains hidden as before.
- **Long controlled vocabulary values** in child fields now wrap properly in the edit pane, avoiding overlap with adjacent fields.

See #12827.
