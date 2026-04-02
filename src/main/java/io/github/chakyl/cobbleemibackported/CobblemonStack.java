package io.github.chakyl.cobbleemibackported;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.render.models.blockbench.PoseableEntityState;
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonFloatingState;
import com.cobblemon.mod.common.entity.PoseType;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;


import com.cobblemon.mod.common.pokemon.Species;
import java.util.List;
import java.util.Set;

import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.util.math.QuaternionUtilsKt;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CobblemonStack extends EmiStack {

    private final Species specie;
    private final PoseableEntityState state = new PokemonFloatingState();
    public CobblemonStack(Species specie)
    {
        this.specie = specie;
    }

    @Override
    public EmiStack copy() {
        return new CobblemonStack(specie);
    }

    @Override
    public void render(GuiGraphics draw, int x, int y, float delta, int flags) {
        PoseStack pose = draw.pose();
        pose.pushPose();
        pose.translate(x+8,y-4, 1f);

        PokemonGuiUtilsKt.drawProfilePokemon(specie.getResourceIdentifier(), (Set<String>) specie.getStandardForm().getAspects(),
                pose,
                QuaternionUtilsKt.fromEulerXYZDegrees(new Quaternionf(), new Vector3f(13F, 35F, 0F)),
                state,
                delta,12f
        );
        pose.popPose();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public CompoundTag getNbt() {
        return null;
    }

    @Override
    public Object getKey() {
        return specie.getResourceIdentifier();
    }

    @Override
    public ResourceLocation getId() {
        return specie.getResourceIdentifier();
    }

    @Override
    public List<ClientTooltipComponent> getTooltip() {
        List<ClientTooltipComponent> list = Lists.newArrayList();
        list.addAll(getTooltipText().stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).toList());
        return list;
    }

    @Override
    public List<Component> getTooltipText() {
        return List.of(specie.getTranslatedName());
    }

    @Override
    public Component getName() {
        return specie.getTranslatedName();
    }
}
