## Contributing

Contributions, bug reports, and feature requests are welcome.

1. [Open an issue](https://github.com/jhaiian/CSFC/issues) to report a bug or suggest a feature
2. Fork the repo and create a branch for your change
3. Submit a pull request with a clear description

---

## Building from Source

### Prerequisites
- Android Studio or JDK 17
- Android SDK (API 36)
- Gradle 9.4.1

### Steps

```bash
git clone https://github.com/jhaiian/CSFC.git
cd CSFC
```

Create a `local.properties` file in the root with your SDK path:

```properties
sdk.dir=/path/to/your/android/sdk
```

For a signed release build, also add:

```properties
signingConfig.storeFile=app/release_keystore.jks
signingConfig.storePassword=your_password
signingConfig.keyAlias=your_alias
signingConfig.keyPassword=your_password
```

Then build:

```bash
chmod +x gradlew
./gradlew assembleRelease
```

APKs will be output to `app/build/outputs/apk/release/`.

---

## Project Structure

```
CSFC/
├── .github/
│   └── workflows/
│       ├── build.yml
│       └── release.yml
├── app/src/main/
│   ├── res/
│   │   ├── drawable/                 # Vector icons and adaptive icon foreground
│   │   ├── layout/                   # Activity layouts
│   │   ├── mipmap-anydpi-v26/        # Adaptive launcher icon
│   │   └── values/                   # strings.xml, themes.xml, colors.xml
│   └── java/com/jhaiian/csfc/
│       ├── app/
│       │   └── CsfcApplication.kt    # Application class
│       └── MainActivity.kt           # Entry point activity
├── Update/
│   ├── Stable.json                   # Stable channel update manifest
│   └── Beta.json                     # Beta channel update manifest
├── fastlane/
│   └── metadata/android/en-US/
│       ├── images/
│       │   └── icon.png
│       ├── full_description.txt
│       ├── short_description.txt
│       └── title.txt
├── docs/
│   └── icon.png
├── CHANGELOG.md
├── Contributing.md
├── Contributors.md
├── LICENSE
├── PRIVACY_POLICY.md
├── README.md
└── TERMS_OF_SERVICE.md
```

## CI/CD Secrets

In order to make the release workflow work, you need the following secrets:

**Secret 1: `BASE_64_SIGNING_KEY`**

```bash
# Convert your keystore to base64
base64 -w 0 your_keystore.jks
# Copy the entire output as the secret value
```

**Secret 2: `LOCAL_PROPERTIES`**

```properties
signingConfig.storeFile=app/release_keystore.jks
signingConfig.storePassword=your_password
signingConfig.keyAlias=your_alias
signingConfig.keyPassword=your_password
```

To make your `release.yml` workflow work, set up the following **secrets** in your repository:

| Secret Name               | Purpose                                                        |
|----------------------------|----------------------------------------------------------------|
| `BASE_64_SIGNING_KEY`      | Encoded release keystore for signing APKs.                    |
| `LOCAL_PROPERTIES`         | Contents of your `local.properties` for SDK path and signing configs. |
| `GIT_USERNAME`             | Your GitHub username for automated commits.                   |
| `GIT_EMAIL`                | Your GitHub email for automated commits.                      |
| `PERSONAL_GITHUB_TOKEN`    | GitHub Personal Access Token (PAT) for pushing commits/tags.  |

---

## How to create a Personal Access Token (PAT)

Your workflow needs a GitHub token to push commits and tags. Follow these steps:

1. Go to **GitHub Settings → Developer settings → Personal Access Tokens → Tokens (classic)**.
2. Click **Generate new token → Generate new token (classic)**.
3. Give the token a name (e.g., `CSFC CI`).
4. Set an expiration (recommended: 90 days or no expiration if you rotate it regularly).
5. Under **Scopes**, check:
   - `repo` → Full control of private repositories
6. Click **Generate token**.
7. Copy the token immediately and add it as the secret `PERSONAL_GITHUB_TOKEN` in your repository.
