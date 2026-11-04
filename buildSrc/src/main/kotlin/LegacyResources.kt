import org.gradle.language.jvm.tasks.ProcessResources

// 1.20.1 nodes take their json from src/eras/1.20.1 and drop the files 1.20.1 cannot read from src/main
fun ProcessResources.legacyResources(modId: String) {
    exclude("$modId.accesswidener", "META-INF/accesstransformer.cfg", "META-INF/neoforge.mods.toml")
    exclude("data/$modId/tags/block/**", "data/$modId/tags/item/**", "data/$modId/tags/entity_type/**")
}
