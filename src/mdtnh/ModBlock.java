package mdtnh;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;

import static mdtnh.BasicItems.*;

public class ModBlock {
    public static Block 熔炉, 压缩机, 粉碎机, 分解机, 卷扳机;

    private static final BasicItems.Metal[] METALS = {iron, copper, lead, tin};

    public static void load() {
        熔炉 = new RecipeCrafter("furnace") {{
            size = 2;
            health = 120;
            consumePower(1.5f);
            requirements(Category.crafting, ItemStack.with(Items.copper, 30, Items.lead, 20));
            buildVisibility = BuildVisibility.shown;

            RecipeCrafter.Recipe[] recipes = new RecipeCrafter.Recipe[METALS.length];
            for (int i = 0; i < METALS.length; i++) {
                BasicItems.Metal metal = METALS[i];
                recipes[i] = RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(metal.powder, 1)},
                        new ItemStack(metal.ingot, 1),
                        60f
                );
            }
            this.groups = new RecipeGroup[]{new RecipeGroup("smelting", recipes)};
            this.groups[0].Texture_name = "programming-circuit1";
        }};

        // 其他方块（压缩机、粉碎机等）类似，这里省略，请参考之前给出的完整代码。
        // 只需将所有 ModItems.items.get(...) 替换为对应 metal.形态 即可。
    }
}