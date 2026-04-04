package io.github.chakyl.cobbleemibackported;

import com.cobblemon.mod.common.api.conditional.RegistryLikeIdentifierCondition;
import com.cobblemon.mod.common.api.conditional.RegistryLikeTagCondition;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.evolution.Evolution;
import com.cobblemon.mod.common.api.pokemon.evolution.requirement.EvolutionRequirement;
import com.cobblemon.mod.common.pokemon.evolution.requirements.*;
import com.cobblemon.mod.common.pokemon.evolution.variants.BlockClickEvolution;
import com.cobblemon.mod.common.pokemon.evolution.variants.ItemInteractionEvolution;
import com.cobblemon.mod.common.pokemon.evolution.variants.TradeEvolution;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

import static io.github.chakyl.cobbleemibackported.CobbleEmiPlugin.CLIENT;
import static io.github.chakyl.cobbleemibackported.CobbleEmiPlugin.EVOLUTIONS_CATEGORY;


public class CobblemonEvolutionRecipe implements EmiRecipe {

    ResourceLocation id;
    Evolution evolution;

    List<EmiIngredient> input;
    List<EmiStack> output;
    List<CobblemonItemStack> sideOutput;
    List<EmiStack> allOutput;

    List<Component> text;
    List<List<Component>> tooltip;
    List<Integer> tooltipHeights;
    List<FormattedText> textSequence;


    public CobblemonEvolutionRecipe(CobblemonStack specie, Evolution evolution, int evolutionIndex) {
        super();

        this.evolution = evolution;

        this.tooltip = new ArrayList<>();
        this.tooltipHeights = new ArrayList<>();

        this.output = new ArrayList<>();
        this.id = specie.getCobbleEMIId().withPrefix("/");
        if (evolution.getResult().getSpecies() != null) {
            Set<String> newAspects = new HashSet<>(specie.getAspects());
            newAspects.addAll(evolution.getResult().getAspects());

            CobblemonStack output1 = new CobblemonStack(PokemonSpecies.INSTANCE.getByName(evolution.getResult().getSpecies()), newAspects);
            this.output.add(output1);
            this.id = this.id.withSuffix("/").withSuffix(output1.getId().getPath());
        }
        // Shedders not in 1.20.1
//        if (evolution.getShedder() != null && evolution.getShedder().getSpecies() != null) {
//            Set<String> newAspects = new HashSet<>(specie.getAspects());
//            newAspects.addAll(evolution.getShedder().getAspects());
//
//            CobblemonStack output2 = new CobblemonStack(PokemonSpecies.getByName(evolution.getShedder().getSpecies()), newAspects);
//            output.add(output2);
//            this.id = this.id.withSuffix("/").withSuffix(output2.getId().getPath());
//        }
        if (evolution.getResult().getForm() != null) {
            this.id = this.id.withSuffix("/").withSuffix(evolution.getResult().getForm());
        }
        this.id = this.id.withSuffix("/evolution").withSuffix(String.valueOf(evolutionIndex));
        this.input = new ArrayList<>();
        this.input.add(specie);

        this.text = new ArrayList<>();
        if (evolution instanceof TradeEvolution) {
            this.text.add(Component.translatable("gui.cobbleemi.evolution_requirement.trade"));
        } else if (evolution instanceof BlockClickEvolution blockClickEvolution) {
            var blockCondition = blockClickEvolution.getRequiredContext();
            if (blockCondition instanceof RegistryLikeTagCondition<Block> tagCondition) {
                this.text.add(Component.translatable("gui.cobbleemi.evolution_requirement.block_click", "#" + tagCondition.getTag().location()));
                if (CLIENT.getConnection() != null) {
                    var registry = CLIENT.getConnection().registryAccess().registry(Registries.BLOCK);
                    registry.flatMap(blocks -> blocks.getTag(tagCondition.getTag())).ifPresent(t -> {
                        tooltip.add(new ArrayList<>());
                        for (var entry : t) {
                            if (entry.unwrapKey().isPresent()) {
                                tooltip.get(tooltip.size() - 1).add(Component.literal(entry.unwrapKey().get().location().toString()));
                            }
                        }
                    });
                    tooltipHeights.add(CLIENT.font.wordWrapHeight("#" + tagCondition.getTag().location(), getDisplayWidth() - 4));
                }
            }
        } else if (evolution instanceof ItemInteractionEvolution itemInteractionEvolution) {
            MutableComponent component = Component.empty();
            var itemPredicate = itemInteractionEvolution.getRequiredContext();
            Stream<Item> items = BuiltInRegistries.ITEM.stream().filter(it -> itemPredicate.getItem().fits(it, BuiltInRegistries.ITEM));
//            item.forEach(e -> );
            addItemPredicateTooltip(component,items.toList());
            this.text.add(Component.translatable("gui.cobbleemi.evolution_requirement.item_interaction", component));

        }
        getText(evolution.getRequirements(), this.text);
        this.sideOutput = new ArrayList<>();
        this.textSequence = this.text.stream().flatMap(t -> CLIENT.font.getSplitter().splitLines(t, getDisplayWidth() - 4, Style.EMPTY).stream()).toList();
        if (CLIENT.getConnection() != null) {

//            for (var entry : evolution.getDrops().getEntries()) {
//                if (entry instanceof ItemDropEntry itemDrop) {
//
//                    var itemRegistry = CLIENT.getConnection().registryAccess().registry(Registries.ITEM);
//                    Item item;
//                    if (itemRegistry.isPresent() && (item = itemRegistry.get().get(itemDrop.getItem())) != null) {
//                        this.sideOutput.add(CobblemonItemStack.of(entry,EmiStack.of(item)));
//                    }
//                }
//            }
        }
        this.allOutput = new ArrayList<>(output);
        this.allOutput.addAll(sideOutput.stream().map(t -> t.stack).toList());
    }

