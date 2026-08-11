package mdtnh.turret;

import arc.graphics.Color;
import arc.util.Time;
import mdtnh.ModLiquids;
import mdtnh.energy.SteamEnergyConverter;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.ui.Bar;

/**
 * 流体弹药 + 蒸汽能源。
 *
 * <p>支持蒸汽同时作为燃料和弹药。若 steamReserveForAmmo < 0，
 * 且蒸汽本身也是 ammoTypes 中的弹种，会自动保留“一发弹药”的蒸汽，
 * 避免 SteamEnergyConverter 把最后一发转换成焦耳。</p>
 */
public class MdtSteamLiquidTurret extends MdtEnergyLiquidTurret {

    public Liquid steam;
    public float joulesPerSteamUnit = 60f;
    public float maxSteamUsePerSecond = 1f;

    /**
     * 转换器必须保留的蒸汽量。
     * <ul>
     *     <li>< 0：自动；蒸汽是弹药时保留 1 发，否则保留 0。</li>
     *     <li>>= 0：固定保留指定数量。</li>
     * </ul>
     */
    public float steamReserveForAmmo = -1f;

    public MdtSteamLiquidTurret(String name) {
        super(name);

        hasLiquids = true;
        liquidCapacity = 20f;

        buildType = MdtSteamLiquidTurretBuild::new;
    }

    public Liquid steamLiquid() {
        return steam != null ? steam : ModLiquids.steam;
    }

    @Override
    public void setBars() {
        super.setBars();

        addBar("mdt-steam", build -> {
            MdtSteamLiquidTurretBuild turret = (MdtSteamLiquidTurretBuild) build;

            return new Bar(
                () -> {
                    Liquid type = steamLiquid();
                    return type == null
                        ? "Steam/Fuel: unavailable"
                        : "Steam/Fuel: "
                            + Math.round(turret.liquids.get(type) * 10f) / 10f
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

    public class MdtSteamLiquidTurretBuild extends MdtEnergyLiquidTurretBuild {

        @Override
        public boolean canConnectToElectricGrid() {
            return false;
        }

        protected float steamReserve() {
            if (steamReserveForAmmo >= 0f) {
                return steamReserveForAmmo;
            }

            Liquid type = steamLiquid();
            if (type == null) return 0f;

            mindustry.entities.bullet.BulletType bullet = ammoTypes.get(type);
            return bullet == null ? 0f : liquidRequiredFor(bullet);
        }

        @Override
        public void updateTile() {
            Liquid type = steamLiquid();

            if (type != null && Time.delta > 0f) {
                float available = liquids.get(type);
                float convertible = Math.max(0f, available - steamReserve());

                if (convertible > 0.000001f) {
                    /*
                     * SteamEnergyConverter 本身按 maxSteamUsePerSecond 限速。
                     * 这里再把本 tick 允许速率钳到 convertible，
                     * 从而保证转换后不会低于 steamReserve()。
                     */
                    float reserveSafeRate = convertible * 60f / Time.delta;

                    SteamEnergyConverter.convert(
                        this,
                        nodeState,
                        energySpec(),
                        type,
                        joulesPerSteamUnit,
                        Math.min(maxSteamUsePerSecond, reserveSafeRate),
                        Time.delta
                    );
                }
            }

            super.updateTile();
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            Liquid steamType = steamLiquid();

            if (steamType != null && liquid == steamType) {
                // 蒸汽可以只是燃料，也可以同时是 ammoTypes 中的弹药。
                if (ammoTypes.containsKey(liquid)) {
                    preferredAmmoLiquid = liquid;
                }

                return liquids != null
                    && liquids.get(liquid) < liquidCapacity - 0.0001f;
            }

            // 其它合法流体弹药交给 MdtEnergyLiquidTurret。
            return super.acceptLiquid(source, liquid);
        }
    }
}
