package mdtnh;

import mindustry.Vars;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.mod.Mod;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.consumers.ConsumeItemFlammable;
import mindustry.world.draw.DrawDefault;

public class ModCrafters {
    public static GenericCrafter Small_Coal_Fired_Boiler;
    public static RecipeCrafter test;

    public static void load(Mod mod){
        Small_Coal_Fired_Boiler=new GenericCrafter("small-coal-fired-boiler"){{
            health = 100;
            size = 2;
            //使用此重载，Anuke会贴心地帮你设置成BuildVisibility.shown
            requirements(Category.crafting, ItemStack.with(Items.copper,50));
        }};
        Small_Coal_Fired_Boiler.outputLiquid=new LiquidStack(ModLiquids.steam, 1);
        Small_Coal_Fired_Boiler.craftTime=60;
        Small_Coal_Fired_Boiler.drawer=new DrawDefault();
        Small_Coal_Fired_Boiler.consume(new ConsumeItemFlammable());;
        Small_Coal_Fired_Boiler.consumeLiquid(Liquids.water,1);


        test = new RecipeCrafter("multi-factory"){{
            // 准备各组配方
            RecipeGroup groupMetals = new RecipeGroup(
                    "Programming-circuit-1",          // 可选：组图标
                    new RecipeCrafter.Recipe[]{
                            RecipeCrafter.Recipe.items(
                                    new ItemStack[]{new ItemStack(Items.copper, 3), new ItemStack(Items.lead, 2)},
                                    new ItemStack(Items.graphite, 1), 60f
                            ),
                            RecipeCrafter.Recipe.items(
                                    new ItemStack[]{new ItemStack(Items.titanium, 2)},
                                    new ItemStack(Items.silicon, 2), 50f
                            )
                    }
            );

            RecipeGroup groupElectronics = new RecipeGroup(
                    "Programming-circuit-2",
                    new RecipeCrafter.Recipe[]{
                            RecipeCrafter.Recipe.items(
                                    new ItemStack[]{new ItemStack(Items.copper, 1), new ItemStack(Items.silicon, 2)},
                                    new ItemStack(Items.metaglass, 2), 90f
                            ),
                            RecipeCrafter.Recipe.withLiquid(
                                    new ItemStack[]{new ItemStack(Items.silicon, 3)},
                                    new LiquidStack[]{new LiquidStack(Liquids.water, 0.1f)},
                                    new ItemStack(Items.surgeAlloy, 1),
                                    null, 120f
                            )
                    }
            );

            groups = new RecipeGroup[]{ groupMetals, groupElectronics };
        }};
        test.size = 2;
        test.health = 300;
    }

}