    public void addItemPredicateTooltip(MutableComponent component, List<Item> itemPredicate) {
//        if (itemPredicate.count().max().isPresent() && itemPredicate.count().min().isPresent()) {
//            component.append(Component.translatable("gui.cobbleemi.evolution_requirement.item_predicates.min_max",itemPredicate.count().min().get(),  itemPredicate.count().max().get()));
//        }
//        else if (itemPredicate.count().max().isPresent())
//        {
//            component.append(Component.translatable("gui.cobbleemi.evolution_requirement.item_predicates.max", itemPredicate.count().max().get()));
//        }
//        else if (itemPredicate.count().min().isPresent())
//        {
//            component.append(Component.translatable("gui.cobbleemi.evolution_requirement.item_predicates.min", itemPredicate.count().min().get()));
//        }
        if (!itemPredicate.isEmpty())
        {
//            List<String> items = new ArrayList<>();

            component.append(Component.translatable("gui.cobbleemi.evolution_requirement.item_predicates.any_of"));
            ClientPacketListener clientPacketListener = CLIENT.getConnection();
//            item.forEach(e -> {
//
//            });
            for (Item item : itemPredicate)
            {
                if (item != null) {
                    component.append(item.getDescription());
                    if (clientPacketListener == null) {
                        continue;
                    }
                    this.input.add(EmiStack.of(item));
                }
            }
//            if (!items.isEmpty()) {
//            }
        }
//        if (!itemPredicate.components().alwaysMatches())
//        {
//            itemPredicate.components().asPatch().entrySet()
//        }
//
//        if (!itemPredicate.subPredicates().isEmpty())
//        {
//            component.append(" ").append(Component.translatable("gui.cobbleemi.evolution_requirement.item_predicates.all_of"));
//            for (var subPredicate : itemPredicate.subPredicates().entrySet())
//            {
//                    addItemPredicateTooltip(component, subPredicate.getValue());
//            }
//        }

    }

