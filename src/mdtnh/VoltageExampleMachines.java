package mdtnh;

import arc.util.Log;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;

/**
 * 基于 {@link VoltageRecipeRegistry} 的示例机器族。
 *
 * <p>每创建一个注册器都会生成 17 个方块：15 台电力机器（ULV 到 MAX）、
 * 1 台 ULV 蒸汽机和 1 台 ULV 手动机。注册配方的耗时与能耗按最低等级给定，
 * 注册器会自动派生所有更高电压等级的超频版本。</p>
 *
 * <p>示例覆盖了多个电压等级：ULV 的基础蚀刻、LV/MV/HV 的冶炼、
 * EV 的合金锻造以及 IV 的量子组件，用于演示配方按等级分发与超频关系。</p>
 */
public class VoltageExampleMachines {

    /** 通用冶炼炉机器族及其分级配方注册器。 */
    public static VoltageRecipeRegistry furnace;

    /** 材料处理器机器族及其分级配方注册器。 */
    public static VoltageRecipeRegistry processor;

    /**
     * 在内容加载阶段创建机器族并注册配方。
     *
     * <p>必须在进入地图前调用；游戏运行中不能再新增内容。</p>
     */
    public static void load() {

        /*
         * 冶炼炉族配方从 LV 起步并逐级覆盖到 EV。
         * 不同最低等级用于体现配方只向同级及更高等级机器派生的规则。
         */
        furnace = new VoltageRecipeRegistry("example-furnace");

        // LV：煤 x2 -> 石墨 x1，120 tick / 240J
        furnace.register("smelting", VoltageTier.LV,
                RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(Items.coal, 2)},
                        new ItemStack(Items.graphite, 1),
                        120f
                ).energy(240f));

        // MV：砂 x2 + 铅 x1 -> 玻璃 x1，90 tick / 720J
        furnace.register("smelting", VoltageTier.MV,
                RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(Items.sand, 2), new ItemStack(Items.lead, 1)},
                        new ItemStack(Items.metaglass, 1),
                        90f
                ).energy(720f));

        // HV：煤 x2 + 砂 x2 -> 硅 x1，60 tick / 1920J（使用显式耗时能耗重载演示）
        furnace.register("smelting", VoltageTier.HV,
                RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(Items.coal, 2), new ItemStack(Items.sand, 2)},
                        new ItemStack(Items.silicon, 1),
                        1f
                ),
                1920f,  // HV 总能耗
                60f     // HV 耗时
        );

        // EV：钛 x2 + 油 15 -> 塑钢 x1 + 冷冻液 10（液体配方，80 tick / 3840J）
        furnace.register("smelting", VoltageTier.EV,
                RecipeCrafter.Recipe.withLiquid(
                        new ItemStack[]{new ItemStack(Items.titanium, 2)},
                        new LiquidStack[]{new LiquidStack(Liquids.oil, 15f)},
                        new ItemStack[]{new ItemStack(Items.plastanium, 1)},
                        new LiquidStack[]{new LiquidStack(Liquids.cryofluid, 10f)},
                        80f
                ).energy(3840f));

        customizeFamily(furnace, "示例冶炼炉", 40, 40f);


        /*
         * 材料处理器覆盖 ULV、EV 和 IV 等级。
         * ULV 配方会额外进入蒸汽机与手动机，覆盖特殊动力机器分支。
         */
        processor = new VoltageRecipeRegistry("example-processor");

        // ULV：铜 x1 + 铅 x1 -> 硅 x1，120 tick / 96J
        // 该等级配方会同时出现在 ULV 蒸汽机与 ULV 手动机上。
        processor.register("circuits", VoltageTier.ULV,
                RecipeCrafter.Recipe.items(
                        new ItemStack[]{new ItemStack(Items.copper, 1), new ItemStack(Items.lead, 1)},
                        new ItemStack(Items.silicon, 1),
                        120f
                ).energy(96f));

        // EV：铜 x2 + 铅 x2 + 钛 x1 + 硅 x1 -> 合金 x1，240 tick / 7680J
        processor.register("advanced", VoltageTier.EV,
                RecipeCrafter.Recipe.items(
                        new ItemStack[]{
                                new ItemStack(Items.copper, 2),
                                new ItemStack(Items.lead, 2),
                                new ItemStack(Items.titanium, 1),
                                new ItemStack(Items.silicon, 1)
                        },
                        new ItemStack(Items.surgeAlloy, 1),
                        240f
                ).energy(7680f));

        // IV：合金 x2 + 相位织物 x1 + 水 20 -> 冲击化合物 x2，200 tick / 15360J
        processor.register("advanced", VoltageTier.IV,
                RecipeCrafter.Recipe.withLiquid(
                        new ItemStack[]{new ItemStack(Items.surgeAlloy, 2), new ItemStack(Items.phaseFabric, 1)},
                        new LiquidStack[]{new LiquidStack(Liquids.water, 20f)},
                        new ItemStack[]{new ItemStack(Items.blastCompound, 2)},
                        null,
                        200f
                ).energy(15360f));

        customizeFamily(processor, "示例处理器", 30, 30f);

        Log.info("VoltageExampleMachines loaded: furnace + processor families.");
    }

    /**
     * 为机器族所有变体统一设置显示信息和物料容量。
     *
     * <p>标准电力机器按等级生成名称和输入电压说明；
     * ULV 蒸汽机和手动机使用独立说明，明确能源来源与配方限制。</p>
     *
     * @param registry        要配置的机器族
     * @param baseLocalName  机器族基础中文名称
     * @param itemCapacity   物品容量
     * @param liquidCapacity 液体容量
     */
    private static void customizeFamily(VoltageRecipeRegistry registry, String baseLocalName,
                                        int itemCapacity, float liquidCapacity) {
        for (VoltageTier tier : VoltageTier.values()) {
            RecipeCrafter machine = registry.machine(tier);
            machine.localizedName = tier.displayName + " " + baseLocalName;
            machine.description = "执行不同电压等级配方的示例" + baseLocalName + "。"
                    + "输入电压区间 " + Math.round(tier.minVoltageV) + "V ~ "
                    + Math.round(tier.maxVoltageV) + "V。";
            machine.itemCapacity = itemCapacity;
            machine.liquidCapacity = liquidCapacity;
        }

        registry.ulvSteamMachine.localizedName = "ULV " + baseLocalName + "(蒸汽)";
        registry.ulvSteamMachine.description = "以蒸汽为内部缓存充能的" + baseLocalName
                + "，只能执行 ULV 配方，不连接 MDT 电线。";
        registry.ulvSteamMachine.itemCapacity = itemCapacity;
        registry.ulvSteamMachine.liquidCapacity = liquidCapacity;

        registry.ulvManualMachine.localizedName = "ULV " + baseLocalName + "(手动)";
        registry.ulvManualMachine.description = "不消耗能源的" + baseLocalName
                + "，只能执行 ULV 配方，耗时为基准的 4 倍。";
        registry.ulvManualMachine.itemCapacity = itemCapacity;
        registry.ulvManualMachine.liquidCapacity = liquidCapacity;
    }
}
