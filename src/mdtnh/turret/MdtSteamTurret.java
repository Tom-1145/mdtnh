package mdtnh.turret;

import arc.graphics.Color;
import arc.util.Time;
import mdtnh.ModLiquids;
import mdtnh.energy.SteamEnergyConverter;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.ui.Bar;

/** 使用蒸汽转焦耳供能的固定弹丸炮台。 */
public class MdtSteamTurret extends MdtEnergyTurret {

    public Liquid steam;
    public float joulesPerSteamUnit = 60f;
    public float maxSteamUsePerSecond = 1f;

    public MdtSteamTurret(String name) {
        super(name);

        hasLiquids = true;
        liquidCapacity = 20f;

        buildType = MdtSteamTurretBuild::new;
    }

    public Liquid steamLiquid() {
        return steam != null ? steam : ModLiquids.steam;
    }

    @Override
    public void setBars() {
        super.setBars();

        addBar("mdt-steam", build -> {
            MdtSteamTurretBuild turret = (MdtSteamTurretBuild) build;

            return new Bar(
                () -> {
                    Liquid type = steamLiquid();
                    return type == null
                        ? "Steam: unavailable"
                        : "Steam: " + Math.round(turret.liquids.get(type) * 10f) / 10f
                            + " / " + Math.round(liquidCapacity * 10f) / 10f;
                },
                () -> Color.lightGray,
                () -> {
                    Liquid type = steamLiquid();
                    return type == null || liquidCapacity <= 0f
                        ? 0f
                        : Math.min(1f, turret.liquids.get(type) / liquidCapacity);
                }
            );
        });
    }

    public class MdtSteamTurretBuild extends MdtEnergyTurretBuild {

        @Override
        public boolean canConnectToElectricGrid() {
            return false;
        }

        @Override
        public void updateTile() {
            Liquid type = steamLiquid();
            if (type != null) {
                SteamEnergyConverter.convert(
                    this,
                    nodeState,
                    energySpec(),
                    type,
                    joulesPerSteamUnit,
                    maxSteamUsePerSecond,
                    Time.delta
                );
            }

            super.updateTile();
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            Liquid type = steamLiquid();

            if (type != null && liquid == type) {
                return liquids != null
                    && liquids.get(liquid) < liquidCapacity - 0.0001f;
            }

            // 保留 Turret 自身冷却液等 ConsumeLiquid 的兼容性。
            return super.acceptLiquid(source, liquid);
        }
    }
}
