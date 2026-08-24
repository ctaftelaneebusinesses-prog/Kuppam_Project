# OneTownCity Android

This is a Trusted Web Activity wrapper around the production site at
`https://onetowncity.com/`. Django remains the source of truth for routing,
authentication, location, listings, uploads, and dynamic data.

## Local build

Install Android Studio or the Android command-line tools with API 36 and a
compatible JDK. From this directory:

```bash
gradle bundleDebug
```

For a release bundle, provide signing values through environment variables or
`~/.gradle/gradle.properties` (never commit them):

```text
ONETOWNCITY_STORE_FILE=/absolute/path/to/upload-key.jks
ONETOWNCITY_STORE_PASSWORD=...
ONETOWNCITY_KEY_ALIAS=...
ONETOWNCITY_KEY_PASSWORD=...
```

Then run:

```bash
gradle bundleRelease
```

If signing values are omitted, Gradle can configure the release variant but the
result is not suitable for Google Play upload. Use Play App Signing and keep
the upload keystore outside the repository.

## Digital Asset Links

After the first signed release key exists, publish the generated SHA-256
certificate fingerprint in:

```text
https://onetowncity.com/.well-known/assetlinks.json
```

Use the exact package name `com.onetowncity.app`. The fingerprint is
intentionally not committed because it depends on the real signing key.
Without this verification file, Android safely opens the URL in a verified
browser context rather than granting full TWA verification.

## Release checklist

- Set the production `DATABASE_URL`, `SECRET_KEY`, `ALLOWED_HOSTS`, and
  `DEBUG=False` for the web deployment.
- Deploy the web app over HTTPS before building the release bundle.
- Configure the real signing key through private CI or Gradle properties.
- Publish `assetlinks.json` after obtaining the signing certificate fingerprint.
- Upload `app/build/outputs/bundle/release/app-release.aab` to Play Console.
