package com.teseting.common.cobbleemi;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.conditional.RegistryLikeCondition;
import com.cobblemon.mod.common.api.conditional.RegistryLikeIdentifierCondition;
import com.cobblemon.mod.common.api.conditional.RegistryLikeTagCondition;
import com.cobblemon.mod.common.api.drop.DropEntry;
import com.cobblemon.mod.common.api.drop.ItemDropEntry;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools;
import com.cobblemon.mod.common.api.spawning.condition.SpawningCondition;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import io.github.chakyl.cobbleemibackported.CobblemonGeneralDropRecipe;
import io.github.chakyl.cobbleemibackported.CobblemonItemStack;
import io.github.chakyl.cobbleemibackported.CobblemonSpawnSpecificDropRecipe;
import io.github.chakyl.cobbleemibackported.CobblemonStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;


import java.lang.reflect.Field;
import java.util.*;



@EmiEntrypoint
public class CobbleEmiPlugin implements EmiPlugin {

    public static final EmiRecipeCategory COBBLEMON_GENERAL_DROPS_CATEGORY = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath("cobbleemi","cobblemon_general_drops"), EmiStack.of(CobblemonItems.GILDED_CHEST));
    public static final EmiRecipeCategory COBBLEMON_SPAWN_SPECIFIC_DROPS_RECIPE_CATEGORY =  new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath("cobbleemi","cobblemon_spawn_drops"), EmiStack.of(CobblemonItems.BLUE_GILDED_CHEST));



    public static final Map<Species, List<Component>> POKEMON_INFO_MAP = new HashMap<>();

    public static final Map<Species, List<Pair<List<CobblemonItemStack>,List<Component>>>> EXTRA_DROP_MAP = new HashMap<>();
    public static final Minecraft CLIENT = Minecraft.getInstance();

    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(COBBLEMON_GENERAL_DROPS_CATEGORY);
        registry.addCategory(COBBLEMON_SPAWN_SPECIFIC_DROPS_RECIPE_CATEGORY);
        for (SpawnDetail spawnDetail : CobblemonSpawnPools.WORLD_SPAWN_POOL) {
            if (spawnDetail instanceof PokemonSpawnDetail pokemonSpawnDetail) {
                Species specie = pokemonSpawnDetail.getPokemon().create().getSpecies();
                List<Component> info = new ArrayList<>();

                if (pokemonSpawnDetail.getWeight() >= 0) {
                    info.add(Component.translatable("gui.cobbleemi.spawninfo.rngweight").append(String.valueOf(pokemonSpawnDetail.getWeight())));
                }

                if (pokemonSpawnDetail.getPercentage() >= 0) {
                    info.add(Component.translatable("gui.cobbleemi.spawninfo.percentage").append(String.valueOf(pokemonSpawnDetail.getPercentage())));
                }

                if (pokemonSpawnDetail.getLevelRange() != null) {
                    info.add(Component.translatable("gui.cobbleemi.spawninfo.levelrange").append(
                            pokemonSpawnDetail.getLevelRange().getFirst() + "-"
                                    + pokemonSpawnDetail.getLevelRange().getLast()));
                }

                if (!pokemonSpawnDetail.getConditions().isEmpty()) {
                    List<Component> output = writeCondtoStringBuilder(pokemonSpawnDetail.getConditions().get(0));
                    if (!output.isEmpty()) {
                        info.add(Component.translatable("gui.cobbleemi.spawninfo.mustsatisfy"));
                        info.addAll(output);
                    }
                }
                if (!pokemonSpawnDetail.getAnticonditions().isEmpty()) {
                    List<Component> output = writeCondtoStringBuilder(pokemonSpawnDetail.getAnticonditions().get(0));
                    if (!output.isEmpty()) {
                        info.add(Component.translatable("gui.cobbleemi.spawninfo.mustnotsatisfy"));
                        info.addAll(output);
                    }
                }
                if (POKEMON_INFO_MAP.containsKey(specie)) {
                    POKEMON_INFO_MAP.get(specie).add(Component.literal("---------------"));
                    POKEMON_INFO_MAP.get(specie).addAll(info);
                } else {
                    POKEMON_INFO_MAP.put(specie, info);
                }

                if (pokemonSpawnDetail.getDrops() != null && !pokemonSpawnDetail.getDrops().getEntries().isEmpty()) {
                    EXTRA_DROP_MAP.putIfAbsent(specie, new ArrayList<>());
                    EXTRA_DROP_MAP.get(specie).add(Pair.of(new ArrayList<>(), new ArrayList<>(info)));

                    ClientPacketListener clientPacketListener = CLIENT.getConnection();
                    if (clientPacketListener == null) {
                        continue;
                    }
                    for (DropEntry dropEntry : pokemonSpawnDetail.getDrops().getEntries()) {
                        if (dropEntry instanceof ItemDropEntry itemDrop) {
                            Item item = clientPacketListener.registryAccess().registryOrThrow(Registries.ITEM).get(itemDrop.getItem());
                            if (item == null) {
                                continue;
                            }
//                            EXTRA_DROP_MAP.get(specie).getLast().get(0).add(CobblemonItemStack.of(itemDrop, EmiStack.of(item)));
                        }
                    }
                }
            }
        }

        for (Species specie : PokemonSpecies.INSTANCE.getSpecies().stream().sorted(Comparator.comparing(Species::getNationalPokedexNumber)).toList())
        {
            registry.addEmiStack(new CobblemonStack(specie));
            if (!specie.getDrops().getEntries().isEmpty()) {
                registry.addRecipe(new CobblemonGeneralDropRecipe(specie));
            }
            if (EXTRA_DROP_MAP.containsKey(specie)) {
                for (Pair<List<CobblemonItemStack>, List<Component>> pair : EXTRA_DROP_MAP.get(specie)) {
                    registry.addRecipe(new CobblemonSpawnSpecificDropRecipe(specie, pair.getFirst(), pair.getSecond()));
                }
            }

            if (POKEMON_INFO_MAP.containsKey(specie))
            {
                registry.addRecipe(new EmiInfoRecipe(List.of(new CobblemonStack(specie)),
                        POKEMON_INFO_MAP.get(specie),
                        specie.getResourceIdentifier().withPrefix("/spawn_info/")));
            }
        }
    }

    public List<Component> writeCondtoStringBuilder(SpawningCondition<?> spawningCondition)
    {
        List<Component> components = new ArrayList<>();
        Field[] fields =  SpawningCondition.class.getDeclaredFields();
        for (Field field : fields) {
            Class <?> fieldType = field.getType();
            field.setAccessible(true);
            try {
                if (field.get(spawningCondition) == null)
                {
                    field.setAccessible(false);
                    continue;
                }

                if (fieldType == Integer.class || fieldType == Float.class) {

                        components.add(Component.translatable("gui.cobbleemi.spawninfo."+field.getName()).append(field.get(spawningCondition).toString()));
                }
                else if (fieldType == Boolean.class)
                {
                    components.add(Component.translatable("gui.cobbleemi.spawninfo."+field.getName()+"."+field.get(spawningCondition)));
                }
//                else{
//                    LOGGER.info("Unhandled field type: " + fieldType.getName() + " for field " + field.getName());
//                }
                else if (field.getName().equals("biomes"))
                {
                    if (spawningCondition.getBiomes() != null)
                    {
                        components.add(Component.translatable("gui.cobbleemi.spawninfo.biomes"));
                        for (RegistryLikeCondition<Biome> condition : spawningCondition.getBiomes()) {
                            if (condition instanceof RegistryLikeTagCondition<Biome> tagCondition) {
                                components.add(Component.literal((tagCondition.getTag().location().toString())));
                            } else if (condition instanceof RegistryLikeIdentifierCondition<Biome> identifierCondition) {
                                components.add(Component.literal(identifierCondition.getIdentifier().toString()));
                            }

                        }
                    }
                }
                else if (field.getName().equals("moonPhase"))
                {
//                    if (spawningCondition.getMoonPhase() != null)
//                    {
//                        components.add(Component.translatable("gui.cobbleemi.spawninfo.moonPhase").append(
//                                spawningCondition.getMoonPhase().getRanges().get(0).toString()
//                                        +"-"
//                        + spawningCondition.getMoonPhase().getRanges().get(spawningCondition.getMoonPhase().getRanges().size() - 1).toString()));
//                    }
                }
                else if (field.getName().equals("structures"))
                {
                    if (spawningCondition.getStructures() != null)
                    {
                        components.add(Component.translatable("gui.cobbleemi.spawninfo.structures"));
                        for (Either<ResourceLocation, TagKey<Structure>> structureKey : spawningCondition.getStructures()) {
                            structureKey.ifLeft((rl)->components.add(Component.literal(rl.toString())));
                            structureKey.ifRight((rl)->components.add(Component.literal(rl.location().toString())));
                        }
                    }
                }
                else if (field.getName().equals("timeRange"))
                {
                    if (spawningCondition.getTimeRange() != null)
                    {
                        components.add(Component.translatable("gui.cobbleemi.spawninfo.timeRange").append(
                                spawningCondition.getTimeRange().getRanges().get(0).toString()
                                        +"-"
                                        + spawningCondition.getTimeRange().getRanges().get(spawningCondition.getTimeRange().getRanges().size() - 1).toString()));
                    }
                }
                else if (field.getName().equals("dimensions"))
                {
                    if (spawningCondition.getDimensions() != null)
                    {
                        components.add(Component.translatable("gui.cobbleemi.spawninfo.dimensions"));
                        for (ResourceLocation rl : spawningCondition.getDimensions()) {
                            components.add(Component.literal(rl.toString()));
                        }
                    }
                }
            }
            catch (IllegalAccessException exception)
            {
                LOGGER.error(exception.getMessage());
            }
            field.setAccessible(false);
        }
        return components;
    }
}
