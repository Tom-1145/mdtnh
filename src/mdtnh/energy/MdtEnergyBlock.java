package mdtnh.energy;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.ui.Bar;
import mindustry.world.Block;

/**
 * MDT 能源系统的通用示例方块。
 *
 * <p>该类型可以通过 {@link EnergyRole} 配置为发电机、导线、用电器或电池。
 * 不同角色共用同一个建筑实现和储能状态，具体的自动发电、自动耗电和网络传输
 * 由 {@link MdtEnergySystem} 每个模拟秒统一结算。</p>
 *
 * <p>本方块明确关闭 Mindustry 原生电力图相关属性，避免原生电网和 MDT 离散
 * 能源网络同时处理同一建筑。</p>
 */
public class MdtEnergyBlock extends Block {

    /**
     * 供内容注册代码使用的能源角色。
     *
     * <p>加载内容时会转换为能源系统统一使用的 {@link EnergySpec.Role}。</p>
     */
    public enum EnergyRole {
        generator,
        wire,
        consumer,
        battery
    }

    /** 方块在能源网络中的角色。 */
    public EnergyRole role = EnergyRole.battery;

    /** 输出电压，同时决定每个 1A 输出包从来源扣除的焦耳数。 */
    public float voltageV = 12f;

    /** 能够正常接收的最低输入电压；低于该值的包会被丢弃。 */
    public float minInputVoltageV = 10f;

    /** 能够正常接收的最高输入电压；高于该值的包会摧毁建筑。 */
    public float maxInputVoltageV = 14f;

    /** 内部能源缓存容量，单位为焦耳。 */
    public float capacityJ = 1000f;

    /** 新建筑生成时的初始荷电比例，建议设置在 0 到 1 之间。 */
    public float initialEnergyFraction = 0f;

    /** 每个模拟秒允许接收的最大电流包数量。 */
    public int maxInputA = 1;

    /** 每个模拟秒允许发送的最大电流包数量。 */
    public int maxOutputA = 1;

    /** 发电机每个模拟秒自动加入内部缓存的能量。 */
    public float generationJPerSecond = 0f;

    /** 用电器每个模拟秒自动从内部缓存扣除的能量。 */
    public float consumptionJPerSecond = 0f;

    /** 导线每个模拟秒允许通过的最大电流包数量。 */
    public int maxWireCurrentA = 1;

    /** 一个 1A 包经过该导线格时产生的电压损失。 */
    public float wireLossV = 0f;

    /** 未提供模组贴图时使用的原版图集区域名称。 */
    public String fallbackRegion = "battery";

    /**
     * 能源系统读取的统一规格对象。
     *
     * <p>内容字段可以在方块注册的双括号初始化器中配置；{@link #load()} 会在
     * Mindustry 完成内容定义后把这些字段整理为不可按实例变化的规格。</p>
     */
    private EnergySpec spec;

    public MdtEnergyBlock(String name) {
        super(name);

        update = true;
        solid = true;
        destructible = true;
        canOverdrive = false;

        // MDT 能源系统独立于 Mindustry 原生 PowerGraph。
        hasPower = false;
        outputsPower = false;
        consumesPower = false;
        conductivePower = false;
        connectedPower = false;

        // 能量状态参与存档与网络同步。
        sync = true;

        buildType = MdtEnergyBuild::new;
    }

    /**
     * 加载贴图并建立供运行时读取的能源规格。
     *
     * <p>规格在内容加载阶段创建，之后所有该方块的建筑实例共享同一个对象。</p>
     */
    @Override
    public void load() {
        super.load();
        region = Core.atlas.find(fallbackRegion);

        spec = new EnergySpec();
        spec.role = convertRole(role);
        spec.voltageV = voltageV;
        spec.minInputVoltageV = minInputVoltageV;
        spec.maxInputVoltageV = maxInputVoltageV;
        spec.capacityJ = capacityJ;
        spec.maxInputA = maxInputA;
        spec.maxOutputA = maxOutputA;
        spec.maxWireCurrentA = maxWireCurrentA;
        spec.wireLossV = wireLossV;
    }

    /**
     * 判断方块是否为导线。
     *
     * <p>在 {@link #load()} 之前规格对象尚未创建，因此需要回退到内容字段判断。</p>
     */
    public boolean isWire() {
        return spec != null ? spec.isWire() : (role == EnergyRole.wire);
    }

    /** @return 供能源网络使用的方块级规格。 */
    public EnergySpec energySpec() {
        return spec;
    }

    /** 将内容注册层使用的角色转换为能源系统角色。 */
    private static EnergySpec.Role convertRole(EnergyRole r) {
        switch (r) {
            case generator: return EnergySpec.Role.generator;
            case wire:      return EnergySpec.Role.wire;
            case consumer:  return EnergySpec.Role.consumer;
            default:        return EnergySpec.Role.battery;
        }
    }

