package mdtnh;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.mod.Mod;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.consumers.ConsumeItemFlammable;
import mindustry.world.draw.DrawDefault;
import mindustry.world.meta.BuildVisibility;

import java.util.HashMap;
import java.util.Vector;

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



        MultiblockStructer testAltar = new MultiblockStructer("test-altar") {{
            size = 1;  // 建筑本身只占 1 格，检测周围的方块
            requirements(Category.crafting, ItemStack.with(Items.copper, 50));
            buildVisibility = BuildVisibility.shown;
            update = true;
            solid = true;
            buildType = MultiblockStructerBuilding::new; // 需要添加构造函数引用
            drawer=new DrawDefault();
            // 映射表
            Vector<Block> coreBlock = new Vector<>();
            coreBlock.add(Blocks.coreShard);
            Vector<Block> wallBlock = new Vector<>();
            wallBlock.add(Blocks.copperWall);
            Vector<Block> emptyBlock = new Vector<>();
            emptyBlock.add(Blocks.air);

            Vector<Vector<Block>> mapping = new Vector<>();
            mapping.add(coreBlock);  // 索引0
            mapping.add(wallBlock);  // 索引1
            mapping.add(emptyBlock); // 索引2

            // 一级结构：左右两格必须是墙，上下两格必须是空气，自身位置可以是任意
            LevelStruct level1 = new LevelStruct();
            level1.struct = new HashMap<>();
            level1.struct.put(new pos(1, 0), 1);   // 右一格 -> 墙
            level1.struct.put(new pos(-1, 0), 1);  // 左一格 -> 墙
            level1.struct.put(new pos(0, 1), 2);   // 上一格 -> 空气
            level1.struct.put(new pos(0, -1), 2);  // 下一格 -> 空气
            level1.Mapping = mapping;

            levels = new Vector<>();
            levels.add(level1);
        }};
    }

}
