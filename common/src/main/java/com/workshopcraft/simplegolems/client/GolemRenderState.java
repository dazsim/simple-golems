package com.workshopcraft.simplegolems.client;

import com.workshopcraft.simplegolems.entity.Job;
import com.workshopcraft.simplegolems.entity.Trouble;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class GolemRenderState extends LivingEntityRenderState {
    public final ItemStackRenderState tool = new ItemStackRenderState();
    public float attackTime;
    public Job job = Job.BLANK;
    public int colour = 0xFFFFFF;
    public boolean powered;
    public boolean waiting;
    public boolean carrying;
    public final ItemStackRenderState carried = new ItemStackRenderState();
    public Trouble trouble = Trouble.NONE;
    public float bubble;
}
