import org.gradle.language.jvm.tasks.ProcessResources

// 1.20.1 nodes take their json from src/legacy/1.20.1 and drop the 26.x formats from src/main
fun ProcessResources.legacyResources(modId: String) {
    exclude("$modId.accesswidener", "META-INF/accesstransformer.cfg", "META-INF/neoforge.mods.toml")
    exclude("assets/$modId/items/**", "data/$modId/recipe/**", "data/$modId/tags/block/**", "data/$modId/tags/item/**", "data/$modId/tags/entity_type/**")
    filesMatching("pack.mcmeta") { filter(LegacyPackMeta::class.java) }
}
