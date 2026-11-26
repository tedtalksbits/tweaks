// Development-only plugin: checks Tweaks against a real Paper server. Never install it on a live server.
plugins { java }
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}
dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.211-stable")
    compileOnly(project(":tweaks-plugin"))
}
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
