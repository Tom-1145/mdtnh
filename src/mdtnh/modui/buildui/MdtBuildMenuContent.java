package mdtnh.modui.buildui;

import arc.scene.style.TextureRegionDrawable;
import mdtnh.*;
import mdtnh.energy.MdtEnergyBlocks;
import mdtnh.turret.MdtImplementedTurrets;
import mindustry.content.Blocks;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.Turret;

/**
 * MDT 建造菜单的静态内容定义。
 *
 * <p>该类只描述菜单结构：分类层级、分类图标以及各方块的归属。
 * HUD 注入、点击、导航和选择行为由 {@link MdtBuildMenuFragment} 负责。</p>
 *
 * <p>分类路径使用点号表达层级，例如
 * {@code production.voltage.furnace.electric}。同级节点和方块的界面顺序
 * 与这里的注册顺序保持一致。</p>
 */
public final class MdtBuildMenuContent {

    /** 全局 MDT 建造菜单注册表。 */
    public static final BuildMenuRegistry registry = new BuildMenuRegistry("MDT");

    /** 防止分类与方块重复注册。 */
    private static boolean loaded;

    /** 纯静态内容定义类，不允许实例化。 */
    private MdtBuildMenuContent() {}

    /**
     * 构建完整菜单树。
     *
     * <p>调用前相关方块必须已经创建完成，否则无法读取其 UI 图标。
     * 方法具有幂等保护，多次调用只会执行第一次。</p>
     */
    public static void load() {
        if (loaded) return;
        loaded = true;

        registry.root.icon = icon(MdtEnergyBlocks.exampleWire);

        // 生产设备：普通生产、电压机器和多方块设备分别使用独立子树。
        registry.category("production", "生产设备", icon(ModCrafters.multiFactory));
        registry.category("production.basic", "基础生产", icon(ModCrafters.Small_Coal_Fired_Boiler));
        registry.add("production.basic",
                ModCrafters.Small_Coal_Fired_Boiler,
                ModCrafters.multiFactory,
                ModCrafters.steamFactory
        );

        // 电压机器：先按机器族划分，再区分标准电力变体与 ULV 特殊动力变体。
        registry.category("production.voltage", "电压机器",
                icon(VoltageExampleMachines.furnace.machine(VoltageTier.LV)));
        registry.category("production.voltage.furnace", "冶炼炉族",
                icon(VoltageExampleMachines.furnace.machine(VoltageTier.LV)));
        registry.category("production.voltage.furnace.electric", "电力机器",
                icon(VoltageExampleMachines.furnace.machine(VoltageTier.LV)));
        registry.category("production.voltage.furnace.ulv-special", "ULV 特殊机器",
                icon(VoltageExampleMachines.furnace.ulvSteamMachine));

        for (VoltageTier tier : VoltageTier.values()) {
            Block machine = VoltageExampleMachines.furnace.machine(tier);
            if (machine != null) registry.add("production.voltage.furnace.electric", machine);
        }
        registry.add("production.voltage.furnace.ulv-special",
                VoltageExampleMachines.furnace.ulvSteamMachine,
                VoltageExampleMachines.furnace.ulvManualMachine
        );

        registry.category("production.voltage.processor", "材料处理器族",
                icon(VoltageExampleMachines.processor.machine(VoltageTier.ULV)));
        registry.category("production.voltage.processor.electric", "电力机器",
                icon(VoltageExampleMachines.processor.machine(VoltageTier.ULV)));
        registry.category("production.voltage.processor.ulv-special", "ULV 特殊机器",
                icon(VoltageExampleMachines.processor.ulvSteamMachine));

        for (VoltageTier tier : VoltageTier.values()) {
            Block machine = VoltageExampleMachines.processor.machine(tier);
            if (machine != null) registry.add("production.voltage.processor.electric", machine);
        }
        registry.add("production.voltage.processor.ulv-special",
                VoltageExampleMachines.processor.ulvSteamMachine,
                VoltageExampleMachines.processor.ulvManualMachine
        );

        // 多方块系统：核心与各类输入/输出舱室分组，便于继续扩展结构组件。
        registry.category("production.multiblock", "多方块设备", icon(ModCrafters.poweredAltar));
        registry.category("production.multiblock.core", "多方块核心", icon(ModCrafters.poweredAltar));
        registry.add("production.multiblock.core", ModCrafters.poweredAltar);

        registry.category("production.multiblock.hatch", "结构舱室", icon(ModCrafters.copperInputHatch));
        registry.category("production.multiblock.hatch.item", "物品舱", icon(ModCrafters.copperInputHatch));
        registry.add("production.multiblock.hatch.item",
                ModCrafters.copperInputHatch, ModCrafters.productOutputHatch);

        registry.category("production.multiblock.hatch.liquid", "液体舱", icon(ModCrafters.liquidInputHatch));
        registry.add("production.multiblock.hatch.liquid",
                ModCrafters.liquidInputHatch, ModCrafters.liquidOutputHatch);

        registry.category("production.multiblock.hatch.energy", "能源舱", icon(ModCrafters.energyInputHatch));
        registry.add("production.multiblock.hatch.energy",
                ModCrafters.energyInputHatch, ModCrafters.steamInputHatch);

        // MDT 能源：按发电、输电、储能、用电四种基本职责组织。
        registry.category("energy", "MDT 能源", icon(MdtEnergyBlocks.exampleGenerator));
        registry.category("energy.generation", "发电", icon(MdtEnergyBlocks.exampleGenerator));
        registry.add("energy.generation", MdtEnergyBlocks.exampleGenerator);

        registry.category("energy.transmission", "输电", icon(MdtEnergyBlocks.exampleWire));
        registry.add("energy.transmission", MdtEnergyBlocks.exampleWire);

        registry.category("energy.storage", "储能", icon(MdtEnergyBlocks.exampleBattery));
        registry.add("energy.storage", MdtEnergyBlocks.exampleBattery);

        registry.category("energy.consumer", "用电设备", icon(MdtEnergyBlocks.exampleConsumer));
        registry.add("energy.consumer", MdtEnergyBlocks.exampleConsumer);

        registry.category("turret","炮台",icon(Blocks.arc));
        registry.add("turret", MdtImplementedTurrets.broadsword);
        registry.add("turret", MdtImplementedTurrets.accumulated);
        registry.add("turret", MdtImplementedTurrets.dispersal);
        registry.add("turret", MdtImplementedTurrets.electricArc);
        registry.add("turret", MdtImplementedTurrets.electrode);
        registry.add("turret", MdtImplementedTurrets.excitation);
        registry.add("turret", MdtImplementedTurrets.ironWave);
        registry.add("turret", MdtImplementedTurrets.magneticRail);
        registry.add("turret", MdtImplementedTurrets.thrower);
    }

    /**
     * 将方块 UI 图标包装成菜单按钮可使用的 Drawable。
     *
     * @param block 图标来源方块
     * @return 基于 {@link Block#uiIcon} 的纹理 Drawable
     * @throws IllegalStateException 方块尚未创建时抛出，用于暴露内容加载顺序错误
     */
    private static TextureRegionDrawable icon(Block block) {
        if (block == null)
            throw new IllegalStateException("Tried to create build-menu icon from null Block.");
        return new TextureRegionDrawable(block.uiIcon);
    }
}