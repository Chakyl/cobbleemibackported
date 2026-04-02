//package io.github.chakyl.cobbleemibackported;
//
//import com.cobblemon.mod.common.api.conditional.RegistryLikeIdentifierCondition;
//import com.cobblemon.mod.common.api.conditional.RegistryLikeTagCondition;
//import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
//import com.cobblemon.mod.common.api.pokemon.evolution.Evolution;
//import com.cobblemon.mod.common.api.pokemon.evolution.requirement.EvolutionRequirement;
//import com.cobblemon.mod.common.pokemon.Pokemon;
//import com.cobblemon.mod.common.pokemon.evolution.predicate.NbtItemPredicate;
//import com.cobblemon.mod.common.pokemon.evolution.variants.BlockClickEvolution;
//import com.cobblemon.mod.common.pokemon.evolution.variants.ItemInteractionEvolution;
//import com.cobblemon.mod.common.pokemon.evolution.variants.TradeEvolution;
//import com.cobblemon.mod.common.pokemon.evolution.requirements.*;
//import com.cobblemon.mod.common.registry.ItemTagCondition;
//import dev.emi.emi.api.recipe.EmiRecipe;
//import dev.emi.emi.api.recipe.EmiRecipeCategory;
//import dev.emi.emi.api.stack.EmiIngredient;
//import dev.emi.emi.api.stack.EmiStack;
//import dev.emi.emi.api.widget.WidgetHolder;
//import net.minecraft.advancements.critereon.ItemPredicate;
//import net.minecraft.advancements.critereon.NbtPredicate;
//import net.minecraft.client.multiplayer.ClientPacketListener;
//import net.minecraft.core.HolderSet;
//import net.minecraft.core.registries.Registries;
//import net.minecraft.network.chat.Component;
//import net.minecraft.network.chat.MutableComponent;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.level.block.Block;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.*;
//
//import static com.teseting.common.cobbleemi.CobbleEmiPlugin.*;
//
//public class CobblemonEvolutionRecipe implements EmiRecipe {
//
//    ResourceLocation id;
//    List<EmiIngredient> input;
//
//    List<EmiStack> output;
//    List<Component> tooltip;
//
//
//    public CobblemonEvolutionRecipe(CobblemonStack specie, Evolution evolution, int evolutionIndex) {
//        super();
//
//        this.output = new ArrayList<>();
//        this.id = specie.getCobbleEMIId().withPrefix("/");
//        if (evolution.getResult().getSpecies() != null) {
//            Set<String> newAspects = new HashSet<>(specie.getAspects());
//            newAspects.addAll(evolution.getResult().getAspects());
//
//            CobblemonStack output1 = new CobblemonStack(PokemonSpecies.INSTANCE.getByName(evolution.getResult().getSpecies()),newAspects);
//            this.output.add(output1);
//            this.id = this.id.withSuffix("/").withSuffix(output1.getId().getPath());
//        }
//
//        if (evolution.getShedder() != null && evolution.getShedder().getSpecies() != null)
//        {
//            Set<String> newAspects = new HashSet<>(specie.getAspects());
//            newAspects.addAll(evolution.getShedder().getAspects());
//            CobblemonStack output2 = new CobblemonStack(PokemonSpecies.INSTANCE.getByName(evolution.getShedder().getSpecies()),newAspects);
//            output.add(output2);
//            this.id = this.id.withSuffix("/").withSuffix(output2.getId().getPath());
//        }
//        if (evolution.getResult().getForm() != null)
//        {
//            this.id = this.id.withSuffix("/").withSuffix(evolution.getResult().getForm());
//        }
//        this.id = this.id.withSuffix("/evolution").withSuffix(String.valueOf(evolutionIndex));
//        this.input = new ArrayList<>();
//        this.input.add(specie);
//
//        this.tooltip = new ArrayList<>();
//
//        switch (evolution) {
//            case TradeEvolution ignored ->
//                    this.tooltip.add(Component.translatable("gui.cobbleemi.evolution_requirement.trade"));
//            case BlockClickEvolution blockClickEvolution -> {
//                var blockCondition = blockClickEvolution.getRequiredContext();
//                if (blockCondition instanceof RegistryLikeTagCondition<Block> tagCondition) {
//                    this.tooltip.add(Component.translatable("gui.cobbleemi.evolution_requirement.block_click", "#" + tagCondition.getTag().location()));
//                } else if (blockCondition instanceof RegistryLikeIdentifierCondition<Block> identifierCondition) {
//                    this.tooltip.add(Component.translatable("gui.cobbleemi.evolution_requirement.block_click", identifierCondition.getIdentifier().toString()));
//                }
//            }
//            case ItemInteractionEvolution itemInteractionEvolution -> {
//                MutableComponent component = Component.empty();
//                var itemPredicate = itemInteractionEvolution.getRequiredContext();
//
//                addItemPredicateTooltip(component, itemPredicate);
//                this.tooltip.add(Component.translatable("gui.cobbleemi.evolution_requirement.item_interaction", component));
//            }
//            default -> {
//            }
//        }
//        this.tooltip.addAll(getTooltip(evolution.getRequirements()));
//    }
//
//    public void addItemPredicateTooltip(MutableComponent component, NbtItemPredicate itemPredicate) {
//
//        if (itemPredicate.getItem() instanceof RegistryLikeTagCondition<Item> tagCondition) {
//            component.append(Component.translatable("gui.cobbleemi.evolution_requirement.item_predicates.any_of", "#"+tagCondition.getTag()));
//
//            ClientPacketListener clientPacketListener = CLIENT.getConnection();
//            if (clientPacketListener != null) {
//                var registry = clientPacketListener.registryAccess().registry(Registries.ITEM);
//                Optional<HolderSet.Named<Item>> item;
//                if (registry.isPresent() && (item = registry.get().getTag(tagCondition.getTag())).isPresent()) {
//                    this.input.addAll(item.get().stream().map((holderSet)-> EmiStack.of(holderSet.value())).toList());
//                }
//            }
//        }
//        else if (itemPredicate.getItem() instanceof RegistryLikeIdentifierCondition<Item> identifierCondition) {
//            component.append(Component.translatable("gui.cobbleemi.evolution_requirement.item_predicates.any_of", identifierCondition.getIdentifier().toString()));
//            ClientPacketListener clientPacketListener = CLIENT.getConnection();
//            if (clientPacketListener != null) {
//                var registry = clientPacketListener.registryAccess().registry(Registries.ITEM);
//                 Item item;
//                if (registry.isPresent() && (item = registry.get().get(identifierCondition.getIdentifier())) != null) {
//                    this.input.add(EmiStack.of(item));
//                }
//            }
//        }
//
//
//
//        if (itemPredicate.getNbt() != null)
//        {
//            component.append(Component.translatable("gui.cobbleemi.evolution_requirement.item_predicates.tag", itemPredicate.getNbt().tag().toString()));
//
//        }
//
////        if (itemPredicate.count().max().isPresent() && itemPredicate.count().min().isPresent()) {
////            component.append(Component.translatable("gui.cobbleemi.evolution_requirement.item_predicates.min_max",itemPredicate.count().min().get(),  itemPredicate.count().max().get()));
////        }
////        else if (itemPredicate.count().max().isPresent())
////        {
////            component.append(Component.translatable("gui.cobbleemi.evolution_requirement.item_predicates.max", itemPredicate.count().max().get()));
////        }
////        else if (itemPredicate.count().min().isPresent())
////        {
////            component.append(Component.translatable("gui.cobbleemi.evolution_requirement.item_predicates.min", itemPredicate.count().min().get()));
////        }
////        if (itemPredicate.items().isPresent())
////        {
//////            List<String> items = new ArrayList<>();
////
////            component.append(Component.translatable("gui.cobbleemi.evolution_requirement.item_predicates.any_of"));
////            ClientPacketListener clientPacketListener = CLIENT.getConnection();
////
////            for (var item : itemPredicate.items().get())
////            {
////                if (item.unwrapKey().isPresent()) {
////                    component.append(item.unwrapKey().get().location().toString());
////                    if (clientPacketListener == null) {
////                        continue;
////                    }
////                    var registry = clientPacketListener.registryAccess().registry(Registries.ITEM);
////                    Item item1;
////                    if (registry.isPresent() && (item1 = registry.get().get(item.unwrapKey().get())) != null) {
////                        this.input.add(EmiStack.of(item1));
////                    }
////                }
////            }
////            if (!items.isEmpty()) {
////            }
//        }
////        if (!itemPredicate.components().alwaysMatches())
////        {
////            itemPredicate.components().asPatch().entrySet()
////        }
////
////        if (!itemPredicate.subPredicates().isEmpty())
////        {
////            component.append(" ").append(Component.translatable("gui.cobbleemi.evolution_requirement.item_predicates.all_of"));
////            for (var subPredicate : itemPredicate.subPredicates().entrySet())
////            {
////                    addItemPredicateTooltip(component, subPredicate.getValue());
////            }
////        }
//    public List<Component> getTooltip(Collection<EvolutionRequirement> requirements)
//    {
//        List<Component> tooltip = new ArrayList<>();
//        for (EvolutionRequirement requirement : requirements)
//        {
//            if (requirement instanceof AnyRequirement anyRequirement) {
//                tooltip.add(Component.translatable("gui.cobbleemi.evolution_requirement.anyOf"));
//                tooltip.addAll(getTooltip(anyRequirement.getPossibilities()));
//            }
//            else if (requirement instanceof AttackDefenceRatioRequirement attackDefenceRatioRequirement) {
//                tooltip.add(Component.translatable(
//                        "gui.cobbleemi.evolution_requirement.attack_defence_ratio",
//                        attackDefenceRatioRequirement.getRatio().name()
//                ));
//            }
//            else if (requirement instanceof BattleCriticalHitsRequirement battleCriticalHitsRequirement) {
//                tooltip.add(Component.translatable(
//                        "gui.cobbleemi.evolution_requirement.battle_critical_hits",
//                        battleCriticalHitsRequirement.getAmount()
//                ));
//            }
//            else if (requirement instanceof BiomeRequirement biomeRequirement) {
//
//                if (biomeRequirement.getBiomeCondition() instanceof  RegistryLikeTagCondition<?> tagCondition) {
//                    tooltip.add(Component.translatable("gui.cobbleemi.evolution_requirement.biome", "#"+ tagCondition.getTag().location()));
//                } else if (biomeRequirement.getBiomeCondition() instanceof RegistryLikeIdentifierCondition<?> identifierCondition) {
//                    tooltip.add(Component.translatable("gui.cobbleemi.evolution_requirement.biome", identifierCondition.getIdentifier().toString()));
//                }
//            }
//            else if (requirement instanceof BlocksTraveledRequirement blocksTraveledRequirement) {
//                tooltip.add(Component.translatable(
//                        "gui.cobbleemi.evolution_requirement.blocks_traveled",
//                        blocksTraveledRequirement.getAmount()
//                ));
//            }
//            else if (requirement instanceof DamageTakenRequirement damageTakenRequirement) {
//                tooltip.add(Component.translatable(
//                        "gui.cobbleemi.evolution_requirement.damage_taken",
//                        damageTakenRequirement.getAmount()
//                ));
//            }
//            else if (requirement instanceof DefeatRequirement defeatRequirement) {
//                tooltip.add(Component.translatable(
//                        "gui.cobbleemi.evolution_requirement.defeat",
//                        defeatRequirement.getAmount(),
//                        defeatRequirement.getTarget().getSpecies()
//                ));
//            }
//            else if (requirement instanceof FriendshipRequirement friendshipRequirement) {
//                tooltip.add(Component.translatable(
//                        "gui.cobbleemi.evolution_requirement.friendship",
//                        friendshipRequirement.getAmount()
//                ));
//            }
//            else if (requirement instanceof HeldItemRequirement heldItemRequirement) {
//
//                MutableComponent component = Component.empty();
//                var itemPredicate = heldItemRequirement.getItemCondition();
//
//                addItemPredicateTooltip(component, itemPredicate);
//                tooltip.add(Component.translatable("gui.cobbleemi.evolution_requirement.held_item", component));
//            }
//            else if (requirement instanceof LevelRequirement levelRequirement) {
//
//                if (levelRequirement.getMaxLevel() == Integer.MAX_VALUE)
//                {
//                    tooltip.add(Component.translatable(
//                            "gui.cobbleemi.evolution_requirement.level_min",
//                            levelRequirement.getMinLevel()
//                    ));
//
//                }
//                else if (levelRequirement.getMinLevel() == 1)
//                {
//                    tooltip.add(Component.translatable(
//                            "gui.cobbleemi.evolution_requirement.level_max",
//                            levelRequirement.getMaxLevel()
//                    ));
//                }
//                else {
//                    tooltip.add(Component.translatable(
//                            "gui.cobbleemi.evolution_requirement.level",
//                            levelRequirement.getMinLevel(),
//                            levelRequirement.getMaxLevel()
//                    ));
//                }
//
//            }
//            else if (requirement instanceof MoveSetRequirement moveSetRequirement) {
//                tooltip.add(Component.translatable("gui.cobbleemi.evolution_requirement.moveset",
//                moveSetRequirement.getMove().getName()));
//            }
//            else if (requirement instanceof MoveTypeRequirement moveTypeRequirement) {
//                tooltip.add(Component.translatable(
//                        "gui.cobbleemi.evolution_requirement.move_type",
//                        moveTypeRequirement.getType().getName()
//                ));
//            }
//            else if (requirement instanceof PartyMemberRequirement partyMemberRequirement) {
//                if (partyMemberRequirement.getTarget().getSpecies() == null && partyMemberRequirement.getTarget().getType() == null)
//                {
//                    // TODO: handle pokemon properties
//                    tooltip.add(Component.translatable("gui.cobbleemi.evolution_requirement.pokemon_properties_unknown", partyMemberRequirement.getTarget().getOriginalString()));
//                    LOGGER.error("Unsupported PartyMemberRequirement property %s", partyMemberRequirement.getTarget().getOriginalString());
//                    continue;
//                }
//
//                String targetString;
//
//                if (partyMemberRequirement.getTarget().getType() != null)
//                {
//                    targetString = partyMemberRequirement.getTarget().getType() + " type pokemon";
//                }
//                else
//                {
//                    targetString = partyMemberRequirement.getTarget().getSpecies();
//                }
//
//                if (partyMemberRequirement.getContains()) {
//                    tooltip.add(Component.translatable(
//                            "gui.cobbleemi.evolution_requirement.party_member",
//                            targetString
//                    ));
//                }
//                else
//                {
//                    tooltip.add(Component.translatable(
//                            "gui.cobbleemi.evolution_requirement.party_member.not",
//                            targetString
//                    ));
//                }
//            }
//            else if (requirement instanceof PokemonPropertiesRequirement pokemonPropertiesRequirement) {
////                tooltip.add(Component.translatable("gui.cobbleemi.evolution_requirement.pokemon_properties",
////                pokemonPropertiesRequirement.getTarget().getForm()));
//                // TODO: handle pokemon properties
//
//                if (!pokemonPropertiesRequirement.getTarget().getAspects().isEmpty())
//                {
//                    tooltip.add(Component.translatable("gui.cobbleemi.evolution_requirement.pokemon_properties_aspects",
//                            String.join(", ", pokemonPropertiesRequirement.getTarget().getAspects())
//                    ));
//                    continue;
//                }
//                if (!pokemonPropertiesRequirement.getTarget().getCustomProperties().isEmpty())
//                {
//                    for (var entry : pokemonPropertiesRequirement.getTarget().getCustomProperties())
//                    {
//                        tooltip.add(Component.translatable("gui.cobbleemi.evolution_requirement.pokemon_properties_custom",
//                                entry.asString()
//                        ));
//                    }
//                    continue;
//                }
//                if (pokemonPropertiesRequirement.getTarget().getNickname() != null)
//                {
//                    tooltip.add(Component.translatable("gui.cobbleemi.evolution_requirement.pokemon_properties_nickname",
//                            pokemonPropertiesRequirement.getTarget().getNickname()
//                    ));
//                    continue;
//                }
//                if (pokemonPropertiesRequirement.getTarget().getNature() != null)
//                {
//                    tooltip.add(Component.translatable("gui.cobbleemi.evolution_requirement.pokemon_properties_nature",
//                            pokemonPropertiesRequirement.getTarget().getNature()
//                    ));
//                    continue;
//                }
//
//                LOGGER.error("Unknown pokemon properties requirement: %s defaulting to showing original string", pokemonPropertiesRequirement.getTarget().getOriginalString());
//                tooltip.add(Component.translatable("gui.cobbleemi.evolution_requirement.pokemon_properties_unknown", pokemonPropertiesRequirement.getTarget().getOriginalString()));
//
//            }
//            else if (requirement instanceof PropertyRangeRequirement propertyRangeRequirement) {
//                tooltip.add(Component.translatable(
//                        "gui.cobbleemi.evolution_requirement.property_range",
//                        propertyRangeRequirement.getFeature(),
//                        propertyRangeRequirement.getRange().getFirst(),
//                        propertyRangeRequirement.getRange().getLast()
//
//                        ));
//            }
//            else if (requirement instanceof RecoilRequirement recoilRequirement) {
//                tooltip.add(Component.translatable(
//                        "gui.cobbleemi.evolution_requirement.recoil",
//                        recoilRequirement.getAmount()
//                ));
//            }
//            else if (requirement instanceof StatCompareRequirement statCompareRequirement) {
//                tooltip.add(Component.translatable(
//                        "gui.cobbleemi.evolution_requirement.stat_compare",
//                        statCompareRequirement.getHighStat(),
//                        statCompareRequirement.getLowStat()
//                ));
//            }
//            else if (requirement instanceof StatEqualRequirement statEqualRequirement) {
//                tooltip.add(Component.translatable(
//                        "gui.cobbleemi.evolution_requirement.stat_equal",
//                        statEqualRequirement.getStatOne(),
//                        statEqualRequirement.getStatTwo()
//                ));
//            }
//            else if (requirement instanceof UseMoveRequirement useMoveRequirement) {
//                tooltip.add(Component.translatable(
//                        "gui.cobbleemi.evolution_requirement.use_move",
//                        useMoveRequirement.getMove().getName(),
//                        useMoveRequirement.getAmount()
//                ));
//            }
//        }
//        return tooltip;
//    }
//
//
//    @Override
//    public EmiRecipeCategory getCategory() {
//        return EVOLUTIONS_CATEGORY;
//    }
//
//    @Override
//    public @Nullable ResourceLocation getId() {
//        return id;
//    }
//
//    @Override
//    public List<EmiIngredient> getInputs() {
//        return input;
//    }
//
//    @Override
//    public List<EmiStack> getOutputs() {
//        return output;
//    }
//
//    @Override
//    public int getDisplayWidth() {
//        return 144;
//    }
//
//    @Override
//    public int getDisplayHeight() {
//        return 130;
//    }
//
//    @Override
//    public void addWidgets(WidgetHolder widgets) {
//        widgets.addSlot(input.getFirst(), 18, 50);
//
//        for (int i = 0; i < output.size(); i++) {
//            widgets.addSlot(output.get(i), widgets.getWidth() - 36, 50+i*18).recipeContext(this);
//        }
//        widgets.addFillingArrow(widgets.getWidth()/2-9, 50,  1500);
//        widgets.addTooltipText(this.tooltip,widgets.getWidth()/2-9,  50, 24,17);
//        if (this.input.size() > 1)
//        {
//            for (int i = 1; i < input.size(); i++) {
//                widgets.addSlot(input.get(i), widgets.getWidth()/2-input.size()/2*18+i*18-9, 70);
//            }
//        }
//    }
//}
