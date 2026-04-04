package io.github.chakyl.cobbleemibackported;

import com.cobblemon.mod.common.client.render.models.blockbench.PoseableEntityState;
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonFloatingState;
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonPoseableModel;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.FormData;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.emi.emi.api.stack.EmiStack;
import io.github.chakyl.cobbleemibackported.CobbleEmiBackported;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;


import com.cobblemon.mod.common.pokemon.Species;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.util.math.QuaternionUtilsKt;
import org.joml.Quaternionf;
import org.joml.Vector3f;


public class CobblemonStack extends EmiStack {

    private final Species specie;
    Set<String> aspects = new HashSet<>();

    String aspectsString = "";

    private final PoseableEntityState state = new PokemonFloatingState();
    public CobblemonStack(Species specie)
    {
        this.specie = specie;
    }

    public CobblemonStack(FormData formData)
    {
        this.specie = formData.getSpecies();
        this.setAspects(new HashSet<>(formData.getAspects()));
    }

    public CobblemonStack(Species specie, Set<String> aspects)
    {
        this.specie = specie;
        this.setAspects(aspects);
    }

    public Species getSpecie() {
        return specie;
    }

    public Set<String> getAspects()
    {
        return aspects;
    }

    public void setAspects(Set<String> aspects) {
        this.aspects = aspects;
        if (!aspects.isEmpty()) {
            this.aspectsString = "-"+aspects.stream().sorted().collect(Collectors.joining("-")).toLowerCase().replace("?","question").replace("!","exclamation").replaceAll("[^a-z0-9/._-]", "");
        }
    }


    @Override
    public EmiStack copy() {
        return new CobblemonStack(specie, aspects);
    }

    @Override
    public void render(GuiGraphics draw, int x, int y, float delta, int flags) {
        PoseStack pose = draw.pose();
        pose.pushPose();
        pose.translate(x+8,y-4, 1f);
        PokemonGuiUtilsKt.drawProfilePokemon(specie.getResourceIdentifier(), this.getAspects(),
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
        return getId().toString();
    }

    @Override
    public ResourceLocation getId() {
        return specie.getResourceIdentifier().withSuffix(aspectsString);
    }

    public ResourceLocation getCobbleEMIId() {
        return ResourceLocation.fromNamespaceAndPath(CobbleEmiBackported.MODID, getId().getPath());
    }

    @Override
    public List<ClientTooltipComponent> getTooltip() {
        List<ClientTooltipComponent> list = Lists.newArrayList();
        list.addAll(getTooltipText().stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).toList());
        return list;
    }

    @Override
    public List<Component> getTooltipText() {
        return List.of(getName());
    }

    @Override
    public Component getName() {
        FormData form = specie.getForm(aspects);
        if (form.equals(specie.getStandardForm()) || form.getName().equals("Normal")) {
            return specie.getTranslatedName();
        }
        else
        {
              return Component.literal(form.getName()).append(" ").append(specie.getTranslatedName());
        }
    }
}
