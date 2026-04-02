package io.github.chakyl.cobbleemibackported;

import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.datafixers.util.Either;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiResolutionRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.teseting.common.cobbleemi.CobbleEmiPlugin.CLIENT;

public abstract class CobblemonDropRecipe implements EmiRecipe {

    private final List<EmiIngredient> coblemonInputStack;
    protected List<EmiStack> outputStack  = new ArrayList<>();
    // list of entries of a list of lines which may be a line of text or a line of items
    protected List<List<Either<List<CobblemonItemStack>, FormattedCharSequence>>> text = new ArrayList<>();
    protected int numSlots = 0;

    protected ResourceLocation id;

    protected CobblemonDropRecipe(Species specie, String suffix)
    {
        this.id = specie.getResourceIdentifier().withPrefix("/").withSuffix("/"+suffix);
        this.coblemonInputStack = List.of(new CobblemonStack(specie));

    }

    protected EmiIngredient getIngredient() {
        return this.coblemonInputStack.get(0);
    }


    @Override
    public @Nullable ResourceLocation getId() {
        return this.id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return this.coblemonInputStack;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputStack;
    }

    @Override
    public int getDisplayWidth() {
        return 144;
    }

    @Override
    public int getDisplayHeight() {
        return 130;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
//        widgets.addText(Component.literal(this.cobblemonName), 18, 4, 0, false);
        widgets.addSlot(coblemonInputStack.get(0), 72-18/2, 0);

        int lineCount = (widgets.getHeight()) / CLIENT.font.lineHeight / 2;
        PageManager manager = new PageManager(text, lineCount, (EmiStack) coblemonInputStack.get(0));
        if (text.size() > 1 || (!text.isEmpty() && text.get(0).size() > lineCount)) {
            widgets.addButton(2, 2, 12, 12, 0, 0, () -> true, (mouseX, mouseY, button) -> {
                manager.scroll(-1);
            });
            widgets.addButton(widgets.getWidth() - 14, 2, 12, 12, 12, 0, () -> true, (mouseX, mouseY, button) -> {
                manager.scroll(1);
            });
        }


        widgets.addDrawable(0, 2, 0, 0, (raw, mouseX, mouseY, delta) -> {
            int lo = manager.start();
            int itemY = 18;
            for (Either<List<CobblemonItemStack>, FormattedCharSequence> line : manager.lines.get(manager.entryIndex).subList(Math.min(lo, manager.lines.get(manager.entryIndex).size()), Math.min(lo + manager.pageSize, manager.lines.get(manager.entryIndex).size()))) {
                if (line.left().isPresent()) {
                    itemY += 18;
                } else {
                    if (line.right().isEmpty()) continue;
                    FormattedCharSequence textLine = line.right().get();
                    raw.drawString(CLIENT.font, textLine, 2, itemY, -1);
                    itemY += 11;
                }
            }

        });
        for (int i = 0; i < numSlots; i++) {
            int x = 2 + (i % 7) * 18;
            int y = 18 + (i / 7) * 18;
            widgets.add(new PageSlotWidget(manager, i, x, y));
        }


    }
    private static class PageManager {
        public final List<List<Either<List<CobblemonItemStack>, FormattedCharSequence>>> lines;
        public final int pageSize;
        public int currentPage;
        public int totalPages = 0;
        public int entryIndex = 0;
        public final List<Integer> entryPageIndex = new ArrayList<>();
        public EmiStack outputStack;

        public PageManager(List<List<Either<List<CobblemonItemStack>, FormattedCharSequence>>> lines, int pageSize, EmiStack outputStack) {
            this.lines = lines;
            this.pageSize = pageSize;
            this.outputStack = outputStack;
            int pages = 0;
            for (List<Either<List<CobblemonItemStack>, FormattedCharSequence>> entry : lines) {
                entryPageIndex.add(pages);
                pages += (entry.size() - 1) / pageSize + 1;
            }
            this.totalPages = pages;
        }

        public void scroll(int delta) {
            currentPage += delta;
            if (currentPage < 0) {
                currentPage = totalPages - 1;
                entryIndex = entryPageIndex.size() - 1;
            }
            if (currentPage >= totalPages) {
                currentPage = 0;
                entryIndex = 0;
            }
            while (entryIndex < entryPageIndex.size()-1 && currentPage >= entryPageIndex.get(entryIndex+1)) {
                entryIndex++;
            }
            while (entryIndex > 0 && currentPage < entryPageIndex.get(entryIndex)) {
                entryIndex--;
            }
        }
        // line start index of current page
        public int start() {
            return (currentPage -  entryPageIndex.get(entryIndex)) * pageSize;
        }
        public CobblemonItemStack getStack(int offset) {
            int lineIndex = offset / 7;
            int itemIndex = offset % 7;
            if (lineIndex >= lines.get(entryIndex).size()) {
                return new CobblemonItemStack();
            }
            if (currentPage != entryPageIndex.get(entryIndex))
            {
                return new CobblemonItemStack();
            }
            Either<List<CobblemonItemStack>, FormattedCharSequence> line = lines.get(entryIndex).get(lineIndex);
            if (line.left().isPresent() && !line.left().get().isEmpty()) {
                return line.left().get().get(itemIndex);
            }
            return new CobblemonItemStack();
        }
        public EmiRecipe getRecipe(int offset) {
            return new EmiResolutionRecipe(getStack(offset).stack, outputStack);
        }
    }
    private class PageSlotWidget extends SlotWidget {
        public final PageManager manager;
        public final int offset;

        public PageSlotWidget(PageManager manager, int offset, int x, int y) {
            super(EmiStack.EMPTY, x, y);
            this.manager = manager;
            this.offset = offset;
        }

        @Override
        public EmiIngredient getStack() {
            return manager.getStack(offset).stack;
        }

        @Override
        public EmiRecipe getRecipe() {
            return manager.getRecipe(offset);
        }


        @Override
        public void render(GuiGraphics draw, int mouseX, int mouseY, float delta) {
            if (!getStack().isEmpty()) {
                super.render(draw, mouseX, mouseY, delta);
            }
        }

        @Override
        public void drawBackground(GuiGraphics draw, int mouseX, int mouseY, float delta) {
            super.drawBackground(draw, mouseX, mouseY, delta);
        }
        @Override
        public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
            List<ClientTooltipComponent> tooltipComponentList =  super.getTooltip(mouseX, mouseY);
            CobblemonItemStack cobblemonItemStack = manager.getStack(offset);

            tooltipComponentList.add(
                    ClientTooltipComponent.create(
                            Component.translatable("gui.cobbleemi.spawninfo.drop_chance", cobblemonItemStack.getPercentage()).getVisualOrderText()
                    )
            );
            if (cobblemonItemStack.getQuantityRange()  == null ) {
                tooltipComponentList.add(
                        ClientTooltipComponent.create(
                                Component.translatable("gui.cobbleemi.spawninfo.quantity", cobblemonItemStack.getQuantity()).getVisualOrderText()

                        )
                );
            }
            else
            {
                tooltipComponentList.add(
                        ClientTooltipComponent.create(
                                Component.translatable("gui.cobbleemi.spawninfo.quantity_range", cobblemonItemStack.getQuantityRange().getStart(), cobblemonItemStack.getQuantityRange().getLast()).getVisualOrderText()

                        )
                );
            }
            tooltipComponentList.add(
                    ClientTooltipComponent.create(
                            Component.translatable("gui.cobbleemi.spawninfo.maxtimeschosen", cobblemonItemStack.getMaxSelectableTimes()).getVisualOrderText()

                    )
            );
            return tooltipComponentList;
        }
    }
}
