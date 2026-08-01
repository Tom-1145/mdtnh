package mdtnh;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;

import java.util.HashMap;
import java.util.Map;

import static mdtnh.ModItems.items;

public class BasicFactory {
    // 存储所有工厂，键格式：工厂类型_等级_金属，例如 "furnace_basic_iron"
    public static Map<String, Block> factories = new HashMap<>();

    // 金属列表（与 ModItems 一致）
    private static final String[] METALS = {"iron", "copper", "lead", "tin"};

    // 要注册的工厂等级
    private static final FactoryTire[] TIERS = {FactoryTire.BASIC, FactoryTire.ADVANCED, FactoryTire.EXPERT};

    public static void load() {
        createFurnaces();
        createCompressors();
        createGrinders();
        createDecomposers();
        createRollingMills();
        // 可以继续添加其他工厂类型...
    }

    // ----- 熔炉系列 -----
    private static void createFurnaces() {
        for (FactoryTire tier : TIERS) {
            for (String metal : METALS) {
                String name = "furnace_" + tier.name().toLowerCase() + "_" + metal;
                factories.put(name, new RecipeCrafter(name) {{
                    size = tier.size;
                    health = tier.health;
                    consumePower(1.5f * tier.powerMultiplier);
                    requirements(Category.crafting,
                            ItemStack.with(Items.copper, (int)(30 * tier.powerMultiplier),
                                    Items.lead, (int)(20 * tier.powerMultiplier)));
                    buildVisibility = BuildVisibility.shown;

                    RecipeCrafter.Recipe[] recipes = new RecipeCrafter.Recipe[1];
                    recipes[0] = RecipeCrafter.Recipe.items(
                            new ItemStack[]{new ItemStack(items.get(metal + "_powder"), 1)},
                            new ItemStack(items.get(metal + "_ingot"), 1),
                            60f / tier.craftTimeMultiplier   // 速度倍率
                    );
                    this.groups = new RecipeCrafter.RecipeGroup[]{
                            new RecipeCrafter.RecipeGroup("smelting_" + tier.name().toLowerCase(), recipes)
                    };
                    this.groups[0].Texture_name = "programming-circuit1";
                }});
            }
        }
    }

    // ----- 压缩机系列（可逆） -----
    private static void createCompressors() {
        for (FactoryTire tier : TIERS) {
            for (String metal : METALS) {
                String name = "compressor_" + tier.name().toLowerCase() + "_" + metal;
                factories.put(name, new RecipeCrafter(name) {{
                    size = tier.size;
                    health = tier.health;
                    consumePower(2.0f * tier.powerMultiplier);
                    requirements(Category.crafting,
                            ItemStack.with(Items.titanium, (int)(20 * tier.powerMultiplier),
                                    Items.silicon, (int)(15 * tier.powerMultiplier)));
                    buildVisibility = BuildVisibility.shown;

                    RecipeCrafter.Recipe[] recipes = new RecipeCrafter.Recipe[2];
                    recipes[0] = RecipeCrafter.Recipe.items(
                            new ItemStack[]{new ItemStack(items.get(metal + "_ingot"), 9)},
                            new ItemStack(items.get(metal + "_block"), 1),
                            120f / tier.craftTimeMultiplier
                    );
                    recipes[1] = RecipeCrafter.Recipe.items(
                            new ItemStack[]{new ItemStack(items.get(metal + "_block"), 1)},
                            new ItemStack(items.get(metal + "_ingot"), 9),
                            80f / tier.craftTimeMultiplier
                    );
                    this.groups = new RecipeCrafter.RecipeGroup[]{
                            new RecipeCrafter.RecipeGroup("compress_" + tier.name().toLowerCase(), recipes)
                    };
                    this.groups[0].Texture_name = "programming-circuit2";
                }});
            }
        }
    }

