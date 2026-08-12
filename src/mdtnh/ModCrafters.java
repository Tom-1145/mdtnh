package mdtnh;

import mdtnh.energy.EnergySpec;
import mdtnh.hatch.EnergyInputHatch;
import mdtnh.hatch.ItemInputHatch;
import mdtnh.hatch.ItemOutputHatch;
import mdtnh.hatch.LiquidInputHatch;
import mdtnh.hatch.LiquidOutputHatch;
import mdtnh.hatch.SteamInputHatch;
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

/**
 * 注册项目中的生产建筑、舱室和多方块结构示例。
 *
 * <p>该类只负责内容定义与参数装配，不保存运行时状态。每个已放置建筑的生产进度、
 * 物品、液体和能量由相应的 Building 子类独立维护。</p>
 */
public class ModCrafters {

    /** 使用 Mindustry 原生生产逻辑的蒸汽锅炉。 */
    public static Boiler Small_Coal_Fired_Boiler;

    /** 供其他内容注册代码访问的多配方工厂引用。 */
    public static RecipeCrafter multiFactory;
    @Deprecated
    public static RecipeCrafter test;

    /** 直接消耗蒸汽、无法连接电线的示例多配方工厂。 */
    public static SteamRecipeCrafter steamFactory;

    /**
     * 创建所有生产相关内容。
     *
     * <p>舱室实例先于多方块核心注册，因为结构映射需要直接引用允许出现的方块对象。</p>
     */
    // ---------- 多方块舱室 ----------
    public static ItemInputHatch copperInputHatch;
    public static ItemOutputHatch productOutputHatch;

    public static LiquidInputHatch liquidInputHatch;
    public static LiquidOutputHatch liquidOutputHatch;

    public static EnergyInputHatch energyInputHatch;
    public static SteamInputHatch steamInputHatch;

    // ---------- 多方块核心 ----------
    public static MultiblockStructer poweredAltar;

