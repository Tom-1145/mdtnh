package mdtnh.modui.buildui;

import arc.scene.style.TextureRegionDrawable;
import mdtnh.*;
import mdtnh.energy.MdtEnergyBlocks;
import mindustry.world.Block;

public final class MdtBuildMenuContent {
    public static final BuildMenuRegistry registry = new BuildMenuRegistry("MDT");
    private static boolean loaded;

    private MdtBuildMenuContent() {}

    public static void load() {
        if (loaded) return;
        loaded = true;

        registry.root.icon = icon(MdtEnergyBlocks.exampleWire);

        // 生产设备
        registry.category("production", "生产设备", icon(ModCrafters.multiFactory));
        registry.category("production.basic", "基础生产", icon(ModCrafters.Small_Coal_Fired_Boiler));
        registry.add("production.basic",
                ModCrafters.Small_Coal_Fired_Boiler,
                ModCrafters.multiFactory,
                ModCrafters.steamFactory
        );

        // 电压机器
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

        // 多方块系统
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

        // MDT 能源
        registry.category("energy", "MDT 能源", icon(MdtEnergyBlocks.exampleGenerator));
        registry.category("energy.generation", "发电", icon(MdtEnergyBlocks.exampleGenerator));
        registry.add("energy.generation", MdtEnergyBlocks.exampleGenerator);

        registry.category("energy.transmission", "输电", icon(MdtEnergyBlocks.exampleWire));
        registry.add("energy.transmission", MdtEnergyBlocks.exampleWire);

        registry.category("energy.storage", "储能", icon(MdtEnergyBlocks.exampleBattery));
        registry.add("energy.storage", MdtEnergyBlocks.exampleBattery);

        registry.category("energy.consumer", "用电设备", icon(MdtEnergyBlocks.exampleConsumer));
        registry.add("energy.consumer", MdtEnergyBlocks.exampleConsumer);
    }

    private static TextureRegionDrawable icon(Block block) {
        if (block == null)
            throw new IllegalStateException("Tried to create build-menu icon from null Block.");
        return new TextureRegionDrawable(block.uiIcon);
    }
}