    // ----- 粉碎机系列 -----
    private static void createGrinders() {
        for (FactoryTire tier : TIERS) {
            for (String metal : METALS) {
                String name = "grinder_" + tier.name().toLowerCase() + "_" + metal;
                factories.put(name, new RecipeCrafter(name) {{
                    size = tier.size;
                    health = tier.health;
                    consumePower(1.2f * tier.powerMultiplier);
                    requirements(Category.crafting,
                            ItemStack.with(Items.copper, (int)(20 * tier.powerMultiplier),
                                    Items.lead, (int)(15 * tier.powerMultiplier)));
                    buildVisibility = BuildVisibility.shown;

                    RecipeCrafter.Recipe[] recipes = new RecipeCrafter.Recipe[1];
                    recipes[0] = RecipeCrafter.Recipe.items(
                            new ItemStack[]{new ItemStack(items.get(metal + "_ingot"), 1)},
                            new ItemStack(items.get(metal + "_powder"), 1),
                            50f / tier.craftTimeMultiplier
                    );
                    this.groups = new RecipeCrafter.RecipeGroup[]{
                            new RecipeCrafter.RecipeGroup("grinding_" + tier.name().toLowerCase(), recipes)
                    };
                    this.groups[0].Texture_name = "programming-circuit3";
                }});
            }
        }
    }

    // ----- 分解机系列 -----
    private static void createDecomposers() {
        for (FactoryTire tier : TIERS) {
            for (String metal : METALS) {
                String name = "decomposer_" + tier.name().toLowerCase() + "_" + metal;
                factories.put(name, new RecipeCrafter(name) {{
                    size = tier.size;
                    health = tier.health;
                    consumePower(1.0f * tier.powerMultiplier);
                    requirements(Category.crafting,
                            ItemStack.with(Items.copper, (int)(15 * tier.powerMultiplier),
                                    Items.lead, (int)(10 * tier.powerMultiplier)));
                    buildVisibility = BuildVisibility.shown;

                    RecipeCrafter.Recipe[] recipes = new RecipeCrafter.Recipe[3];
                    recipes[0] = RecipeCrafter.Recipe.items(
                            new ItemStack[]{new ItemStack(items.get(metal + "_ingot"), 1)},
                            new ItemStack(items.get(metal + "_granule"), 9),
                            40f / tier.craftTimeMultiplier
                    );
                    recipes[1] = RecipeCrafter.Recipe.items(
                            new ItemStack[]{new ItemStack(items.get(metal + "_powder"), 1)},
                            new ItemStack(items.get(metal + "_small-pile-powder"), 4),
                            30f / tier.craftTimeMultiplier
                    );
                    recipes[2] = RecipeCrafter.Recipe.items(
                            new ItemStack[]{new ItemStack(items.get(metal + "_powder"), 1)},
                            new ItemStack(items.get(metal + "_pinch-powder"), 9),
                            35f / tier.craftTimeMultiplier
                    );
                    this.groups = new RecipeCrafter.RecipeGroup[]{
                            new RecipeCrafter.RecipeGroup("decompose_" + tier.name().toLowerCase(), recipes)
                    };
                    this.groups[0].Texture_name = "programming-circuit4";
                }});
            }
        }
    }

    // ----- 卷扳机系列 -----
    private static void createRollingMills() {
        for (FactoryTire tier : TIERS) {
            for (String metal : METALS) {
                String name = "rollingmill_" + tier.name().toLowerCase() + "_" + metal;
                factories.put(name, new RecipeCrafter(name) {{
                    size = tier.size;
                    health = tier.health;
                    consumePower(1.8f * tier.powerMultiplier);
                    requirements(Category.crafting,
                            ItemStack.with(Items.titanium, (int)(25 * tier.powerMultiplier),
                                    Items.silicon, (int)(10 * tier.powerMultiplier)));
                    buildVisibility = BuildVisibility.shown;

                    RecipeCrafter.Recipe[] recipes = new RecipeCrafter.Recipe[2];
                    recipes[0] = RecipeCrafter.Recipe.items(
                            new ItemStack[]{new ItemStack(items.get(metal + "_ingot"), 1)},
                            new ItemStack(items.get(metal + "_plate"), 1),
                            60f / tier.craftTimeMultiplier
                    );
                    recipes[1] = RecipeCrafter.Recipe.items(
                            new ItemStack[]{new ItemStack(items.get(metal + "_ingot"), 1)},
                            new ItemStack(items.get(metal + "_foil"), 4),
                            45f / tier.craftTimeMultiplier
                    );
                    this.groups = new RecipeCrafter.RecipeGroup[]{
                            new RecipeCrafter.RecipeGroup("rolling_" + tier.name().toLowerCase(), recipes)
                    };
                    this.groups[0].Texture_name = "programming-circuit5";
                }});
            }
        }
    }

    // 便捷获取方法（可选）
    public static Block get(String tier, String type, String metal) {
        return factories.get(type + "_" + tier + "_" + metal);
    }
}