    /**
     * 为导线显示通过电流，为其他节点显示储能量和输入输出电流。
     */
    @Override
    public void setBars() {
        super.setBars();

        if (isWire()) {
            addBar("mdt-current", build -> {
                MdtEnergyBuild energy = (MdtEnergyBuild) build;
                return new Bar(
                        () -> "Current: " + energy.nodeState.currentA + " / " + maxWireCurrentA + " A",
                        () -> Color.valueOf("ffd37f"),
                        () -> maxWireCurrentA <= 0 ? 0f : Math.min(1f, energy.nodeState.currentA / (float) maxWireCurrentA)
                );
            });
        } else {
            addBar("mdt-energy", build -> {
                MdtEnergyBuild energy = (MdtEnergyBuild) build;
                return new Bar(
                        () -> "Energy: " + Math.round(energy.nodeState.energyJ) + " / " + Math.round(capacityJ) + " J",
                        () -> Color.valueOf("ffd37f"),
                        () -> capacityJ <= 0f ? 0f : Math.min(1f, energy.nodeState.energyJ / capacityJ)
                );
            });

            addBar("mdt-io", build -> {
                MdtEnergyBuild energy = (MdtEnergyBuild) build;
                int maximum = Math.max(1, Math.max(maxInputA, maxOutputA));
                return new Bar(
                        () -> "I/O: " + energy.nodeState.inputA + " A in, "
                                + energy.nodeState.outputA + " A out | "
                                + Math.round(energy.nodeState.lastInputVoltageV * 10f) / 10f + " V"
                                + " [" + minInputVoltageV + "~" + maxInputVoltageV + " V]",
                        () -> Color.valueOf("84f491"),
                        () -> Math.min(1f, Math.max(energy.nodeState.inputA, energy.nodeState.outputA) / (float) maximum)
                );
            });
        }
    }

    /**
     * 已放置的通用能源建筑。
     *
     * <p>实现 {@link MdtEnergyNode} 后，能源系统可以通过接口统一访问建筑位置、
     * 方块规格和实例状态，无需依赖具体建筑继承层次。</p>
     */
    public class MdtEnergyBuild extends Building implements MdtEnergyNode {

        /** 该建筑实例自己的能量与上一模拟秒电流统计。 */
        public final EnergyState nodeState = new EnergyState();

        @Override
        public Building energyBuilding() {
            return this;
        }

        @Override
        public EnergySpec energySpec() {
            return MdtEnergyBlock.this.energySpec();
        }

        @Override
        public EnergyState energyState() {
            return nodeState;
        }

        /**
         * 返回该建筑所属的外层方块定义。
         *
         * <p>自动发电和自动耗电参数目前保存在方块类中，能源系统通过该方法读取。</p>
         */
        public MdtEnergyBlock energyBlock() {
            return MdtEnergyBlock.this;
        }

        /** @return 当前建筑是否为导线实例。 */
        public boolean isWire() {
            return energySpec().isWire();
        }

        /** @return 当前能量占容量的比例；无容量节点返回 0。 */
        public float soc() {
            float cap = energySpec().capacityJ;
            return cap <= 0f ? 0f : nodeState.energyJ / cap;
        }

        /**
         * 根据内容配置初始化新建筑的能量。
         *
         * <p>导线始终从 0 开始；储能节点按照初始荷电比例计算。</p>
         */
        @Override
        public void created() {
            super.created();
            nodeState.energyJ = isWire() ? 0f : energySpec().capacityJ * initialEnergyFraction;
        }

        /**
         * 绘制导线与相邻能源节点之间的连接线。
         *
         * <p>线宽随上一模拟秒通过的电流增大，仅用于视觉反馈，不参与网络计算。</p>
         */
        @Override
        public void draw() {
            if (isWire()) {
                float fraction = energySpec().maxWireCurrentA <= 0
                        ? 0f
                        : Math.min(1f, nodeState.currentA / (float) energySpec().maxWireCurrentA);

                Draw.color(Color.valueOf("ffd37f"));
                Lines.stroke(1.2f + 1.8f * fraction);

                for (int direction = 0; direction < 4; direction++) {
                    Building nearby = tile.nearbyBuild(direction);
                    if (nearby instanceof MdtEnergyNode
                            && MdtEnergySystem.canConnect(this, (MdtEnergyNode) nearby)) {
                        Lines.line(x, y, nearby.x, nearby.y);
                    }
                }

                Draw.reset();
            }

            super.draw();
        }

        /** 将当前储能量追加到建筑存档数据。 */
        @Override
        public void write(Writes write) {
            super.write(write);
            nodeState.write(write);
        }

        /** 从建筑存档恢复储能量。 */
        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            nodeState.read(read, energySpec());
        }

        /** 存档格式版本 1 保存能源状态中的当前能量。 */
        @Override
        public byte version() {
            return 1;
        }
    }
}