    public static ModDrill testDrill;
    public static void load() {

        // 小型锅炉消耗可燃物和水，每 60 tick 生产一单位蒸汽。
        Small_Coal_Fired_Boiler = new Boiler("small-coal-fired-boiler") {{
            health = 100;
            size = 2;
            requirements(Category.crafting, ItemStack.with(Items.copper, 50));
            fuelList = new FuelList(
                    Items.coal,new FuelList.FuelProp(new ItemStack(ModItems.tinyPileOfDarkAsh,1),10800f,1F/3F)
            );
            maxHeat = 500;
            maxSteamAmount = 3200;
            maxWaterAmount = 1600;
            productSpeed = 0.005f;
            heatLoseSpeed = 0.01f;
            heatSpeed = 0.06944f;
        }};
        /*
         * 物品输入仓由传送设备写入原料，但不会主动把内容 dump 到外部。
         * 多方块核心会根据当前配方从指定输入仓中统一取料。
         */

        // 多配方工厂
        /*
        重名内容，为保证其正常加载，已被注释
        multiFactory = new RecipeCrafter("multi-factory") {{
            size = 2;
            health = 300;
            requirements(Category.crafting, ItemStack.with(Items.copper, 80, Items.silicon, 40));
            energySpec.role = EnergySpec.Role.consumer;
            energySpec.voltageV = 12f;
            energySpec.capacityJ = 720f;
            energySpec.maxInputA = 12;
            energySpec.maxOutputA = 0;

            RecipeGroup groupMetals = new RecipeGroup("metals", new RecipeCrafter.Recipe[]{
                    RecipeCrafter.Recipe.items(new ItemStack[]{new ItemStack(Items.copper,3), new ItemStack(Items.lead,2)}, new ItemStack(Items.graphite,1), 60f).energy(144f),
                    RecipeCrafter.Recipe.items(new ItemStack[]{new ItemStack(Items.titanium,2)}, new ItemStack(Items.silicon,2), 50f).energy(200f)
            });
            RecipeGroup groupElectronics = new RecipeGroup("electronics", new RecipeCrafter.Recipe[]{
                    RecipeCrafter.Recipe.items(new ItemStack[]{new ItemStack(Items.copper,1), new ItemStack(Items.silicon,2)}, new ItemStack(Items.metaglass,2), 90f).energy(360f),
                    RecipeCrafter.Recipe.withLiquid(new ItemStack[]{new ItemStack(Items.silicon,3)}, new LiquidStack[]{new LiquidStack(Liquids.water,0.1f)}, new ItemStack(Items.surgeAlloy,1), null, 120f).energy(500f)
            });
            groups = new RecipeCrafter.RecipeGroup[]{groupMetals, groupElectronics};
        }};
        */
        test = multiFactory;

        // 蒸汽多配方工厂（假设已在 SteamRecipeCrafter 中完成定义）
        steamFactory = new SteamRecipeCrafter("steam-factory") {{
            size = 2;
            health = 300;
            requirements(Category.crafting, ItemStack.with(Items.copper, 70, Items.lead, 50));
            // 具体配方在 SteamRecipeCrafter 构造中定义
        }};

        // 注册物品舱室
        copperInputHatch = new ItemInputHatch("copper-input-hatch") {{
            localizedName = "通用输入仓";
            itemCapacity = 20;
            requirements(Category.distribution, ItemStack.with(Items.copper, 30, Items.lead, 15));
        }};

        /*
         * 物品输出仓由多方块核心直接写入产物，其容量决定结构是否具备完整输出空间。
         */
        ItemOutputHatch productOutputHatch = new ItemOutputHatch("product-output-hatch") {{
            localizedName = "通用输出仓";
            itemCapacity = 20;
            requirements(Category.distribution, ItemStack.with(Items.copper, 30, Items.lead, 15));
        }};

        /*
         * 液体输入仓接受管道输入的原料液体，但不会主动输出，
         * 多方块核心根据当前配方从其中取料。
         */
        //怎么还有？
        liquidInputHatch = new LiquidInputHatch("liquid-input-hatch") {{
            localizedName = "液体输入仓";
            liquidCapacity = 60f;
            requirements(Category.distribution, ItemStack.with(Items.copper, 30, Items.lead, 15));
        }};

        /*
         * 液体输出仓由多方块核心直接写入产物液体，
         * 其容量决定结构是否具备完整的液体输出空间。
         */
        //你看，又有
        liquidOutputHatch = new LiquidOutputHatch("liquid-output-hatch") {{
            localizedName = "液体输出仓";
            liquidCapacity = 60f;
            requirements(Category.distribution, ItemStack.with(Items.copper, 30, Items.lead, 15));
        }};

        /*
         * 能源输入仓是外部 MDT 能源网络与多方块核心之间的缓冲。
         * 它以消费者身份接收电流包，核心生产时再从其 EnergyState 中扣除焦耳。
         */
        //还有
        energyInputHatch = new EnergyInputHatch("energy-input-hatch") {{
            localizedName = "能源输入仓";
            requirements(Category.power, ItemStack.with(Items.copper, 50, Items.silicon, 20));

            energySpec.voltageV = 12f;
            energySpec.minInputVoltageV = 10f;
            energySpec.maxInputVoltageV = 14f;
            energySpec.capacityJ = 4800f;
            energySpec.maxInputA = 32;
        }};


        /*
         * 蒸汽仓接收管道输入的蒸汽，并把它转换为多方块结构可消费的内部焦耳缓存。
         * 它继承能源仓接口，但明确关闭电网连接，因此导线贴在旁边也不会建立连接。
         */
        steamInputHatch = new SteamInputHatch("steam-input-hatch") {{
            localizedName = "蒸汽能源仓";
            description = "接收蒸汽并转换为内部能源缓存，不能连接 MDT 电线。";
            requirements(Category.power, ItemStack.with(Items.copper, 45, Items.lead, 30));

            liquidCapacity = 40f;
            energySpec.capacityJ = 4800f;
            joulesPerSteamUnit = 120f;
            maxSteamUsePerSecond = 2f;
        }};

        /*
         * 单方块多配方工厂直接实现 MdtEnergyNode。
         * 其内部缓存由导线网络充电，配方每 tick 按总能耗比例扣除能量。
         */
        //出现了两个重名内容
        multiFactory = new RecipeCrafter("multi-factory") {{
            size = 2;
            health = 300;
            requirements(Category.crafting, ItemStack.with(Items.copper, 80, Items.silicon, 40));

            // 工厂只接收电力，不向网络提供能量。
            energySpec.role = EnergySpec.Role.consumer;
            energySpec.voltageV = 12f;
            energySpec.minInputVoltageV = 10f;
            energySpec.maxInputVoltageV = 14f;
            energySpec.capacityJ = 720f;
            energySpec.maxInputA = 12;
            energySpec.maxOutputA = 0;

            /*
             * 配方的 energy(joules) 表示完成一次生产周期需要的总能量。
             * 实际运行时会按照 delta / craftTime 均匀分摊到每个 tick。
             */
            RecipeGroup groupMetals = new RecipeGroup(
                    "metals",
                    new RecipeCrafter.Recipe[]{
                            RecipeCrafter.Recipe.items(
                                    new ItemStack[]{new ItemStack(Items.copper, 3), new ItemStack(Items.lead, 2)},
                                    new ItemStack(Items.graphite, 1), 60f
                            ).energy(144f),
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
                                    new ItemStack[]{new ItemStack(Items.surgeAlloy, 1), new ItemStack(Items.metaglass, 1)},
                                    new LiquidStack[]{new LiquidStack(Liquids.oil, 0.05f)},
                                    120f
                            ).energy(500f)
                    }
            );

            groups = new RecipeCrafter.RecipeGroup[]{groupMetals, groupElectronics};
        }};


