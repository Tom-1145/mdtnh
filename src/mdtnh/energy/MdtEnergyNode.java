package mdtnh.energy;

import mindustry.gen.Building;

/**
 * 能够参与 MDT 离散能源网络的建筑接口。
 *
 * <p>能源系统只依赖该接口，不要求建筑继承某个特定方块类。因此普通单方块工厂、
 * 多方块能源仓和专用能源方块都可以通过组合 {@link EnergySpec} 与
 * {@link EnergyState} 接入同一网络，避免 Java 单继承带来的限制。</p>
 */
public interface MdtEnergyNode {

    /**
     * 返回承载该能源节点的 Mindustry 建筑实例。
     *
     * <p>网络使用建筑的位置、队伍和邻接信息进行分组与寻路。</p>
     */
    Building energyBuilding();

    /** @return 该节点的方块级能源参数。 */
    EnergySpec energySpec();

    /** @return 该建筑实例独立保存的能源状态。 */
    EnergyState energyState();

    /** @return 当前节点是否只承担导线传输职责。 */
    default boolean isEnergyWire() {
        return energySpec().isWire();
    }

    /**
     * 处理超过最高输入电压的电流包。
     *
     * <p>默认行为是清空能源缓存并摧毁建筑。特殊设备可以重写该方法，
     * 实现耐压保险、分阶段损坏或自定义爆炸效果，而无需改变全局寻路器。</p>
     *
     * @param inputVoltageV 实际到达建筑的电压
     */
    default void onOvervoltage(float inputVoltageV) {
        energyState().energyJ = 0f;
        energyBuilding().kill();
    }
}
