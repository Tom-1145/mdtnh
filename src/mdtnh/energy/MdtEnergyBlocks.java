package mdtnh.energy;

import mdtnh.energy.MdtEnergyBlock.EnergyRole;
import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.meta.BuildVisibility;

/**
 * 注册用于测试 MDT 能源网络的四种基础方块。
 *
 * <p>这些方块主要用于验证离散电流包、导线线损、输入输出上限和储能调度。
 * 正式内容可以继续使用 {@link MdtEnergyBlock} 并覆盖相同的公开配置字段。</p>
 */
public final class MdtEnergyBlocks {

    /** 持续向内部缓存发电，并可向网络输出的示例节点。 */
    public static MdtEnergyBlock exampleGenerator;

    /** 自动连接相邻能源节点并承担路径传输的示例导线。 */
    public static MdtEnergyBlock exampleWire;

    /** 持续消耗内部能量、仅允许网络输入的示例负载。 */
    public static MdtEnergyBlock exampleConsumer;

    /** 可充可放的示例储能节点。 */
    public static MdtEnergyBlock exampleBattery;

    private MdtEnergyBlocks() {
    }

    /** 创建并注册全部示例能源方块。 */
    public static void load() {

        // 发电机输出 18V；每秒产生 96J，因此持续输出能力受发电量和缓存共同限制。
        exampleGenerator = new MdtEnergyBlock("example-generator") {{
            localizedName = "示例发电机";
            description = "每秒自动产生 96 J；以 12 V、最多 8 A 向相邻导线网络输出。";
            fallbackRegion = "combustion-generator";
            role = EnergyRole.generator;

            voltageV = 12f;
            minInputVoltageV = 0f;
            maxInputVoltageV = 0f;
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

        // 导线不储能；容量字段设为 0，只使用载流上限与单格压降。
        exampleWire = new MdtEnergyBlock("example-wire") {{
            localizedName = "示例导线";
            description = "自动连接四周同队的示例电力方块。每格最多通过 16 A，每个 1 A 包损失 0.05 V。";
            fallbackRegion = "power-node";
            role = EnergyRole.wire;

            capacityJ = 0f;
            voltageV = 0f;
            minInputVoltageV = 0f;
            maxInputVoltageV = 0f;
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

        // 负载以满缓存开始，每秒先自动扣除 48J，再由网络尝试补充。
        exampleConsumer = new MdtEnergyBlock("example-consumer") {{
            localizedName = "示例用电器";
            description = "每秒自动减少 48 J；正常输入范围为 10~14 V，每秒最多接收 6 A。";
            fallbackRegion = "arc";
            role = EnergyRole.consumer;

            voltageV = 12f;
            minInputVoltageV = 10f;
            maxInputVoltageV = 14f;
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

        // 电池可接收发电机余量，并在消费者缺能时作为后备来源。
        exampleBattery = new MdtEnergyBlock("example-battery") {{
            localizedName = "示例电池";
            description = "储存 12000 J；每秒最多输入 10 A、输出 10 A。只向用电器放电，避免电池互相来回传输。";
            fallbackRegion = "battery";
            role = EnergyRole.battery;

            voltageV = 12f;
            minInputVoltageV = 10f;
            maxInputVoltageV = 14f;
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
