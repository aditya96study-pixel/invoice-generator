# Release signing key — keep safe

`amitabh-release.jks` signs the app. Android only installs an update over the existing app if it is signed with the **same** key, so:

- Keep a copy of this folder somewhere safe (e.g. a USB stick or private cloud folder).
- Never share it publicly. `signing.properties` contains its password.
- If it is lost, the app can still be rebuilt, but the phone must uninstall the old app first (which deletes its data).
