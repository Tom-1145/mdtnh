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

public class MdtEnergyBlock extends Block {

    // 保留旧枚举，内部转换为 EnergySpec.Role
    public enum EnergyRole {
        generator,
        wire,
        consumer,
        battery
    }

    // ===== 旧字段（保留，仅用于初始化，之后会同步到 spec） =====
    public EnergyRole role = EnergyRole.battery;
    public float voltageV = 12f;
    public float capacityJ = 1000f;
    public float initialEnergyFraction = 0f;
    public int maxInputA = 1;
    public int maxOutputA = 1;
    public float generationJPerSecond = 0f;
    public float consumptionJPerSecond = 0f;
    public int maxWireCurrentA = 1;
    public float wireLossV = 0f;
    public String fallbackRegion = "battery";

    // ===== 统一的能源规格（内部使用） =====
    private EnergySpec spec;

    public MdtEnergyBlock(String name) {
        super(name);

        update = true;
        solid = true;
        destructible = true;
        canOverdrive = false;
        hasPower = false;
        outputsPower = false;
        consumesPower = false;
        conductivePower = false;
        connectedPower = false;
        sync = true;

        buildType = MdtEnergyBuild::new;
    }

    /** 在 load 时将旧字段复制到统一的 spec，后续始终用 spec */
    @Override
    public void load() {
        super.load();
        region = Core.atlas.find(fallbackRegion);

        spec = new EnergySpec();
        spec.role = convertRole(role);
        spec.voltageV = voltageV;
        spec.capacityJ = capacityJ;
        spec.maxInputA = maxInputA;
        spec.maxOutputA = maxOutputA;
        spec.maxWireCurrentA = maxWireCurrentA;
        spec.wireLossV = wireLossV;
        // generator / consumer 实际上也是电池模型，这里不额外处理
    }

    public boolean isWire() {
        return spec != null ? spec.isWire() : (role == EnergyRole.wire);
    }

    public EnergySpec energySpec() {
        return spec;
    }

    private static EnergySpec.Role convertRole(EnergyRole r) {
        switch (r) {
            case generator: return EnergySpec.Role.generator;
            case wire:      return EnergySpec.Role.wire;
            case consumer:  return EnergySpec.Role.consumer;
            default:        return EnergySpec.Role.battery;
        }
    }

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
                        () -> "I/O: " + energy.nodeState.inputA + " A in, " + energy.nodeState.outputA + " A out",
                        () -> Color.valueOf("84f491"),
                        () -> Math.min(1f, Math.max(energy.nodeState.inputA, energy.nodeState.outputA) / (float) maximum)
                );
            });
        }
    }

    public class MdtEnergyBuild extends Building implements MdtEnergyNode {

        // 使用统一的能源状态对象
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

        /** 兼容旧代码的快捷方式 */
        public MdtEnergyBlock energyBlock() {
            return MdtEnergyBlock.this;
        }

        public boolean isWire() {
            return energySpec().isWire();
        }

        public float soc() {
            float cap = energySpec().capacityJ;
            return cap <= 0f ? 0f : nodeState.energyJ / cap;
        }

        @Override
        public void created() {
            super.created();
            nodeState.energyJ = isWire() ? 0f : energySpec().capacityJ * initialEnergyFraction;
        }

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

        @Override
        public void write(Writes write) {
            super.write(write);
            nodeState.write(write);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            nodeState.read(read, energySpec());
        }

        @Override
        public byte version() {
            return 1;
        }
    }
}