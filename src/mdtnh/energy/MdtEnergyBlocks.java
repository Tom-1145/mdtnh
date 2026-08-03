package mdtnh.energy;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.meta.BuildVisibility;

/** Registers the four example blocks. */
public final class MdtEnergyBlocks {
    public static MdtEnergyBlock exampleGenerator;
    public static MdtEnergyBlock exampleWire;
    public static MdtEnergyBlock exampleConsumer;
    public static MdtEnergyBlock exampleBattery;

    private MdtEnergyBlocks() {
    }

    public static void load() {
        exampleGenerator = new MdtEnergyBlock("example-generator") {{
            localizedName = "示例发电机";
            description = "每秒自动产生 96 J；以 12 V、最多 8 A 向相邻导线网络输出。";
            fallbackRegion = "combustion-generator";
            role = EnergyRole.generator;

            voltageV = 12f;
            capacityJ = 2400f;
            initialEnergyFraction = 0f;
            generationJPerSecond = 96f;
            maxInputA = 0;
            maxOutputA = 8;

            health = 260;
            size = 1;
            alwaysUnlocked = true;
            buildVisibility = BuildVisibility.shown;
            requirements(Category.power, ItemStack.with(
                Items.copper, 40,
                Items.lead, 25
            ));
        }};

        exampleWire = new MdtEnergyBlock("example-wire") {{
            localizedName = "示例导线";
            description = "自动连接四周同队的示例电力方块。每格最多通过 16 A，每个 1 A 包损失 0.05 V。";
            fallbackRegion = "power-node";
            role = EnergyRole.wire;

            capacityJ = 0f;
            voltageV = 0f;
            maxInputA = 0;
            maxOutputA = 0;
            maxWireCurrentA = 16;
            wireLossV = 0.05f;

            health = 90;
            size = 1;
            alwaysUnlocked = true;
            buildVisibility = BuildVisibility.shown;
            requirements(Category.power, ItemStack.with(
                Items.copper, 4,
                Items.lead, 2
            ));
        }};

        exampleConsumer = new MdtEnergyBlock("example-consumer") {{
            localizedName = "示例用电器";
            description = "相当于每秒自动减少 48 J 的 12 V 电池；每秒最多接收 6 A。";
            fallbackRegion = "arc";
            role = EnergyRole.consumer;

            voltageV = 12f;
            capacityJ = 600f;
            initialEnergyFraction = 1f;
            consumptionJPerSecond = 48f;
            maxInputA = 6;
            maxOutputA = 0;

            health = 220;
            size = 1;
            alwaysUnlocked = true;
            buildVisibility = BuildVisibility.shown;
            requirements(Category.power, ItemStack.with(
                Items.copper, 30,
                Items.lead, 20,
                Items.silicon, 10
            ));
        }};

        exampleBattery = new MdtEnergyBlock("example-battery") {{
            localizedName = "示例电池";
            description = "储存 12000 J；每秒最多输入 10 A、输出 10 A。只向用电器放电，避免电池互相来回传输。";
            fallbackRegion = "battery";
            role = EnergyRole.battery;

            voltageV = 12f;
            capacityJ = 12000f;
            initialEnergyFraction = 0.25f;
            maxInputA = 10;
            maxOutputA = 10;

            health = 320;
            size = 1;
            alwaysUnlocked = true;
            buildVisibility = BuildVisibility.shown;
            requirements(Category.power, ItemStack.with(
                Items.copper, 50,
                Items.lead, 60,
                Items.silicon, 20
            ));
        }};
    }
}