    public List<Component> getText(Collection<EvolutionRequirement> requirements, List<Component> textComponent) {
//        List<Component> textComponent = new ArrayList<>();
        for (EvolutionRequirement requirement : requirements) {
            if (requirement instanceof AnyRequirement anyRequirement) {
                textComponent.add(Component.translatable("gui.cobbleemi.evolution_requirement.anyOf"));
                getText(anyRequirement.getPossibilities(), textComponent);
            } else if (requirement instanceof AttackDefenceRatioRequirement attackDefenceRatioRequirement) {
                textComponent.add(Component.translatable(
                        "gui.cobbleemi.evolution_requirement.attack_defence_ratio",
                        attackDefenceRatioRequirement.getRatio().name()
                ));
            } else if (requirement instanceof BattleCriticalHitsRequirement battleCriticalHitsRequirement) {
                textComponent.add(Component.translatable(
                        "gui.cobbleemi.evolution_requirement.battle_critical_hits",
                        battleCriticalHitsRequirement.getAmount()
                ));
            } else if (requirement instanceof BiomeRequirement biomeRequirement) {

                if (biomeRequirement.getBiomeCondition() instanceof RegistryLikeTagCondition<Biome> tagCondition) {
                    textComponent.add(Component.translatable("gui.cobbleemi.evolution_requirement.biome"));
                    textComponent.add(Component.literal("#" + tagCondition.getTag().location()));
                    tooltip.add(new ArrayList<>());
                    ClientPacketListener clientPacketListener = CLIENT.getConnection();
                    if (clientPacketListener != null) {
                        var registry = clientPacketListener.registryAccess().registry(Registries.BIOME);
                        registry.flatMap(biomes -> biomes.getTag(tagCondition.getTag())).ifPresent(t -> {
                            for (var entry : t) {
                                if (entry.unwrapKey().isPresent()) {
                                    tooltip.get(tooltip.size() - 1).add(Component.literal(entry.unwrapKey().get().location().toString()));
                                }
                            }
                        });
                        tooltipHeights.add(CLIENT.font.wordWrapHeight(tagCondition.getTag().location().toString(), getDisplayWidth() - 4));
                    }
                } else if (biomeRequirement.getBiomeCondition() instanceof RegistryLikeIdentifierCondition<?> identifierCondition) {
                    textComponent.add(Component.translatable("gui.cobbleemi.evolution_requirement.biome", identifierCondition.getIdentifier().toString()));
                }
            } else if (requirement instanceof BlocksTraveledRequirement blocksTraveledRequirement) {
                textComponent.add(Component.translatable(
                        "gui.cobbleemi.evolution_requirement.blocks_traveled",
                        blocksTraveledRequirement.getAmount()
                ));
            } else if (requirement instanceof DamageTakenRequirement damageTakenRequirement) {
                textComponent.add(Component.translatable(
                        "gui.cobbleemi.evolution_requirement.damage_taken",
                        damageTakenRequirement.getAmount()
                ));
            } else if (requirement instanceof DefeatRequirement defeatRequirement) {
                textComponent.add(Component.translatable(
                        "gui.cobbleemi.evolution_requirement.defeat",
                        defeatRequirement.getAmount(),
                        defeatRequirement.getTarget().getSpecies()
                ));
            } else if (requirement instanceof FriendshipRequirement friendshipRequirement) {
                textComponent.add(Component.translatable(
                        "gui.cobbleemi.evolution_requirement.friendship",
                        friendshipRequirement.getAmount()
                ));
            } else if (requirement instanceof HeldItemRequirement heldItemRequirement) {

                MutableComponent component = Component.empty();
                var itemPredicate = heldItemRequirement.getItemCondition();

                Stream<Item> items = BuiltInRegistries.ITEM.stream().filter(it -> itemPredicate.getItem().fits(it, BuiltInRegistries.ITEM));
                addItemPredicateTooltip(component,items.toList());
                textComponent.add(Component.translatable("gui.cobbleemi.evolution_requirement.held_item", component));
            } else if (requirement instanceof LevelRequirement levelRequirement) {

                if (levelRequirement.getMaxLevel() == Integer.MAX_VALUE) {
                    textComponent.add(Component.translatable(
                            "gui.cobbleemi.evolution_requirement.level_min",
                            levelRequirement.getMinLevel()
                    ));

                } else if (levelRequirement.getMinLevel() == 1) {
                    textComponent.add(Component.translatable(
                            "gui.cobbleemi.evolution_requirement.level_max",
                            levelRequirement.getMaxLevel()
                    ));
                } else {
                    textComponent.add(Component.translatable(
                            "gui.cobbleemi.evolution_requirement.level",
                            levelRequirement.getMinLevel(),
                            levelRequirement.getMaxLevel()
                    ));
                }

            } else if (requirement instanceof MoveSetRequirement moveSetRequirement) {
                textComponent.add(Component.translatable("gui.cobbleemi.evolution_requirement.moveset",
                        moveSetRequirement.getMove().getName()));
            } else if (requirement instanceof MoveTypeRequirement moveTypeRequirement) {
                textComponent.add(Component.translatable(
                        "gui.cobbleemi.evolution_requirement.move_type",
                        moveTypeRequirement.getType().getName()
                ));
            } else if (requirement instanceof PartyMemberRequirement partyMemberRequirement) {
                if (partyMemberRequirement.getTarget().getSpecies() == null && partyMemberRequirement.getTarget().getForm() == null) {
                    // TODO: handle pokemon properties
                    textComponent.add(Component.translatable("gui.cobbleemi.evolution_requirement.pokemon_properties_unknown", partyMemberRequirement.getTarget().getOriginalString()));
                    CobbleEmiBackported.LOGGER.error("Unsupported PartyMemberRequirement property %s", partyMemberRequirement.getTarget().getOriginalString());
                    continue;
                }

                String targetString;

                if (partyMemberRequirement.getTarget().getForm() != null) {
                    targetString = partyMemberRequirement.getTarget().getForm() + " type pokemon";
                } else {
                    targetString = partyMemberRequirement.getTarget().getSpecies();
                }

                if (partyMemberRequirement.getContains()) {
                    textComponent.add(Component.translatable(
                            "gui.cobbleemi.evolution_requirement.party_member",
                            targetString
                    ));
                } else {
                    textComponent.add(Component.translatable(
                            "gui.cobbleemi.evolution_requirement.party_member.not",
                            targetString
                    ));
                }
            } else if (requirement instanceof PokemonPropertiesRequirement pokemonPropertiesRequirement) {
//                tooltip.add(Component.translatable("gui.cobbleemi.evolution_requirement.pokemon_properties",
//                pokemonPropertiesRequirement.getTarget().getForm()));
                // TODO: handle pokemon properties

                if (!pokemonPropertiesRequirement.getTarget().getAspects().isEmpty()) {
                    textComponent.add(Component.translatable("gui.cobbleemi.evolution_requirement.pokemon_properties_aspects",
                            String.join(", ", pokemonPropertiesRequirement.getTarget().getAspects())
                    ));
                    continue;
                }
                if (!pokemonPropertiesRequirement.getTarget().getCustomProperties().isEmpty()) {
                    for (var entry : pokemonPropertiesRequirement.getTarget().getCustomProperties()) {
                        textComponent.add(Component.translatable("gui.cobbleemi.evolution_requirement.pokemon_properties_custom",
                                entry.asString()
                        ));
                    }
                    continue;
                }
                if (pokemonPropertiesRequirement.getTarget().getNickname() != null) {
                    textComponent.add(Component.translatable("gui.cobbleemi.evolution_requirement.pokemon_properties_nickname",
                            pokemonPropertiesRequirement.getTarget().getNickname()
                    ));
                    continue;
                }
                if (pokemonPropertiesRequirement.getTarget().getNature() != null) {
                    textComponent.add(Component.translatable("gui.cobbleemi.evolution_requirement.pokemon_properties_nature",
                            pokemonPropertiesRequirement.getTarget().getNature()
                    ));
                    continue;
                }

                CobbleEmiBackported.LOGGER.error("Unknown pokemon properties requirement: %s defaulting to showing original string", pokemonPropertiesRequirement.getTarget().getOriginalString());
                textComponent.add(Component.translatable("gui.cobbleemi.evolution_requirement.pokemon_properties_unknown", pokemonPropertiesRequirement.getTarget().getOriginalString()));

            } else if (requirement instanceof PropertyRangeRequirement propertyRangeRequirement) {
                textComponent.add(Component.translatable(
                        "gui.cobbleemi.evolution_requirement.property_range",
                        propertyRangeRequirement.getFeature(),
                        propertyRangeRequirement.getRange().getFirst(),
                        propertyRangeRequirement.getRange().getLast()

                ));
            } else if (requirement instanceof RecoilRequirement recoilRequirement) {
                textComponent.add(Component.translatable(
                        "gui.cobbleemi.evolution_requirement.recoil",
                        recoilRequirement.getAmount()
                ));
            } else if (requirement instanceof StatCompareRequirement statCompareRequirement) {
                textComponent.add(Component.translatable(
                        "gui.cobbleemi.evolution_requirement.stat_compare",
                        statCompareRequirement.getHighStat(),
                        statCompareRequirement.getLowStat()
                ));
            } else if (requirement instanceof StatEqualRequirement statEqualRequirement) {
                textComponent.add(Component.translatable(
                        "gui.cobbleemi.evolution_requirement.stat_equal",
                        statEqualRequirement.getStatOne(),
                        statEqualRequirement.getStatTwo()
                ));
            } else if (requirement instanceof UseMoveRequirement useMoveRequirement) {
                textComponent.add(Component.translatable(
                        "gui.cobbleemi.evolution_requirement.use_move",
                        useMoveRequirement.getMove().getName(),
                        useMoveRequirement.getAmount()
                ));
            }
        }
        return textComponent;
    }


