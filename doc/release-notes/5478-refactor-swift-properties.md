Now all Swift properties have been migrated to `domain.xml`, no longer needing to maintain a separate
`swift.properties` file, and offering better governability and performance. Furthermore, now the Swift
credential's password is stored using `create-password-alias`, which encrypts the password so that it does
not appear in plain text on `domain.xml`.

In order to migrate to these new configuration settings, please visit 
`doc/sphinx-guides/source/installation/config.rst#swift-storage`.