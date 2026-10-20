package com.workshopcraft.simplegolems.item;

import com.workshopcraft.simplegolems.entity.Job;
import net.minecraft.world.item.Item;

public class CardItem extends Item {
    public final Job job;

    public CardItem(Job job, Properties properties) {
        super(properties);
        this.job = job;
    }
}
