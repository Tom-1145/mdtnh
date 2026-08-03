package mdtnh.energy;

import mindustry.gen.Building;

public interface MdtEnergyNode {
    /** 实际的 Mindustry 建筑实体。 */
    Building energyBuilding();

    /** 方块级配置。 */
    EnergySpec energySpec();

    /** 建筑实例级状态。 */
    EnergyState energyState();

    default boolean isEnergyWire() {
        return energySpec().isWire();
    }
}
