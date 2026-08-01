package mdtnh;

import mdtnh.hatch.ItemInputHatch;
import mdtnh.hatch.ItemOutputHatch;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.ctype.ContentType;
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
import java.util.List;
import java.util.Vector;

public class ModCrafters {
    public static GenericCrafter Small_Coal_Fired_Boiler;
    public static RecipeCrafter test;

    public static void load() {
        Small_Coal_Fired_Boiler = new GenericCrafter("small-coal-fired-boiler") {{
            health = 100;
            size = 2;
            //使用此重载，Anuke会贴心地帮你设置成BuildVisibility.shown
            requirements(Category.crafting, ItemStack.with(Items.copper, 50));
        }};
        Small_Coal_Fired_Boiler.outputLiquid = new LiquidStack(ModLiquids.steam, 1);
        Small_Coal_Fired_Boiler.craftTime = 60;
        Small_Coal_Fired_Boiler.drawer = new DrawDefault();
        Small_Coal_Fired_Boiler.consume(new ConsumeItemFlammable());
        ;
        Small_Coal_Fired_Boiler.consumeLiquid(Liquids.water, 1);


        test = new RecipeCrafter("multi-factory") {{
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

            groups = new RecipeGroup[]{groupMetals, groupElectronics};
        }};
        test.size = 2;
        test.health = 300;


        // 在 MainMod.loadContent() 或其他方块初始化处注册舱室
        ItemInputHatch copperInput = new ItemInputHatch("copper-input-hatch") {{
            localizedName = "铜质输入仓";
            itemCapacity = 20;
        }};
        ItemOutputHatch productOutput = new ItemOutputHatch("product-output-hatch") {{
            localizedName = "产品输出仓";
            itemCapacity = 20;
        }};
        // 假设已在模组中注册了以下舱室方块：
// ItemInputHatch  "copper-input-hatch"   (内部名: mdtnh-copper-input-hatch)
// ItemOutputHatch "product-output-hatch" (内部名: mdtnh-product-output-hatch)

        MultiblockStructer testAltar = new MultiblockStructer("test-altar") {{
            size = 1;
            requirements(Category.crafting, ItemStack.with(Items.copper, 80, Items.lead, 40));
            buildVisibility = BuildVisibility.shown;

            // 结构映射表
            Vector<Block> core = new Vector<>();
            core.add(this);         // 0: 核心
            Vector<Block> in = new Vector<>();
            in.add(copperInput);     // 1: 输入仓
            Vector<Block> out = new Vector<>();
            out.add(productOutput);   // 2: 输出仓
            Vector<Block> air = new Vector<>();
            air.add(Blocks.air);    // 3: 空气

            List<List<Block>> mapping = new Vector<>();
            mapping.add(core);
            mapping.add(in);
            mapping.add(out);
            mapping.add(air);

            // 结构布局 (偏移 -> 类型索引)
            LevelStruct level1 = new LevelStruct();
            level1.struct = new HashMap<>();
            level1.struct.put(new pos(0, 0), 0);   // 中心 → 核心
            level1.struct.put(new pos(1, 0), 1);   // 右   → 输入仓
            level1.struct.put(new pos(-1, 0), 1);  // 左   → 输入仓
            level1.struct.put(new pos(0, 1), 2);   // 上   → 输出仓
            level1.struct.put(new pos(0, -1), 3);  // 下   → 空气
            level1.Mapping = mapping;
            level1.inputs = new pos[]{new pos(1, 0), new pos(-1, 0)};
            level1.outputs = new pos[]{new pos(0, 1)};

            levels = new Vector<>();
            levels.add(level1);

            // 配方组
            groups = new RecipeGroup[]{
                    // 组1: 基础电子
                    new RecipeGroup("electronics", new Recipe[]{
                            new Recipe(
                                    new ItemStack[]{new ItemStack(Items.copper, 3), new ItemStack(Items.lead, 2)},
                                    new ItemStack(Items.silicon, 1),
                                    120f
                            ),
                            new Recipe(
                                    new ItemStack[]{new ItemStack(Items.copper, 5), new ItemStack(Items.lead, 3), new ItemStack(Items.sand, 2)},
                                    new ItemStack(Items.metaglass, 1),
                                    180f
                            )
                    }),
                    // 组2: 合金
                    new RecipeGroup("alloy", new Recipe[]{
                            new Recipe(
                                    new ItemStack[]{new ItemStack(Items.copper, 4), new ItemStack(Items.titanium, 2)},
                                    new ItemStack(Items.surgeAlloy, 1),
                                    200f
                            )
                    })
            };
        }};
    }
}
