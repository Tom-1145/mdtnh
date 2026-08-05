package mdtnh.energy;

import mindustry.gen.Building;
import mindustry.type.Liquid;

/**
 * 把建筑液体模块中的蒸汽转换为 {@link EnergyState} 内部能量的通用组件。
 *
 * <p>该类不参与电网寻路，也不要求建筑继承特定父类。任何拥有液体模块、
 * EnergyState 和 EnergySpec 的建筑都可以调用它，从而以组合方式复用蒸汽能源逻辑。</p>
 */
public final class SteamEnergyConverter {

    private SteamEnergyConverter() {
    }

    /**
     * 执行一个 tick 的蒸汽转换。
     *
     * @param building 承载蒸汽液体模块的建筑
     * @param state 需要被充能的内部能源状态
     * @param spec 内部缓存容量规格
     * @param steam 被转换的蒸汽液体
     * @param joulesPerSteamUnit 每单位蒸汽提供的焦耳数
     * @param maxSteamUsePerSecond 每模拟秒最大蒸汽吞吐量
     * @param deltaTicks 本次更新经过的逻辑 tick 数
     * @return 本 tick 实际消耗的蒸汽量
     */
    public static float convert(
            Building building,
            EnergyState state,
            EnergySpec spec,
            Liquid steam,
            float joulesPerSteamUnit,
            float maxSteamUsePerSecond,
            float deltaTicks
    ) {
        if (building == null || state == null || spec == null || steam == null) return 0f;
        if (building.liquids == null) return 0f;
        if (joulesPerSteamUnit <= 0f || maxSteamUsePerSecond <= 0f || deltaTicks <= 0f) return 0f;

        float freeEnergyJ = Math.max(0f, spec.capacityJ - state.energyJ);
        if (freeEnergyJ <= 0.0001f) return 0f;

        float availableSteam = building.liquids.get(steam);
        float tickLimit = maxSteamUsePerSecond * deltaTicks / 60f;
        float steamForFreeSpace = freeEnergyJ / joulesPerSteamUnit;
        float usedSteam = Math.min(availableSteam, Math.min(tickLimit, steamForFreeSpace));

        if (usedSteam <= 0.000001f) return 0f;

        building.liquids.remove(steam, usedSteam);
        state.add(usedSteam * joulesPerSteamUnit, spec);
        return usedSteam;
    }
}
