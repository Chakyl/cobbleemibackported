package io.github.chakyl.cobbleemibackported;

import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.datafixers.util.Either;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

import static com.teseting.common.cobbleemi.CobbleEmiPlugin.CLIENT;

public class CobblemonSpawnSpecificDropRecipe extends CobblemonDropRecipe{


    public CobblemonSpawnSpecificDropRecipe(Species specie, List<CobblemonItemStack> first, List<Component> second) {
        super(specie, "spawn_specific_drops");
        text.add(new ArrayList<>());
        numSlots = Math.max(numSlots, first.size());
        outputStack.addAll(first.stream().map(t -> t.stack).toList());
        int start = 0;
        while (start < first.size())
        {
            List<CobblemonItemStack> itemLine = first.subList(start, Math.min(start + 7, first.size()));
            text.get(text.size()).add(Either.left(itemLine));
            start += 7;
        }
        text.get(text.size() - 1).addAll( second.stream().flatMap(t -> CLIENT.font.split(t, getDisplayWidth() - 4).stream()).map(Either::<List<CobblemonItemStack>, FormattedCharSequence>right).toList());
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return com.teseting.common.cobbleemi.CobbleEmiPlugin.COBBLEMON_SPAWN_SPECIFIC_DROPS_RECIPE_CATEGORY;
    }
}
