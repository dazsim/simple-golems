plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1.2-fabric"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    properties {
        tags(version, loader)
    }

    constants {
        match(loader, "fabric", "neoforge", "forge")
    }

    replacements.string(current.parsed < "1.21.11") {
        replace("Identifier", "ResourceLocation")
        replace("entity.animal.golem.AbstractGolem", "entity.animal.AbstractGolem")
        replace("entity.animal.sheep.Sheep", "entity.animal.Sheep")
        replace("entity.npc.villager.AbstractVillager", "entity.npc.AbstractVillager")
    }

    replacements.string(current.parsed < "1.20.5") {
        replace("PathType.", "BlockPathTypes.")
        replace("BlockTags.SUBSTRATE_OVERWORLD", "BlockTags.DIRT")
    }
}
