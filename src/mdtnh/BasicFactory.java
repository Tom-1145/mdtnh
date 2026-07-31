package mdtnh;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;

import static mdtnh.ModItems.items;

public class BasicFactory {
    public static Block 熔炉, 压缩机, 粉碎机, 分解机, 卷扳机;

    // 金属内部名列表（必须与 ModItems 中的键一致）
    private static final String[] METALS = {"iron", "copper", "lead", "tin"};

    public static void load() {
        createFurnace();
        createCompressor();
        createGrinder();
        createDecomposer();
        createRollingMill();
    }

    private static void createFurnace() {
        熔炉 = new RecipeCrafter("furnace") {{
            size = 2;
            health = 120;
            consumePower(1.5f);
            requirements(Category.crafting, ItemStack.with(Items.copper, 30, Items.lead, 20));
            buildVisibility = BuildVisibility.shown;

            RecipeCrafter.Recipe[] recipes = new RecipeCrafter.Recipe[METALS.length];
            for (int i = 0; i < METALS.length; i++) {
                String metal = METALS[i];
                recipes[i] = RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(items.get(metal + "_powder"), 1)},
                        new ItemStack(items.get(metal + "_ingot"), 1),
                        60f
                );
            }
            this.groups = new RecipeCrafter.RecipeGroup[]{
                    new RecipeCrafter.RecipeGroup("smelting", recipes)
            };
            this.groups[0].Texture_name = "programming-circuit1";
        }};
    }

    private static void createCompressor() {
        压缩机 = new RecipeCrafter("compressor") {{
            size = 3;
            health = 200;
            consumePower(2.0f);
            requirements(Category.crafting, ItemStack.with(Items.titanium, 20, Items.silicon, 15));
            buildVisibility = BuildVisibility.shown;

            RecipeCrafter.Recipe[] recipes = new RecipeCrafter.Recipe[METALS.length * 2];
            int idx = 0;
            for (String metal : METALS) {
                recipes[idx++] = RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(items.get(metal + "_ingot"), 9)},
                        new ItemStack(items.get(metal + "_block"), 1),
                        120f
                );
                recipes[idx++] = RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(items.get(metal + "_block"), 1)},
                        new ItemStack(items.get(metal + "_ingot"), 9),
                        80f
                );
            }
            this.groups = new RecipeCrafter.RecipeGroup[]{
                    new RecipeCrafter.RecipeGroup("compress", recipes)
            };
            this.groups[0].Texture_name = "programming-circuit2";
        }};
    }

    private static void createGrinder() {
        粉碎机 = new RecipeCrafter("grinder") {{
            size = 2;
            health = 100;
            consumePower(1.2f);
            requirements(Category.crafting, ItemStack.with(Items.copper, 20, Items.lead, 15));
            buildVisibility = BuildVisibility.shown;

            RecipeCrafter.Recipe[] recipes = new RecipeCrafter.Recipe[METALS.length];
            for (int i = 0; i < METALS.length; i++) {
                String metal = METALS[i];
                recipes[i] = RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(items.get(metal + "_ingot"), 1)},
                        new ItemStack(items.get(metal + "_powder"), 1),
                        50f
                );
            }
            this.groups = new RecipeCrafter.RecipeGroup[]{
                    new RecipeCrafter.RecipeGroup("grinding", recipes)
            };
            this.groups[0].Texture_name = "programming-circuit3";
        }};
    }

    private static void createDecomposer() {
        分解机 = new RecipeCrafter("decomposer") {{
            size = 2;
            health = 80;
            consumePower(1.0f);
            requirements(Category.crafting, ItemStack.with(Items.copper, 15, Items.lead, 10));
            buildVisibility = BuildVisibility.shown;

            RecipeCrafter.Recipe[] recipes = new RecipeCrafter.Recipe[METALS.length * 3];
            int idx = 0;
            for (String metal : METALS) {
                recipes[idx++] = RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(items.get(metal + "_ingot"), 1)},
                        new ItemStack(items.get(metal + "_granule"), 9),
                        40f
                );
                recipes[idx++] = RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(items.get(metal + "_powder"), 1)},
                        new ItemStack(items.get(metal + "_small-pile-powder"), 4),
                        30f
                );
                recipes[idx++] = RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(items.get(metal + "_powder"), 1)},
                        new ItemStack(items.get(metal + "_pinch-powder"), 9),
                        35f
                );
            }
            this.groups = new RecipeCrafter.RecipeGroup[]{
                    new RecipeCrafter.RecipeGroup("decompose", recipes)
            };
            this.groups[0].Texture_name = "programming-circuit4";
        }};
    }

    private static void createRollingMill() {
        卷扳机 = new RecipeCrafter("rolling-mill") {{
            size = 3;
            health = 150;
            consumePower(1.8f);
            requirements(Category.crafting, ItemStack.with(Items.titanium, 25, Items.silicon, 10));
            buildVisibility = BuildVisibility.shown;

            RecipeCrafter.Recipe[] recipes = new RecipeCrafter.Recipe[METALS.length * 2];
            int idx = 0;
            for (String metal : METALS) {
                recipes[idx++] = RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(items.get(metal + "_ingot"), 1)},
                        new ItemStack(items.get(metal + "_plate"), 1),
                        60f
                );
                recipes[idx++] = RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(items.get(metal + "_ingot"), 1)},
                        new ItemStack(items.get(metal + "_foil"), 4),
                        45f
                );
            }
            this.groups = new RecipeCrafter.RecipeGroup[]{
                    new RecipeCrafter.RecipeGroup("rolling", recipes)
            };
            this.groups[0].Texture_name = "programming-circuit5";
        }};
    }
}