        /*
         * 蒸汽工厂复用 RecipeCrafter 的配方与内部 EnergyState，但充能来源改为蒸汽。
         * 1 单位蒸汽提供 120J，转换吞吐为每秒 1 单位，因此持续功率上限为 120J/s。
         */
        steamFactory = new SteamRecipeCrafter("steam-multi-factory") {{
            localizedName = "蒸汽多配方工厂";
            description = "消耗蒸汽为内部缓存充能，不接受 MDT 电线供电。";
            size = 2;
            health = 320;
            requirements(Category.crafting, ItemStack.with(
                    Items.copper, 70,
                    Items.lead, 50,
                    Items.graphite, 25
            ));

            liquidCapacity = 30f;
            energySpec.capacityJ = 720f;
            joulesPerSteamUnit = 120f;
            maxSteamUsePerSecond = 1f;

            groups = new RecipeCrafter.RecipeGroup[]{
                    new RecipeCrafter.RecipeGroup("steam-processing", new RecipeCrafter.Recipe[]{
                            RecipeCrafter.Recipe.items(
                                    new ItemStack[]{
                                            new ItemStack(Items.copper, 2),
                                            new ItemStack(Items.lead, 1)
                                    },
                                    new ItemStack(Items.graphite, 1),
                                    60f
                            ).energy(96f)
                    })
            };
        }};

