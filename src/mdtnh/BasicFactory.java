package mdtnh;

import mindustry.content.Items;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;
import mindustry.type.Category;
import static mdtnh.BasicItems.items;
import mindustry.world.meta.BuildVisibility;

public class BasicFactory {
    public static Block 熔炉, 压缩机, 粉碎机, 分解机, 卷扳机;

    private static final String[] METALS = {"iron", "copper", "gold", "titanium"};

    public static void load() {
        // ==================== 1. 熔炉：粉 → 锭 ====================
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
            // 分组图标使用内部名（将查找 sprites/programming-circuit1.png）
            this.groups = new RecipeGroup[]{new RecipeGroup("smelting", recipes)};
            // 为组指定图标纹理名称（不指定则使用第一个配方的输出图标）
            this.groups[0].Texture_name = "programming-circuit1";
        }};

        // ==================== 2. 压缩机：9锭 ↔ 1块 ====================
        压缩机 = new RecipeCrafter("compressor") {{
            size = 3;
            health = 200;
            consumePower(2.0f);
            requirements(Category.crafting, ItemStack.with(Items.titanium, 20, Items.silicon, 15));
            buildVisibility = BuildVisibility.shown;

            RecipeCrafter.Recipe[] recipes = new RecipeCrafter.Recipe[METALS.length * 2];
            int idx = 0;
            for (String metal : METALS) {
                // 压缩
                recipes[idx++] = RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(items.get(metal + "_ingot"), 9)},
                        new ItemStack(items.get(metal + "_block"), 1),
                        120f
                );
                // 分解（反向）
                recipes[idx++] = RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(items.get(metal + "_block"), 1)},
                        new ItemStack(items.get(metal + "_ingot"), 9),
                        80f
                );
            }
            this.groups = new RecipeGroup[]{new RecipeGroup("compress", recipes)};
            this.groups[0].Texture_name = "programming-circuit2";
        }};

        // ==================== 3. 粉碎机：锭 → 粉 ====================
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
            this.groups = new RecipeGroup[]{new RecipeGroup("grinding", recipes)};
            this.groups[0].Texture_name = "programming-circuit3";
        }};

        // ==================== 4. 分解机：锭→粒，粉→小堆/小撮 ====================
        分解机 = new RecipeCrafter("decomposer") {{
            size = 2;
            health = 80;
            consumePower(1.0f);
            requirements(Category.crafting, ItemStack.with(Items.copper, 15, Items.lead, 10));
            buildVisibility = BuildVisibility.shown;

            RecipeCrafter.Recipe[] recipes = new RecipeCrafter.Recipe[METALS.length * 3];
            int idx = 0;
            for (String metal : METALS) {
                // 锭 → 粒
                recipes[idx++] = RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(items.get(metal + "_ingot"), 1)},
                        new ItemStack(items.get(metal + "_granule"), 9),
                        40f
                );
                // 粉 → 小堆粉
                recipes[idx++] = RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(items.get(metal + "_powder"), 1)},
                        new ItemStack(items.get(metal + "_small-pile-powder"), 4),
                        30f
                );
                // 粉 → 小撮粉
                recipes[idx++] = RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(items.get(metal + "_powder"), 1)},
                        new ItemStack(items.get(metal + "_pinch-powder"), 9),
                        35f
                );
            }
            this.groups = new RecipeGroup[]{new RecipeGroup("decompose", recipes)};
            this.groups[0].Texture_name = "programming-circuit4";
        }};

        // ==================== 5. 卷扳机：锭→板，锭→箔 ====================
        卷扳机 = new RecipeCrafter("rolling-mill") {{
            size = 3;
            health = 150;
            consumePower(1.8f);
            requirements(Category.crafting, ItemStack.with(Items.titanium, 25, Items.silicon, 10));
            buildVisibility = BuildVisibility.shown;

            RecipeCrafter.Recipe[] recipes = new RecipeCrafter.Recipe[METALS.length * 2];
            int idx = 0;
            for (String metal : METALS) {
                // 锭 → 板
                recipes[idx++] = RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(items.get(metal + "_ingot"), 1)},
                        new ItemStack(items.get(metal + "_plate"), 1),
                        60f
                );
                // 锭 → 箔
                recipes[idx++] = RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(items.get(metal + "_ingot"), 1)},
                        new ItemStack(items.get(metal + "_foil"), 4),
                        45f
                );
            }
            this.groups = new RecipeGroup[]{new RecipeGroup("rolling", recipes)};
            this.groups[0].Texture_name = "programming-circuit5";
        }};
    }
}