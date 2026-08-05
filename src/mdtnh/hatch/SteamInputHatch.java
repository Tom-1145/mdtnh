package mdtnh.hatch;

import arc.graphics.Color;
import mdtnh.ModLiquids;
import mdtnh.energy.SteamEnergyConverter;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.ui.Bar;

/**
 * 通过接收蒸汽为内部能源缓存充能的多方块能源仓。
 *
 * <p>该仓室继承 {@link EnergyInputHatch}，因此多方块核心仍可从它的 EnergyState
 * 扣除配方能耗；但 {@code electricGridEnabled} 被关闭，导线网络不会收集或连接它。</p>
 */
public class SteamInputHatch extends EnergyInputHatch {

    /** 能够被仓室接受并转换的蒸汽液体。 */
    public Liquid steamLiquid;

    /** 每单位蒸汽转换得到的内部能量，单位为焦耳。 */
    public float joulesPerSteamUnit = 120f;

    /** 每个模拟秒允许转换的最大蒸汽量。 */
    public float maxSteamUsePerSecond = 2f;

    public SteamInputHatch(String name) {
        super(name);
        steamLiquid = ModLiquids.steam;
        electricGridEnabled = false;
        hasLiquids = true;
        liquidCapacity = 40f;

        energySpec.maxInputA = 0;
        energySpec.maxOutputA = 0;
        buildType = SteamInputHatchBuild::new;
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("mdt-steam", raw -> {
            SteamInputHatchBuild build = (SteamInputHatchBuild) raw;
            return new Bar(
                    () -> "Steam: " + Math.round(build.liquids.get(steamLiquid) * 10f) / 10f
                            + " / " + Math.round(liquidCapacity * 10f) / 10f,
                    () -> Color.lightGray,
                    () -> steamLiquid == null || liquidCapacity <= 0f
                            ? 0f
                            : Math.min(1f, build.liquids.get(steamLiquid) / liquidCapacity)
            );
        });
    }

    public class SteamInputHatchBuild extends EnergyInputHatchBuild {

        @Override
        public void updateTile() {
            super.updateTile();
            convertSteamToEnergy();
        }

        protected void convertSteamToEnergy() {
            SteamEnergyConverter.convert(
                    this,
                    energyState,
                    energySpec,
                    steamLiquid,
                    joulesPerSteamUnit,
                    maxSteamUsePerSecond,
                    delta()
            );
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            return liquid == steamLiquid && liquids.get(liquid) < block.liquidCapacity;
        }
    }
}
