package io.github.chakyl.cobbleemibackported;

import com.cobblemon.mod.common.api.drop.DropEntry;
import com.cobblemon.mod.common.api.drop.ItemDropEntry;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.datafixers.util.Either;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

import static com.teseting.common.cobbleemi.CobbleEmiPlugin.CLIENT;

public class CobblemonGeneralDropRecipe extends CobblemonDropRecipe{

    public CobblemonGeneralDropRecipe(Species specie) {
        super(specie, "general_drops");

        List<CobblemonItemStack> line = new ArrayList<>();
        text.add(new ArrayList<>());
        ClientPacketListener clientPacketListener = CLIENT.getConnection();
        if (clientPacketListener == null)
        {
            return;
        }

        for (DropEntry drop : specie.getDrops().getEntries())
        {
            if (drop instanceof ItemDropEntry itemDrop)
            {
                Item item = clientPacketListener.registryAccess().registryOrThrow(Registries.ITEM).get(itemDrop.getItem());
                if (item == null) {
                    continue;
                }
                outputStack.add(EmiStack.of(item));
                line.add(CobblemonItemStack.of(drop, EmiStack.of(item)));
                if (line.size() >= 7) {
                    text.get(text.size()).add(Either.left(line));
                    line = new ArrayList<>();
                }
            }
        }
        if (!line.isEmpty()) {
            text.get(text.size()-1).add(Either.left(line));
        }
        numSlots = Math.max(numSlots, outputStack.size());
    }
    @Override
    public EmiRecipeCategory getCategory() {
        return com.teseting.common.cobbleemi.CobbleEmiPlugin.COBBLEMON_GENERAL_DROPS_CATEGORY;
    }
}
