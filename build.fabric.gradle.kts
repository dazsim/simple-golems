plugins {
    id("dev.kikugie.loom-back-compat")
}

// -Pversion=1.1.4-alpha.1 overrides mod.version for test jars
val modVersion = project.version.toString().takeIf { it != "unspecified" } ?: sc.properties.get<String>("mod.version")
val modId = sc.properties.get<String>("mod.id")
val modName = sc.properties.get<String>("mod.name")

version = modVersion
group = sc.properties.get<String>("mod.group")
base.archivesName = "$modId-fabric-${sc.current.version}"

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

// json whose format changed between versions, written per era by tools/gen_resources.py and tools/gen_eras.py; 26.3 reads the 26.1 files
val era = when {
    sc.current.parsed >= "26.1" -> "26.1"
    sc.current.parsed >= "1.21" -> "1.21.1"
    else -> "1.20.1"
}
// the access widener only opens 26.x members
val modern = sc.current.parsed >= "26.1"
val legacy = sc.current.parsed < "1.20.5"
sourceSets.main { resources.srcDir(rootProject.file("src/eras/$era")) }
fun packProp(key: String) = runCatching { sc.properties.get<String>(key) }.getOrDefault("")

dependencies {
    if (!modern) compileOnly("org.jspecify:jspecify:1.0.0")
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${sc.properties.get<String>("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
    if (modern) accessWidenerPath = rootProject.file("src/main/resources/$modId.accesswidener")
    runConfigs.all {
        runDirectory = rootProject.file("runs/${sc.current.project}")
    }
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
            "loader" to sc.properties.get<String>("deps.fabric_loader"),
            "java" to requiredJava.majorVersion,
            "pack_format" to packProp("mod.pack_format"),
            "pack_min" to packProp("mod.pack_min"),
            "pack_max" to packProp("mod.pack_max"),
        )
        inputs.properties(props)
        filesMatching(listOf("fabric.mod.json", "pack.mcmeta")) { expand(props) }
        exclude("META-INF/neoforge.mods.toml", "META-INF/mods.toml")
        if (!modern) {
            filesMatching("fabric.mod.json") { filter(StripAccessWidener::class.java) }
            exclude("$modId.accesswidener", "META-INF/accesstransformer.cfg")
        }
        if (legacy) legacyResources(modId)
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
            "Implementation-Title" to "fabric",
            "Implementation-Version" to modVersion,
            "Implementation-Vendor" to sc.properties.get<String>("mod.author"),
            "Built-On-Minecraft" to sc.current.version,
        )
    }
}