    @Override
    public EmiRecipeCategory getCategory() {
        return EVOLUTIONS_CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return input;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return allOutput;
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
        int y = 4;
        int tooltipIndex = 0;
        for (int i = 0; i < this.textSequence.size(); i++) {
            if (this.textSequence.get(i).getString().charAt(0) == '#') {
                widgets.addTooltipText(tooltip.get(tooltipIndex), 2, y, getDisplayWidth(), tooltipHeights.get(tooltipIndex));
            }
            widgets.addText(Language.getInstance().getVisualOrder(this.textSequence.get(i)), 4, y, 0, false);
            y += CLIENT.font.lineHeight;
        }

        y += 4;
        widgets.addSlot(input.get(0), 18, y);
        for (int i = 0; i < output.size(); i++) {
            widgets.addSlot(output.get(i), widgets.getWidth() - 36, y + i * 18).recipeContext(this);
        }
        widgets.addFillingArrow(widgets.getWidth() / 2 - 9, y, 1500);
        y += 18;
//        widgets.addTooltipText(this.tooltip,widgets.getWidth()/2-9,  50, 24,17);
        if (this.input.size() > 1) {
            for (int i = 1; i < input.size(); i++) {
                SlotWidget slot = new SlotWidget(input.get(i), widgets.getWidth() / 2 - input.size() / 2 * 18 + i * 18 - 9, y + 4);
                if (evolution.getConsumeHeldItem()) {
                    slot.appendTooltip(Component.translatable("gui.cobbleemi.evolution_requirement.consume_held_item"));
                }
                widgets.add(slot);
            }
            y += 20;
        }
        y += 4;
        if (!evolution.getLearnableMoves().isEmpty()) {
            widgets.addText(Component.translatable("gui.cobbleemi.evolution.learnable_move"), 4, y, 0, false);
            y += CLIENT.font.lineHeight;
            for (var move : evolution.getLearnableMoves()) {
                widgets.addText(Component.literal(move.getName()), 4, y, 0, false);
                y += CLIENT.font.lineHeight;
            }
        }

        if (!sideOutput.isEmpty()) {
            int x = getDisplayWidth() / 2 - 9 - (sideOutput.size() - 1) * 18 / 2;
            int y2 = y;
            for (int i = 0; i < sideOutput.size(); i++) {
                SlotWidget slot = new SlotWidget(sideOutput.get(i).stack, x + i * 18, y2).recipeContext(this);
                CobblemonItemStack cobblemonItemStack = sideOutput.get(i);
                slot.appendTooltip(
                        Component.translatable("gui.cobbleemi.spawninfo.drop_chance", cobblemonItemStack.getPercentage())
                );
                if (cobblemonItemStack.getQuantityRange() == null) {
                    slot.appendTooltip(
                            Component.translatable("gui.cobbleemi.spawninfo.quantity", cobblemonItemStack.getQuantity())
                    );
                } else {
                    slot.appendTooltip(

                            Component.translatable("gui.cobbleemi.spawninfo.quantity_range", cobblemonItemStack.getQuantityRange().getStart(), cobblemonItemStack.getQuantityRange().getLast())

                    );
                }
                slot.appendTooltip(

                        Component.translatable("gui.cobbleemi.spawninfo.maxtimeschosen", cobblemonItemStack.getMaxSelectableTimes())
                );
                widgets.add(slot);
                slot.recipeContext(this);
            }
        }
    }
}