        /*
         * 多方块核心本身不接入外部能源网络。
         * 它通过 LevelStruct.energyInputs 定位结构中的能源仓，并直接消费仓内缓存。
         */
        //你把它当局部变量玩了
        poweredAltar = new MultiblockStructer("powered-altar") {{
            size = 1;
            requirements(Category.crafting, ItemStack.with(Items.copper, 100, Items.silicon, 50));
            buildVisibility = BuildVisibility.shown;

            /*
             * Mapping 的每个下标代表一种结构槽位类型。
             * struct 中保存“相对坐标 -> Mapping 下标”，检测时只要当前位置方块属于
             * 对应允许列表即可通过，因此一个槽位类型可以接受多个可替换方块。
             */
            Vector<Block> core = new Vector<>(); core.add(this);
            Vector<Block> in = new Vector<>(); in.add(copperInputHatch);
            Vector<Block> out = new Vector<>(); out.add(productOutputHatch);
            Vector<Block> liqIn = new Vector<>(); liqIn.add(liquidInputHatch);
            Vector<Block> liqOut = new Vector<>(); liqOut.add(liquidOutputHatch);
            Vector<Block> energy = new Vector<>();
            energy.add(energyInputHatch);
            energy.add(steamInputHatch);
            Vector<Block> air = new Vector<>(); air.add(Blocks.air);

            List<List<Block>> mapping = new Vector<>();
            mapping.add(core);   // 类型 0：核心方块
            mapping.add(in);     // 类型 1：物品输入仓
            mapping.add(out);    // 类型 2：物品输出仓
            mapping.add(energy); // 类型 3：能源输入仓
            mapping.add(air);    // 类型 4：必须为空的结构槽位
            mapping.add(liqIn);  // 类型 5：液体输入仓
            mapping.add(liqOut); // 类型 6：液体输出仓

            /*
             * 所有坐标都以多方块核心 tile 为原点。
             * 当前实现不随核心旋转变换坐标，因此定义的是固定世界方向结构。
             */
            LevelStruct level1 = new LevelStruct();
            level1.struct = new HashMap<>();
            level1.struct.put(new pos(0, 0), 0);
            level1.struct.put(new pos(1, 0), 1);
            level1.struct.put(new pos(-1, 0), 1);
            level1.struct.put(new pos(0, 1), 2);
            level1.struct.put(new pos(0, -1), 4);
            level1.struct.put(new pos(2, 0), 3);
            level1.struct.put(new pos(1, 1), 5);
            level1.struct.put(new pos(-1, 1), 6);
            /*
               IO LI
            II C  II EI
               _  LO
              */

            level1.Mapping = mapping;
            levels = new Vector<>();
            levels.add(level1);

            /*
             * 多方块配方的能耗同样表示完成一次生产需要的总焦耳数。
             * 结构必须同时具备足够原料、输出空间和能源，进度才会继续推进。
             */
            groups = new MultiblockStructer.RecipeGroup[]{
                    new MultiblockStructer.RecipeGroup("smelting", new MultiblockStructer.Recipe[]{
                            MultiblockStructer.Recipe.items(
                                    new ItemStack[]{new ItemStack(Items.copper, 4), new ItemStack(Items.lead, 2)},
                                    new ItemStack[]{new ItemStack(Items.silicon, 2)},
                                    180f
                            ).energy(360f),
                            MultiblockStructer.Recipe.withLiquid(
                                    new ItemStack[]{new ItemStack(Items.titanium, 3), new ItemStack(Items.silicon, 2)},
                                    new LiquidStack[]{new LiquidStack(Liquids.water, 30f)},
                                    new ItemStack[]{new ItemStack(Items.surgeAlloy, 1), new ItemStack(Items.metaglass, 1)},
                                    new LiquidStack[]{new LiquidStack(ModLiquids.steam, 10f)},
                                    240f
                            ).energy(720f)
                    }),
                    new MultiblockStructer.RecipeGroup("advanced", new MultiblockStructer.Recipe[]{
                            MultiblockStructer.Recipe.items(
                                    new ItemStack[]{new ItemStack(Items.surgeAlloy, 1), new ItemStack(Items.phaseFabric, 1)},
                                    new ItemStack[]{new ItemStack(Items.plastanium, 2)},
                                    300f
                            ).energy(1200f)
                    })
            };

            // 配置界面优先尝试从模组图集中读取这些配方组图标。
            groups[0].Texture_name = "programming-circuit1";
            groups[1].Texture_name = "programming-circuit2";
        }};

        testDrill=new ModDrill("testdrill");
    }
}
