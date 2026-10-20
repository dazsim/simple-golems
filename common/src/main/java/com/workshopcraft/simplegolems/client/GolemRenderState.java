package com.workshopcraft.simplegolems.client;

import com.workshopcraft.simplegolems.entity.Job;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;

public class GolemRenderState extends ArmedEntityRenderState {
    public Job job = Job.BLANK;
    public int colour = 0xFFFFFF;
    public boolean powered;
    public boolean carrying;
}
