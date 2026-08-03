package mdtnh.hatch;

import arc.graphics.Color;
import mdtnh.energy.EnergySpec;
import mdtnh.energy.EnergyState;
import mdtnh.energy.MdtEnergyNode;
import mindustry.gen.Building;
import mindustry.ui.Bar;

/**
 * 多方块结构使用的能源输入仓。
 *
 * <p>能源仓本身是一个 MDT 能源网络消费者：外部导线把能量送入它的内部缓存，
 * 多方块核心再根据结构定义中的能源仓坐标从该缓存扣除配方能耗。</p>
 */
public class EnergyInputHatch extends Hatch {

    /** 所有该类型能源仓共享的额定电压、容量和输入输出上限。 */
    public final EnergySpec energySpec = new EnergySpec();

    public EnergyInputHatch(String name) {
        super(name);

        // 能源仓只保存能量，不启用 Hatch 基类提供的物品模块。
        hasItems = false;
        itemCapacity = 0;

        // 作为纯输入端接入网络：允许充电，不允许主动向外部网络放电。
        energySpec.role = EnergySpec.Role.consumer;
        energySpec.voltageV = 12f;
        energySpec.capacityJ = 2400f;
        energySpec.maxInputA = 16;
        energySpec.maxOutputA = 0;

        buildType = EnergyInputHatchBuild::new;
    }

    /**
     * 在建筑信息面板中显示能源仓的当前储能比例。
     */
    @Override
    public void setBars() {
        super.setBars();
        addBar("mdt-energy", raw -> {
            EnergyInputHatchBuild build = (EnergyInputHatchBuild) raw;
            return new Bar(
                    () -> "Energy: " + Math.round(build.energyState.energyJ) + " / " + Math.round(energySpec.capacityJ) + " J",
                    () -> Color.valueOf("ffd37f"),
                    () -> build.energyState.fraction(energySpec)
            );
        });
    }

    /**
     * 已放置的能源仓建筑。
     *
     * <p>通过实现 {@link MdtEnergyNode} 接入能源系统，同时继续继承
     * {@link HatchBuild} 的绘制和基本建筑行为。</p>
     */
    public class EnergyInputHatchBuild extends HatchBuild implements MdtEnergyNode {

        /** 该能源仓实例自己的储能量和电流统计。 */
        public final EnergyState energyState = new EnergyState();

        @Override
        public Building energyBuilding() {
            return this;
        }

        @Override
        public EnergySpec energySpec() {
            return EnergyInputHatch.this.energySpec;
        }

        @Override
        public EnergyState energyState() {
            return energyState;
        }

        /**
         * 存档格式版本 1 保存一个 {@code float} 类型的当前能量值。
         */
        @Override
        public byte version() {
            return 1;
        }

        @Override
        public void write(arc.util.io.Writes write) {
            super.write(write);
            energyState.write(write);
        }

        @Override
        public void read(arc.util.io.Reads read, byte revision) {
            super.read(read, revision);
            if (revision >= 1) {
                energyState.read(read, energySpec());
            }
        }
    }
}
