package mdtnh.turret;

import arc.graphics.Color;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mdtnh.energy.EnergySpec;
import mdtnh.energy.EnergyState;
import mdtnh.energy.MdtEnergyNode;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Building;
import mindustry.logic.LAccess;
import mindustry.ui.Bar;
import mindustry.world.blocks.defense.turrets.Turret;

/**
 * MDT 通用耗能炮台基类。
 *
 * <p>不使用 Mindustry 原生 PowerGraph；炮台能量统一保存在
 * {@link EnergyState#energyJ} 中。固定弹丸、电力/蒸汽输入、物品/流体弹药
 * 都可以在此类之上继续派生。</p>
 */
public abstract class MdtEnergyTurret extends Turret {

    /** 固定弹丸模式使用的弹丸；物品/流体弹药派生类不使用此字段。 */
    public BulletType shootType;

    /** 每次 useAmmo() 调用需要消耗的能量，单位 J。 */
    public float energyPerShotJ = 12f;

    /** 炮台内部能量缓存容量，单位 J。 */
    public float energyCapacityJ = 240f;

    /** 新建建筑时初始储能比例。 */
    public float initialEnergyFraction = 0f;

    /** MDT 额定电压。 */
    public float voltageV = 12f;

    /** 最低允许输入电压。 */
    public float minInputVoltageV = 10f;

    /** 最高允许输入电压。 */
    public float maxInputVoltageV = 14f;

    /** 每个模拟秒最多接收的 1A 包数量。 */
    public int maxInputA = 4;

    protected EnergySpec energySpec;

    public MdtEnergyTurret(String name) {
        super(name);

        // 明确关闭 Mindustry 原生电网。
        hasPower = false;
        outputsPower = false;
        consumesPower = false;
        conductivePower = false;
        connectedPower = false;

        sync = true;
    }

    @Override
    public void load() {
        super.load();

        EnergySpec spec = new EnergySpec();
        spec.role = EnergySpec.Role.consumer;
        spec.voltageV = voltageV;
        spec.minInputVoltageV = minInputVoltageV;
        spec.maxInputVoltageV = maxInputVoltageV;
        spec.capacityJ = Math.max(0f, energyCapacityJ);
        spec.maxInputA = Math.max(0, maxInputA);
        spec.maxOutputA = 0;
        spec.maxWireCurrentA = 0;
        spec.wireLossV = 0f;

        energySpec = spec;
    }

    public EnergySpec energySpec() {
        return energySpec;
    }

    @Override
    public void setBars() {
        super.setBars();

        addBar("mdt-energy", build -> {
            MdtEnergyTurretBuild turret = (MdtEnergyTurretBuild) build;
            EnergySpec spec = turret.energySpec();
            float capacity = spec == null ? 0f : spec.capacityJ;

            return new Bar(
                () -> "Energy: " + Math.round(turret.nodeState.energyJ)
                    + " / " + Math.round(capacity) + " J",
                () -> Color.valueOf("ffd37f"),
                () -> capacity <= 0f ? 0f : turret.nodeState.fraction(spec)
            );
        });
    }

    public abstract class MdtEnergyTurretBuild extends TurretBuild implements MdtEnergyNode {

        public final EnergyState nodeState = new EnergyState();

        @Override
        public Building energyBuilding() {
            return this;
        }

        @Override
        public EnergySpec energySpec() {
            return MdtEnergyTurret.this.energySpec();
        }

        @Override
        public EnergyState energyState() {
            return nodeState;
        }

        /**
         * 基类默认不加入电网。
         * 电力派生类覆盖为 true；蒸汽派生类保持 false。
         */
        @Override
        public boolean canConnectToElectricGrid() {
            return false;
        }

        /**
         * 下一次 useAmmo() 的能耗。
         * 特殊炮台可以根据 BulletType、目标数量等动态覆写。
         */
        protected float nextEnergyCost(BulletType type) {
            return Math.max(0f, energyPerShotJ);
        }

        protected boolean hasEnergyFor(BulletType type) {
            if (type == null) return false;
            if (cheating()) return true;
            return nodeState.has(nextEnergyCost(type));
        }

        protected boolean consumeEnergyFor(BulletType type) {
            if (type == null) return false;
            if (cheating()) return true;
            return nodeState.consume(nextEnergyCost(type));
        }

        protected float energyFraction() {
            EnergySpec spec = energySpec();
            return spec == null ? 0f : nodeState.fraction(spec);
        }

        // ---------------- 固定弹丸模式 ----------------

        @Override
        public boolean hasAmmo() {
            return shootType != null && canConsume() && hasEnergyFor(shootType);
        }

        @Override
        public BulletType peekAmmo() {
            return shootType;
        }

        @Override
        public BulletType useAmmo() {
            BulletType type = shootType;
            if (type == null) return null;
            return consumeEnergyFor(type) ? type : null;
        }

        /**
         * 固定弹丸炮台没有物理弹药，因此控制 HUD 显示储能比例。
         * 物品/流体弹药层会覆写为各自的物理弹药比例。
         */
        @Override
        public float getAmmoFraction() {
            return energyFraction();
        }

        /**
         * 固定弹丸模式下，@ammo / @ammoCapacity 显示焦耳。
         * 物品/流体层会覆写回物理弹药语义。
         */
        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.ammo) return nodeState.energyJ;
            if (sensor == LAccess.ammoCapacity) {
                EnergySpec spec = energySpec();
                return spec == null ? 0d : spec.capacityJ;
            }
            return super.sense(sensor);
        }

        @Override
        public void created() {
            super.created();

            EnergySpec spec = energySpec();
            if (spec != null) {
                float fraction = Math.max(0f, Math.min(1f, initialEnergyFraction));
                nodeState.energyJ = spec.capacityJ * fraction;
            }
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            nodeState.write(write);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);

            if (revision >= 2) {
                nodeState.read(read, energySpec());
            } else {
                EnergySpec spec = energySpec();
                float fraction = Math.max(0f, Math.min(1f, initialEnergyFraction));
                nodeState.energyJ = spec == null ? 0f : spec.capacityJ * fraction;
            }
        }

        @Override
        public byte version() {
            return 2;
        }
    }
}
