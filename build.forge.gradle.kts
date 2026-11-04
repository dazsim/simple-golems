plugins {
    id("net.neoforged.moddev.legacyforge")
    id("neoforge-mutex")
}

// -Pversion=1.1.4-alpha.1 overrides mod.version for test jars
val modVersion = project.version.toString().takeIf { it != "unspecified" } ?: sc.properties.get<String>("mod.version")
val modId = sc.properties.get<String>("mod.id")
val modName = sc.properties.get<String>("mod.name")

version = modVersion
group = sc.properties.get<String>("mod.group")
base.archivesName = "$modId-forge-${sc.current.version}"

sourceSets.main { resources.srcDir(rootProject.file("src/legacy/1.20.1")) }

legacyForge {
    version = sc.properties.get<String>("deps.forge")
    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
    runs {
        register("client") {
            gameDirectory = rootProject.file("runs/${sc.current.project}")
            client()
        }
        register("server") {
            gameDirectory = rootProject.file("runs/${sc.current.project}-server")
            server()
        }
    }
}

dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")
}

java {
    withSourcesJar()
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

tasks {
    processResources {
        val props = mapOf(
            "id" to modId,
            "name" to modName,
            "version" to modVersion,
            "author" to sc.properties.get<String>("mod.author"),
            "license" to sc.properties.get<String>("mod.license"),
            "description" to sc.properties.get<String>("mod.description"),
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
        )
        inputs.properties(props)
        filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) { expand(props) }
        exclude("fabric.mod.json")
        legacyResources(modId)
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    withType<Jar> {
        val name = sc.properties.get<String>("mod.name")
        inputs.property("mod_name", name)
        from(rootProject.file("LICENSE")) { rename { "${it}_$name" } }
    }

    jar {
        manifest.attributes(
            "Specification-Title" to modName,
            "Specification-Vendor" to sc.properties.get<String>("mod.author"),
            "Specification-Version" to modVersion,
            "Implementation-Title" to "forge",
            "Implementation-Version" to modVersion,
            "Implementation-Vendor" to sc.properties.get<String>("mod.author"),
            "Built-On-Minecraft" to sc.current.version,
        )
    }
}
