package io.github.chakyl.cobbleemibackported;

import com.cobblemon.mod.common.api.drop.DropEntry;
import com.cobblemon.mod.common.api.drop.ItemDropEntry;
import dev.emi.emi.api.stack.EmiStack;
import kotlin.ranges.IntRange;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class CobblemonItemStack extends ItemDropEntry {
    public EmiStack stack = EmiStack.EMPTY;

    public ResourceLocation item;
    public int maxSelectableTimes;
    public CompoundTag nbt;
    public float percentage;
    public int quantity;
    public IntRange quantityRange;

    public static CobblemonItemStack of(DropEntry dropEntry, EmiStack emiStack) {
        if (dropEntry instanceof ItemDropEntry itemDropEntry) {
            CobblemonItemStack cobblemonItemStack = new CobblemonItemStack();

            cobblemonItemStack.item = itemDropEntry.getItem();
            cobblemonItemStack.percentage = itemDropEntry.getPercentage();
            cobblemonItemStack.quantity = itemDropEntry.getQuantity();
            cobblemonItemStack.quantityRange = itemDropEntry.getQuantityRange();
            cobblemonItemStack.maxSelectableTimes =itemDropEntry.getMaxSelectableTimes();
            cobblemonItemStack.stack = emiStack;
            return cobblemonItemStack;
        }
        return null;
    }
}