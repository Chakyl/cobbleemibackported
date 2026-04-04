package io.github.chakyl.cobbleemibackported;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.conditional.RegistryLikeCondition;
import com.cobblemon.mod.common.api.conditional.RegistryLikeIdentifierCondition;
import com.cobblemon.mod.common.api.conditional.RegistryLikeTagCondition;
import com.cobblemon.mod.common.api.drop.DropEntry;
import com.cobblemon.mod.common.api.drop.ItemDropEntry;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.evolution.Evolution;
import com.cobblemon.mod.common.api.pokemon.feature.GlobalSpeciesFeatures;
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeatureAssignments;
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeatures;
import com.cobblemon.mod.common.api.spawning.*;
import com.cobblemon.mod.common.api.spawning.condition.AreaTypeSpawningCondition;
import com.cobblemon.mod.common.api.spawning.condition.SpawningCondition;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.multiplier.WeightMultiplier;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.pokemon.SpeciesAdditions;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import kotlin.ranges.IntRange;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;


@EmiEntrypoint
public class CobbleEmiPlugin implements EmiPlugin {

    public static final EmiRecipeCategory COBBLEMON_GENERAL_DROPS_CATEGORY = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath("cobbleemi", "cobblemon_general_drops"), EmiStack.of(CobblemonItems.GILDED_CHEST));
    public static final EmiRecipeCategory COBBLEMON_SPAWN_SPECIFIC_DROPS_RECIPE_CATEGORY = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath("cobbleemi", "cobblemon_spawn_drops"), EmiStack.of(CobblemonItems.BLUE_GILDED_CHEST));
    public static final EmiRecipeCategory EVOLUTIONS_CATEGORY = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath("cobbleemi", "cobblemon_evolutions"), new CobblemonStack(PokemonSpecies.INSTANCE.getByName("bulbasaur")));


    public static final Map<CobblemonStack, List<List<Component>>> POKEMON_INFO_MAP = new HashMap<>();

    public static final Map<CobblemonStack, List<Pair<List<CobblemonItemStack>, List<Component>>>> EXTRA_DROP_MAP = new HashMap<>();
    public static final Minecraft CLIENT = Minecraft.getInstance();

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final List<String> moonPhaseList = List.of(
            "gui.cobbleemi.spawninfo.moonphase.full",
            "gui.cobbleemi.spawninfo.moonphase.wanning_gibbous",
            "gui.cobbleemi.spawninfo.moonphase.third_quarter",
            "gui.cobbleemi.spawninfo.moonphase.wanning_crescent",
            "gui.cobbleemi.spawninfo.moonphase.new",
            "gui.cobbleemi.spawninfo.moonphase.waxing_crescent",
            "gui.cobbleemi.spawninfo.moonphase.first_quarter",
            "gui.cobbleemi.spawninfo.moonphase.waxing_gibbous"
    );

    public static final Map<String, List<IntRange>> TIME_RANGE_MAP = TimeRange.Companion.getTimeRanges().entrySet().stream().collect(HashMap::new, (map, pair) -> map.put(pair.getKey(), new ArrayList<>(pair.getValue().getRanges())), HashMap::putAll);

    static {
        for (Map.Entry<String, List<IntRange>> entry : TIME_RANGE_MAP.entrySet()) {
            entry.getValue().sort(Comparator.comparing(IntRange::getStart).thenComparing(IntRange::getEndInclusive));
        }
    }

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(COBBLEMON_GENERAL_DROPS_CATEGORY);
        registry.addCategory(COBBLEMON_SPAWN_SPECIFIC_DROPS_RECIPE_CATEGORY);
        registry.addCategory(EVOLUTIONS_CATEGORY);

        if (CobblemonSpawnPools.WORLD_SPAWN_POOL.getDetails().isEmpty()) {
            LOGGER.warn("Reloading world spawn pool for EMI spawn info...");

            CobblemonSpawnPools.INSTANCE.load();
            PackRepository packrepository = ServerPacksSource.createPackRepository(Path.of("options.txt")); // add invalid path to skip looking for datapacks in folder
            CloseableResourceManager closeableresourcemanager = new WorldLoader.PackConfig(packrepository, WorldDataConfiguration.DEFAULT, false, false)
                    .createResourceManager()
                    .getSecond();
            LOGGER.warn("Ignore the 'Failed to list packs in options.txt' warning");
            SpeciesFeatures.INSTANCE.reload(closeableresourcemanager);
            GlobalSpeciesFeatures.INSTANCE.reload(closeableresourcemanager);
            SpeciesFeatureAssignments.INSTANCE.reload(closeableresourcemanager);
            Abilities.INSTANCE.reload(closeableresourcemanager);
            PokemonSpecies.INSTANCE.reload(closeableresourcemanager);
            SpeciesAdditions.INSTANCE.reload(closeableresourcemanager);
            SpawnDetailPresets.INSTANCE.reload(closeableresourcemanager);
            CobblemonSpawnPools.WORLD_SPAWN_POOL.reload(closeableresourcemanager);

            closeableresourcemanager.close();
        }

        for (SpawnDetail spawnDetail : CobblemonSpawnPools.WORLD_SPAWN_POOL) {


            if (spawnDetail instanceof PokemonSpawnDetail pokemonSpawnDetail) {
                Species specie = pokemonSpawnDetail.getPokemon().create().getSpecies();
//                String form = pokemonSpawnDetail.getPokemon().getForm();
                var aspects = pokemonSpawnDetail.getPokemon().getAspects();
                FormData formdata = specie.getForm(aspects);

                CobblemonStack cobblemonKeyWithForm = new CobblemonStack(formdata);
//                CobblemonStack cobblemonKey = new CobblemonStack(specie);

                List<Component> info = new ArrayList<>();

                if (!pokemonSpawnDetail.getBucket().getName().isEmpty()) {
                    info.add(Component.translatable("gui.cobbleemi.spawninfo.rngbucket").append(pokemonSpawnDetail.getBucket().getName()));
                }
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
                if (pokemonSpawnDetail.getCompositeCondition() != null) {
                    info.add(Component.translatable("gui.cobbleemi.spawninfo.compositecondition.startcompositecondition"));
                    int i = 1;

                    if (!pokemonSpawnDetail.getCompositeCondition().getConditions().isEmpty()) {
                        info.add(Component.translatable("gui.cobbleemi.spawninfo.compositecondition.musthaveany"));
                        for (SpawningCondition<?> cond : pokemonSpawnDetail.getCompositeCondition().getConditions()) {
                            info.add(Component.translatable("gui.cobbleemi.spawninfo.compositecondition.conditionnumber", i));
                            info.addAll(writeCondtoStringBuilder(cond));
                            i += 1;
                        }
                    }
                    if (!pokemonSpawnDetail.getCompositeCondition().getAnticonditions().isEmpty()) {

                        info.add(Component.translatable("gui.cobbleemi.spawninfo.compositecondition.mustnothaveany"));
                        i = 1;
                        for (SpawningCondition<?> cond : pokemonSpawnDetail.getCompositeCondition().getAnticonditions()) {
                            info.add(Component.translatable("gui.cobbleemi.spawninfo.compositecondition.anticonditionnumber", i));
                            info.addAll(writeCondtoStringBuilder(cond));
                            i += 1;
                        }
                    }

                    info.add(Component.translatable("gui.cobbleemi.spawninfo.compositecondition.endcompositecondition"));
                }

                if (!pokemonSpawnDetail.getWeightMultipliers().isEmpty()) {
                    info.add(Component.literal("---------------"));
                    info.add(Component.translatable("gui.cobbleemi.spawninfo.weightmultiplier"));
                    for (WeightMultiplier weightMultiplier : pokemonSpawnDetail.getWeightMultipliers()) {
                        info.add(Component.translatable("gui.cobbleemi.spawninfo.weightmultiplier.multiplier", weightMultiplier.getMultiplier()));
                        info.add(Component.translatable("gui.cobbleemi.spawninfo.compositecondition.startcompositecondition"));
                        int i = 1;

                        if (!weightMultiplier.getConditions().isEmpty()) {
                            info.add(Component.translatable("gui.cobbleemi.spawninfo.compositecondition.musthaveany"));
                            for (SpawningCondition<?> cond : weightMultiplier.getConditions()) {
                                info.add(Component.translatable("gui.cobbleemi.spawninfo.compositecondition.conditionnumber", i));
                                info.addAll(writeCondtoStringBuilder(cond));
                                i += 1;
                            }
                        }
                        if (!weightMultiplier.getAnticonditions().isEmpty()) {

                            info.add(Component.translatable("gui.cobbleemi.spawninfo.compositecondition.mustnothaveany"));
                            i = 1;
                            for (SpawningCondition<?> cond : pokemonSpawnDetail.getAnticonditions()) {
                                info.add(Component.translatable("gui.cobbleemi.spawninfo.compositecondition.anticonditionnumber", i));
                                info.addAll(writeCondtoStringBuilder(cond));
                                i += 1;
                            }
                        }

                        info.add(Component.translatable("gui.cobbleemi.spawninfo.compositecondition.endcompositecondition"));
                    }

                }

//                if (!POKEMON_INFO_MAP.containsKey(cobblemonKey))
//                {
//                    POKEMON_INFO_MAP.put(cobblemonKey, new HashMap<>());
//                }

                if (!POKEMON_INFO_MAP.containsKey(cobblemonKeyWithForm)) {
                    POKEMON_INFO_MAP.put(cobblemonKeyWithForm, new ArrayList<>());
                }

                POKEMON_INFO_MAP.get(cobblemonKeyWithForm).add(info);

                if (pokemonSpawnDetail.getDrops() != null && !pokemonSpawnDetail.getDrops().getEntries().isEmpty()) {
                    EXTRA_DROP_MAP.putIfAbsent(cobblemonKeyWithForm, new ArrayList<>());
                    EXTRA_DROP_MAP.get(cobblemonKeyWithForm).add(Pair.of(new ArrayList<>(), new ArrayList<>(info)));

                    ClientPacketListener clientPacketListener = CLIENT.getConnection();
                    if (clientPacketListener == null) {
                        continue;
                    }
                    for (DropEntry dropEntry : pokemonSpawnDetail.getDrops().getEntries()) {
                        if (dropEntry instanceof ItemDropEntry itemDrop) {

                            var itemRegistry = clientPacketListener.registryAccess().registry(Registries.ITEM);
                            Item item;
                            if (itemRegistry.isPresent() && (item = itemRegistry.get().get(itemDrop.getItem())) != null) {
                                EXTRA_DROP_MAP.get(cobblemonKeyWithForm).get(EXTRA_DROP_MAP.get(cobblemonKeyWithForm).size() - 1).getFirst().add(CobblemonItemStack.of(itemDrop, EmiStack.of(item)));
                            }
                        }
                    }
                }
            }
        }

        for (Species specie : PokemonSpecies.INSTANCE.getSpecies().stream().sorted(Comparator.comparing(Species::getNationalPokedexNumber)).toList()) {
//            CobblemonStack cobblemonKey = new CobblemonStack(specie);
            if (!specie.getImplemented()) continue;
            if (!specie.getForms().contains(specie.getStandardForm())) {
                FormData form = specie.getStandardForm();

                CobblemonStack cobblemonKeyWithAspects = new CobblemonStack(form);
                registry.addEmiStack(cobblemonKeyWithAspects);

                if (!form.getDrops().getEntries().isEmpty()) {
                    registry.addRecipe(new CobblemonGeneralDropRecipe(cobblemonKeyWithAspects));
                }

                if (POKEMON_INFO_MAP.containsKey(cobblemonKeyWithAspects)) {
                    ResourceLocation rl = cobblemonKeyWithAspects.getCobbleEMIId().withPrefix("/spawninfo/");
                    int info_id = 1;
                    for (var infos : POKEMON_INFO_MAP.get(cobblemonKeyWithAspects)) {
                        registry.addRecipe(new EmiInfoRecipe(List.of(cobblemonKeyWithAspects), infos, rl.withSuffix(String.valueOf(info_id++))));
                    }
                }
                if (EXTRA_DROP_MAP.containsKey(cobblemonKeyWithAspects)) {
                    int dropIndex = 1;
                    for (var pair : EXTRA_DROP_MAP.get(cobblemonKeyWithAspects)) {
                        registry.addRecipe(new CobblemonSpawnSpecificDropRecipe(cobblemonKeyWithAspects, pair.getFirst(), pair.getSecond(), dropIndex++));
                    }
                }
                if (!form.getEvolutions().isEmpty()) {
                    int evolutionIndex = 1;
                    for (Evolution evolution : form.getEvolutions()) {
                        registry.addRecipe(new CobblemonEvolutionRecipe(cobblemonKeyWithAspects, evolution, evolutionIndex++));
                    }
                }
            }
            for (FormData form : specie.getForms()) {
                if (form.formOnlyShowdownId().equals("mega") || form.formOnlyShowdownId().contains("gmax") || form.formOnlyShowdownId().equals("megax") || form.formOnlyShowdownId().equals("megay")) continue;
                CobblemonStack cobblemonKeyWithAspects = new CobblemonStack(form);
                registry.addEmiStack(cobblemonKeyWithAspects);
                if (!form.getDrops().getEntries().isEmpty()) {
                    registry.addRecipe(new CobblemonGeneralDropRecipe(cobblemonKeyWithAspects));
                }

                if (POKEMON_INFO_MAP.containsKey(cobblemonKeyWithAspects)) {
                    ResourceLocation rl = cobblemonKeyWithAspects.getCobbleEMIId().withPrefix("/spawninfo/");
                    int info_id = 1;
                    for (var infos : POKEMON_INFO_MAP.get(cobblemonKeyWithAspects)) {
                        registry.addRecipe(new EmiInfoRecipe(List.of(cobblemonKeyWithAspects), infos, rl.withSuffix(String.valueOf(info_id++))));
                    }
                }
                if (EXTRA_DROP_MAP.containsKey(cobblemonKeyWithAspects)) {
                    int dropIndex = 1;
                    for (var pair : EXTRA_DROP_MAP.get(cobblemonKeyWithAspects)) {
                        registry.addRecipe(new CobblemonSpawnSpecificDropRecipe(cobblemonKeyWithAspects, pair.getFirst(), pair.getSecond(), dropIndex++));
                    }
                }
                if (!form.getEvolutions().isEmpty()) {
                    int evolutionIndex = 1;
                    for (Evolution evolution : form.getEvolutions()) {
                        registry.addRecipe(new CobblemonEvolutionRecipe(cobblemonKeyWithAspects, evolution, evolutionIndex++));
                    }
                }
            }
        }
    }

    static List<String> cover(
            TimeRange target
    ) throws IllegalAccessException {
        List<String> result = new ArrayList<>();
        List<IntRange> targetRanges = target.getRanges();
        targetRanges.sort(Comparator.comparing(IntRange::getStart).thenComparing(IntRange::getEndInclusive));

        for (Map.Entry<String, List<IntRange>> entry : TIME_RANGE_MAP.entrySet()) {
            List<IntRange> intervalRanges = entry.getValue();
            intervalRanges.sort(Comparator.comparing(IntRange::getStart).thenComparing(IntRange::getEndInclusive));
            int target_i = 0;
            int interval_i = 0;
            while (target_i < targetRanges.size() && interval_i < intervalRanges.size()) {
                IntRange targetRange = targetRanges.get(target_i);
                IntRange intervalRange = intervalRanges.get(interval_i);

                if (targetRange.getEndInclusive() < intervalRange.getEndInclusive()) {
                    target_i += 1;
                    continue;
                }
                if (targetRange.getStart() > intervalRange.getStart()) {
                    break;
                }
                interval_i += 1;
                if (interval_i >= intervalRanges.size()) {
                    result.add(entry.getKey());
                    break;
                }
            }
        }
        return result;
    }

    static boolean checkEquals(IntRanges a, IntRanges b) {
        if (a.getRanges().size() != b.getRanges().size()) {
            return false;
        }
        List<IntRange> aRanges = b.getRanges();
        List<IntRange> bRanges = b.getRanges();
        aRanges.sort(Comparator.comparing(IntRange::getStart).thenComparing(IntRange::getEndInclusive));
        bRanges.sort(Comparator.comparing(IntRange::getStart).thenComparing(IntRange::getEndInclusive));

        for (int i = 0; i < aRanges.size(); i++) {
            if (!aRanges.get(i).equals(bRanges.get(i))) {
                return false;
            }
        }
        return true;
    }

    public List<Component> writeCondtoStringBuilder(SpawningCondition<?> spawningCondition) {
        List<Field> fields = new ArrayList<>(List.of(SpawningCondition.class.getDeclaredFields()));
        if (spawningCondition instanceof AreaTypeSpawningCondition<?>) {
            fields.addAll(List.of(AreaTypeSpawningCondition.class.getDeclaredFields()));
        }

        List<Component> components = new ArrayList<>();
        for (Field field : fields) {
            Class<?> fieldType = field.getType();
            field.setAccessible(true);
            try {
                if (field.get(spawningCondition) == null) {
                    field.setAccessible(false);
                    continue;
                }

                if (fieldType == Integer.class || fieldType == Float.class) {

                    components.add(Component.translatable("gui.cobbleemi.spawninfo." + field.getName()).append(field.get(spawningCondition).toString()));
                } else if (fieldType == Boolean.class) {
                    components.add(Component.translatable("gui.cobbleemi.spawninfo." + field.getName() + "." + field.get(spawningCondition)));
                } else if (fieldType == ResourceLocation.class) {
                    components.add(Component.translatable("gui.cobbleemi.spawninfo." + field.getName()).append(Component.literal(field.get(spawningCondition).toString())));
                } else if (field.getName().equals("neededNearbyBlocks")) {
                    if (spawningCondition instanceof AreaTypeSpawningCondition<?> areaTypeSpawningCondition) {
                        if (areaTypeSpawningCondition.getNeededNearbyBlocks() != null) {
                            components.add(Component.translatable("gui.cobbleemi.spawninfo.neededNearbyBlocks"));
                            for (RegistryLikeCondition<Block> blockCondition : areaTypeSpawningCondition.getNeededNearbyBlocks()) {
                                if (blockCondition instanceof RegistryLikeTagCondition<Block> tagCondition) {
                                    components.add(Component.literal(tagCondition.getTag().location().toString()));
                                } else if (blockCondition instanceof RegistryLikeIdentifierCondition<Block> identifierCondition) {
                                    components.add(Component.literal(identifierCondition.getIdentifier().toString()));
                                }
                            }
                        }
                    }
                }
//                else{
//                    LOGGER.info("Unhandled field type: " + fieldType.getName() + " for field " + field.getName());
//                }
                else if (field.getName().equals("biomes")) {
                    if (spawningCondition.getBiomes() != null) {
                        components.add(Component.translatable("gui.cobbleemi.spawninfo.biomes"));
                        for (RegistryLikeCondition<Biome> condition : spawningCondition.getBiomes()) {
                            if (condition instanceof RegistryLikeTagCondition<Biome> tagCondition) {
                                components.add(Component.literal("#" + tagCondition.getTag().location()));
                            } else if (condition instanceof RegistryLikeIdentifierCondition<Biome> identifierCondition) {
                                components.add(Component.literal(identifierCondition.getIdentifier().toString()));
                            }

                        }
                    }
                } else if (field.getName().equals("moonPhase")) {
                    if (spawningCondition.getMoonPhase() != null) {
                        components.add(Component.translatable("gui.cobbleemi.spawninfo.moonPhase"));

                        MutableComponent toAdd = Component.literal("");
                        for (IntRange timeRange : spawningCondition.getMoonPhase().getRanges()) {
                            toAdd.append(timeRange.getStart() + "-" + timeRange.getEndInclusive() + " ");
                        }
                        components.add(toAdd);

                        boolean found = false;
                        for (Map.Entry<String, MoonPhaseRange> entry : MoonPhaseRange.Companion.getMoonPhaseRanges().entrySet()) {
                            if (checkEquals(spawningCondition.getMoonPhase(), entry.getValue())) {
                                found = true;
                                components.add(Component.translatable("gui.cobbleemi.spawninfo.moonphase." + entry.getKey()));
                                break;
                            }
                        }

//                        if (!found) {
//                            toAdd = Component.literal("");
//                            for (IntRange timeRange : spawningCondition.getMoonPhase().getRanges()) {
//                                for (int i = timeRange.getStart(); i <= timeRange.getEndInclusive(); i++) {
//                                    toAdd.append(Component.translatable(moonPhaseList.get(i)));
//                                }
//                            }
//                            components.add(toAdd);
//                        }
                    }
                } else if (field.getName().equals("structures")) {
                    if (spawningCondition.getStructures() != null) {
                        components.add(Component.translatable("gui.cobbleemi.spawninfo.structures"));
                        for (Either<ResourceLocation, TagKey<Structure>> structureKey : spawningCondition.getStructures()) {
                            structureKey.ifLeft((rl) -> components.add(Component.literal(rl.toString())));
                            structureKey.ifRight((rl) -> components.add(Component.literal(rl.location().toString())));
                        }
                    }
                } else if (field.getName().equals("timeRange")) {
                    if (spawningCondition.getTimeRange() != null) {
                        components.add(Component.translatable("gui.cobbleemi.spawninfo.timeRange"));
                        components.add(Component.literal(StringUtils.join(spawningCondition.getTimeRange().getRanges().stream().map((intRange) -> intRange.getStart() + "-" + intRange.getEndInclusive()).toList(), ", ")));
                        try {
                            MutableComponent toAdd = Component.literal("");
                            List<MutableComponent> toAddList = cover(spawningCondition.getTimeRange()).stream().map((string) -> Component.translatable("gui.cobbleemi.spawninfo.timerange." + string)).toList();
                            for (MutableComponent mc : toAddList) {
                                toAdd.append(mc).append(" ");
                            }
                            components.add(toAdd);
                        } catch (IllegalAccessException ignored) {

                        }
                    }
                } else if (field.getName().equals("dimensions")) {
                    if (spawningCondition.getDimensions() != null) {
                        components.add(Component.translatable("gui.cobbleemi.spawninfo.dimensions"));
                        for (ResourceLocation rl : spawningCondition.getDimensions()) {
                            components.add(Component.literal(rl.toString()));
                        }
                    }
                }
            } catch (IllegalAccessException exception) {
                LOGGER.error(exception.getMessage());
            }
            field.setAccessible(false);
        }
        return components;
    }
}
