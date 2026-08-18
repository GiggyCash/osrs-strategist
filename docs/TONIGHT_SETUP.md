# Tonight: First Setup on Fedora

Do this in order. The goal tonight is not to finish the plugin. The goal is to get a clean development copy launching in RuneLite.

## 1. Install the basics
Open Terminal:

```bash
sudo dnf install git java-11-openjdk-devel
```

Check Java:

```bash
java -version
javac -version
```

The RuneLite example-plugin currently targets Java 11 bytecode.

## 2. Install IntelliJ IDEA Community
Use Fedora Software / your preferred package source and install **IntelliJ IDEA Community Edition**.

## 3. Create a GitHub account
If you do not already have one, create one. You will need it later for source control and Plugin Hub submission.

## 4. Create your repository
On GitHub create a public repository:

`osrs-strategist`

Do not worry about making it perfect.

## 5. Clone RuneLite's official example-plugin template
In Terminal:

```bash
cd ~
git clone https://github.com/runelite/example-plugin.git osrs-strategist
cd osrs-strategist
```

This gives you RuneLite's current Gradle wrapper and Plugin Hub project layout.

## 6. Copy this starter pack into that folder
From this ZIP, copy these over the cloned template:
- `src/`
- `build.gradle`
- `settings.gradle`
- `runelite-plugin.properties`
- `README.md`
- `docs/`
- `scripts/`

Keep the template's `gradlew`, `gradlew.bat`, and `gradle/wrapper/` files.

## 7. Test the build
From the project folder:

```bash
chmod +x gradlew
./gradlew clean test
```

Do not continue if this fails. Save the full error and send it to ChatGPT.

## 8. Open the folder in IntelliJ
- Open IntelliJ
- Open `~/osrs-strategist`
- Let Gradle finish importing
- Make sure IntelliJ uses a JDK that can build the project

## 9. Run the development client
The provided Gradle task is set up for RuneLite developer mode.

```bash
./gradlew run
```

If your Jagex Account requires a different development-login flow, use RuneLite's current Jagex Account developer guide rather than changing plugin code.

## 10. Confirm the plugin appears
In the development RuneLite client:
- search settings/plugins for **Gielinor Compass**
- enable it if needed
- log into a test account

At v0.2-dev the entry point is intentionally minimal. We are verifying the project before adding account readers and the strategy engine.

## 11. Initialize your own Git history
The clone contains RuneLite example-plugin history. The simplest clean start is:

```bash
rm -rf .git
git init
git add .
git commit -m "Initial Gielinor Compass foundation"
git branch -M main
git remote add origin https://github.com/YOUR_GITHUB_NAME/osrs-strategist.git
git push -u origin main
```

Replace `YOUR_GITHUB_NAME`.

## Stop point for tonight
If `./gradlew clean test` passes and `./gradlew run` launches RuneLite with Gielinor Compass visible, tonight was successful.

Do not start adding random code yet. Send the result back and we will add the first real subsystem in a controlled order.
