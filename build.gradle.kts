plugins {
    id("java")
    id("maven-publish")
    id("com.gradleup.shadow") version("8.3.9")
    id("io.github.revxrsal.zapper") version("1.0.2")
    id("io.freefair.lombok") version("8.11")
}

group = "com.playerpicked"
version = "3.0.0"

repositories {
    maven {
        name = "PlayerPicked"
        url = uri("https://repo.playerpicked.com/")
        credentials {
            username = project.findProperty("repoUsername") as String
            password = project.findProperty("repoPassword") as String
        }
    }

    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.playerpicked.com/releases")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("org.projectlombok:lombok:1.18.36")

    implementation("io.github.revxrsal:lamp.bukkit:4.0.0-rc.17") {
        exclude(module = "lamp.common")
        exclude(module = "lamp.brigadier")
    }

    zap("org.bstats:bstats-bukkit:3.0.2")
    zap("io.github.revxrsal:lamp.common:4.0.0-rc.17")
    zap("io.github.revxrsal:lamp.brigadier:4.0.0-rc.17")
    zap("com.github.Anon8281:UniversalScheduler:0.1.6")
    zap("it.sauronsoftware.cron4j:cron4j:2.2.5")

    implementation("dev.dejvokep:boosted-yaml:1.3.6")
    implementation("com.playerpicked:pickly-paper:1.0.1")

    compileOnly("me.clip:placeholderapi:2.11.6")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
    options.encoding = "UTF-8"
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(props)
    }
}

zapper {
    libsFolder = "libs"
    relocationPrefix = "com.playerpicked.scheduler"

    repositories {
        includeProjectRepositories()
    }

    relocate("com.playerpicked.pickly", "pickly")
    relocate("com.github.Anon8281.universalScheduler", "universalScheduler")
    relocate("it.sauronsoftware.cron4j", "cron4j")
}

val apiJar = tasks.register<Jar>("apiJar") {
    archiveBaseName.set("Scheduler")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())

    from(sourceSets.main.get().output) {
        include("com/playerpicked/scheduler/api/**")
    }
}

publishing {
    publications {
        create<MavenPublication>("apiJar") {
            artifact(apiJar.get()) {
                classifier = null
            }

            groupId = "com.playerpicked"
            artifactId = "Scheduler"
            version = project.version.toString()
        }
    }

    repositories {
        maven {
            name = "PlayerPicked"
            url = uri("https://repo.playerpicked.com/releases")
            credentials {
                username = project.findProperty("repoUsername") as String
                password = project.findProperty("repoPassword") as String
            }
        }
    }
}

tasks.register("deployApi") {
    dependsOn("apiJar", "publishApiJarPublicationToPlayerPickedRepository")
}