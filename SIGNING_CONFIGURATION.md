# Local Android signing configuration

Copy `keystore.properties.example` to `keystore.properties` locally and supply your own signing configuration. Keep the properties file and signing keystore outside version control.

The repository ignores `keystore.properties`, `.jks`, `.keystore`, `.p12`, and `.pfx` files. The example file contains placeholders only.

If the former signing password was used with a real signing key, change the keystore and key passwords locally. The historical properties file is excluded from the sanitized publication history.
