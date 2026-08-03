package mdtnh;

import mdtnh.energy.EnergySpec;
import mdtnh.hatch.EnergyInputHatch;
import mdtnh.hatch.ItemInputHatch;
import mdtnh.hatch.ItemOutputHatch;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Liquids;
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


        // ========== 1. 注册物品输入仓 ==========
        ItemInputHatch copperInputHatch = new ItemInputHatch("copper-input-hatch") {{
            localizedName = "通用输入仓";
            itemCapacity = 20;
            requirements(Category.distribution, ItemStack.with(Items.copper, 30, Items.lead, 15));
        }};

        // ========== 2. 注册物品输出仓 ==========
        ItemOutputHatch productOutputHatch = new ItemOutputHatch("product-output-hatch") {{
            localizedName = "通用输出仓";
            itemCapacity = 20;
            requirements(Category.distribution, ItemStack.with(Items.copper, 30, Items.lead, 15));
        }};

        // ========== 3. 注册能源输入仓 ==========
        EnergyInputHatch energyInputHatch = new EnergyInputHatch("energy-input-hatch") {{
            localizedName = "能源输入仓";
            requirements(Category.power, ItemStack.with(Items.copper, 50, Items.silicon, 20));

            // 能源规格（可在构造内覆盖，也可保留基类默认值）
            energySpec.voltageV = 12f;
            energySpec.capacityJ = 4800f;   // 更大缓冲
            energySpec.maxInputA = 32;      // 接受高达32A
            // energySpec.maxOutputA 保持 0，防止向外电网放电
        }};

        // ========== 4. 注册多配方工厂（含能源） ==========
        RecipeCrafter multiFactory = new RecipeCrafter("multi-factory") {{
            size = 2;
            health = 300;
            requirements(Category.crafting, ItemStack.with(Items.copper, 80, Items.silicon, 40));

            // 能源配置
            energySpec.role = EnergySpec.Role.consumer;
            energySpec.voltageV = 12f;
            energySpec.capacityJ = 720f;     // 内部缓冲
            energySpec.maxInputA = 12;
            energySpec.maxOutputA = 0;

            // 配方组
            RecipeGroup groupMetals = new RecipeGroup(
                    "metals",
                    new RecipeCrafter.Recipe[]{
                            RecipeCrafter.Recipe.items(
                                    new ItemStack[]{new ItemStack(Items.copper, 3), new ItemStack(Items.lead, 2)},
                                    new ItemStack(Items.graphite, 1), 60f
                            ).energy(144f), // 12V * 2A * 6秒？可根据需要调整
                            RecipeCrafter.Recipe.items(
                                    new ItemStack[]{new ItemStack(Items.titanium, 2)},
                                    new ItemStack(Items.silicon, 2), 50f
                            ).energy(200f)
                    }
            );

            RecipeGroup groupElectronics = new RecipeGroup(
                    "electronics",
                    new RecipeCrafter.Recipe[]{
                            RecipeCrafter.Recipe.items(
                                    new ItemStack[]{new ItemStack(Items.copper, 1), new ItemStack(Items.silicon, 2)},
                                    new ItemStack(Items.metaglass, 2), 90f
                            ).energy(360f),
                            RecipeCrafter.Recipe.withLiquid(
                                    new ItemStack[]{new ItemStack(Items.silicon, 3)},
                                    new LiquidStack[]{new LiquidStack(Liquids.water, 0.1f)},
                                    new ItemStack(Items.surgeAlloy, 1),
                                    null, 120f
                            ).energy(500f)
                    }
            );

            groups = new RecipeCrafter.RecipeGroup[]{groupMetals, groupElectronics};
        }};

        // ========== 5. 注册多方块结构（含能源仓） ==========
        MultiblockStructer poweredAltar = new MultiblockStructer("powered-altar") {{
            size = 1;
            requirements(Category.crafting, ItemStack.with(Items.copper, 100, Items.silicon, 50));
            buildVisibility = BuildVisibility.shown;

            // 结构映射：核心、输入仓、输出仓、能源仓、空气
            Vector<Block> core = new Vector<>(); core.add(this);
            Vector<Block> in = new Vector<>(); in.add(copperInputHatch);
            Vector<Block> out = new Vector<>(); out.add(productOutputHatch);
            Vector<Block> energy = new Vector<>(); energy.add(energyInputHatch);
            Vector<Block> air = new Vector<>(); air.add(Blocks.air);

            List<List<Block>> mapping = new Vector<>();
            mapping.add(core);   // 0
            mapping.add(in);     // 1
            mapping.add(out);    // 2
            mapping.add(energy); // 3
            mapping.add(air);    // 4

            LevelStruct level1 = new LevelStruct();
            level1.struct = new HashMap<>();
            level1.struct.put(new pos(0, 0), 0);   // 核心自身
            level1.struct.put(new pos(1, 0), 1);   // 右 → 输入仓
            level1.struct.put(new pos(-1, 0), 1);  // 左 → 输入仓
            level1.struct.put(new pos(0, 1), 2);   // 上 → 输出仓
            level1.struct.put(new pos(0, -1), 4);  // 下 → 空气
            level1.struct.put(new pos(2, 0), 3);   // 右第二格 → 能源输入仓

            level1.Mapping = mapping;
            level1.inputs = new pos[]{new pos(1, 0), new pos(-1, 0)};
            level1.outputs = new pos[]{new pos(0, 1)};
            level1.energyInputs = new pos[]{new pos(2, 0)};   // 能源仓位置

            levels = new Vector<>();
            levels.add(level1);

            // 配方组（带能耗）
            groups = new MultiblockStructer.RecipeGroup[]{
                    new MultiblockStructer.RecipeGroup("smelting", new MultiblockStructer.Recipe[]{
                            MultiblockStructer.Recipe.items(
                                    new ItemStack[]{new ItemStack(Items.copper, 4), new ItemStack(Items.lead, 2)},
                                    new ItemStack(Items.silicon, 2),
                                    180f
                            ).energy(360f),
                            MultiblockStructer.Recipe.items(
                                    new ItemStack[]{new ItemStack(Items.titanium, 3), new ItemStack(Items.silicon, 2)},
                                    new ItemStack(Items.surgeAlloy, 1),
                                    240f
                            ).energy(720f)
                    }),
                    new MultiblockStructer.RecipeGroup("advanced", new MultiblockStructer.Recipe[]{
                            MultiblockStructer.Recipe.items(
                                    new ItemStack[]{new ItemStack(Items.surgeAlloy, 1), new ItemStack(Items.phaseFabric, 1)},
                                    new ItemStack(Items.plastanium, 2),
                                    300f
                            ).energy(1200f)
                    })
            };
            // 可选自定义贴图
            groups[0].Texture_name = "programming-circuit1";
            groups[1].Texture_name = "programming-circuit2";
        }};
    }
}
