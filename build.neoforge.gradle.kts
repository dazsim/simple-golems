plugins {
    id("net.neoforged.moddev")
    id("neoforge-mutex")
}

// -Pversion=1.1.4-alpha.1 overrides mod.version for test jars
val modVersion = project.version.toString().takeIf { it != "unspecified" } ?: sc.properties.get<String>("mod.version")
val modId = sc.properties.get<String>("mod.id")
val modName = sc.properties.get<String>("mod.name")

version = modVersion
group = sc.properties.get<String>("mod.group")
base.archivesName = "$modId-neoforge-${sc.current.version}"

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

// json whose format changed between versions, written per era by tools/gen_resources.py and tools/gen_eras.py; 26.3 reads the 26.1 files
val era = when {
    sc.current.parsed >= "26.1" -> "26.1"
    sc.current.parsed >= "1.21" -> "1.21.1"
    else -> "1.20.1"
}
// the access transformer only opens 26.x members
val modern = sc.current.parsed >= "26.1"
val legacy = sc.current.parsed < "1.20.5"
sourceSets.main { resources.srcDir(rootProject.file("src/eras/$era")) }
val exDeorum = sc.current.parsed < "26.3"
fun packProp(key: String) = runCatching { sc.properties.get<String>(key) }.getOrDefault("")

neoForge {
    version = sc.properties.get<String>("deps.neo_loader")
    if (modern) accessTransformers.from(rootProject.file("src/main/resources/META-INF/accesstransformer.cfg"))
    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
    runs {
        configureEach {
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        register("client") {
            gameDirectory = rootProject.file("runs/${sc.current.project}")
            client()
        }
        register("server") {
            gameDirectory = rootProject.file("runs/${sc.current.project}-server")
            server()
        }
        register("data") {
            gameDirectory = rootProject.file("runs/${sc.current.project}-data")
            clientData()
            programArguments.addAll("--mod", modId, "--all", "--output", rootProject.file("src/generated/resources").absolutePath, "--existing", rootProject.file("src/main/resources").absolutePath)
        }
    }
}

sourceSets.main { resources.srcDir(rootProject.file("src/generated/resources")) }

// Ex Deorum is optional: the sifter and hammer programs use it when it is installed
repositories {
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") }
        filter { includeGroup("maven.modrinth") }
    }
    exclusiveContent {
        forRepository { maven("https://cursemaven.com") }
        filter { includeGroup("curse.maven") }
    }
}

dependencies {
    // no Ex Deorum build for 26.3 yet
    if (exDeorum) compileOnly(sc.properties.get<String>("deps.exdeorum"))
    if (!modern) compileOnly("org.jspecify:jspecify:1.0.0")
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain.languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
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
            "neoforge" to sc.properties.get<String>("deps.neo_compat"),
            "fml" to sc.properties.get<String>("deps.neo_fml_compat"),
            "exdeorum" to packProp("deps.exdeorum_range"),
            "logo_key" to if (sc.current.parsed >= "26.3") "iconFile" else "logoFile",
            "pack_format" to packProp("mod.pack_format"),
            "pack_min" to packProp("mod.pack_min"),
            "pack_max" to packProp("mod.pack_max"),
        )
        inputs.properties(props)
        filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta")) { expand(props) }
        exclude("fabric.mod.json", "META-INF/mods.toml")
        if (!modern) exclude("$modId.accesswidener", "META-INF/accesstransformer.cfg")
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
            "Implementation-Title" to "neoforge",
            "Implementation-Version" to modVersion,
            "Implementation-Vendor" to sc.properties.get<String>("mod.author"),
            "Built-On-Minecraft" to sc.current.version,
        )
    }
}
