package mdtnh.turret;

import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Building;
import mindustry.logic.LAccess;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.consumers.ConsumeLiquidFilter;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatValues;

/**
 * 流体弹药 + MDT 能量的炮台层。
 *
 * <p>每发流体消耗量遵循原版 LiquidTurret：
 * {@code liquidPerUse = 1 / bullet.ammoMultiplier}。</p>
 *
 * <p>与原版不同的是，本类不依赖 {@code liquids.current()} 判断弹药，
 * 而是显式选择 ammoTypes 中有库存的流体，因此同一建筑可以同时保存
 * “蒸汽燃料 + 另一种流体弹药”，不会因 current liquid 被蒸汽占用而失效。</p>
 */
public class MdtEnergyLiquidTurret extends MdtEnergyTurret {

    public ObjectMap<Liquid, BulletType> ammoTypes = new OrderedMap<>();

    public MdtEnergyLiquidTurret(String name) {
        super(name);

        hasLiquids = true;
        buildType = MdtEnergyLiquidTurretBuild::new;
    }

    /** 格式：[liquid1, bullet1, liquid2, bullet2, ...] */
    public void ammo(Object... objects) {
        ammoTypes = OrderedMap.of(objects);
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.ammo, StatValues.ammo(ammoTypes, name));
    }

    @Override
    public void setBars() {
        super.setBars();

        addBar("mdt-liquid-ammo", build -> {
            MdtEnergyLiquidTurretBuild turret = (MdtEnergyLiquidTurretBuild) build;

            return new Bar(
                () -> {
                    Liquid liquid = turret.displayAmmoLiquid();
                    if (liquid == null) return "Ammo: empty";

                    return "Ammo: " + liquid.localizedName
                        + " " + Math.round(turret.liquids.get(liquid) * 10f) / 10f
                        + " / " + Math.round(liquidCapacity * 10f) / 10f;
                },
                () -> {
                    Liquid liquid = turret.displayAmmoLiquid();
                    return liquid == null
                        ? mindustry.graphics.Pal.ammo
                        : liquid.color;
                },
                () -> {
                    Liquid liquid = turret.displayAmmoLiquid();
                    return liquid == null || liquidCapacity <= 0f
                        ? 0f
                        : Math.min(1f, turret.liquids.get(liquid) / liquidCapacity);
                }
            );
        });
    }

    @Override
    public void init() {
        /*
         * 这里只把 ConsumeLiquidFilter 当作“输入有效性/UI 条件”，
         * update() 必须为空，真正扣弹药只能发生在 useAmmo()。
         */
        consume(new ConsumeLiquidFilter(ammoTypes::containsKey, 1f) {
            @Override
            public void update(Building build) {
                // 禁止连续自动扣液体。
            }

            @Override
            public float efficiency(Building build) {
                return build instanceof MdtEnergyLiquidTurretBuild turret
                    && turret.hasLiquidAmmoOnly()
                    ? 1f
                    : 0f;
            }

            @Override
            public void display(mindustry.world.meta.Stats stats) {
                // 弹药统计由本炮台自己的 Stat.ammo 显示。
            }
        });

        if (targetGround) {
            ammoTypes.each((liquid, type) ->
                placeOverlapRange = Math.max(
                    placeOverlapRange,
                    range + type.rangeChange + placeOverlapMargin
                )
            );
        }

        super.init();
    }

    public class MdtEnergyLiquidTurretBuild extends MdtEnergyTurretBuild {

        /** 最近输入/最近选中的有效弹药流体。 */
        protected Liquid preferredAmmoLiquid;

        protected float liquidRequiredFor(BulletType type) {
            if (type == null || type.ammoMultiplier <= 0f) {
                return Float.POSITIVE_INFINITY;
            }

            return 1f / type.ammoMultiplier;
        }

        protected boolean enoughLiquid(Liquid liquid, BulletType type) {
            if (liquid == null || type == null) return false;
            if (cheating()) return true;

            return liquids != null
                && liquids.get(liquid) + 0.000001f >= liquidRequiredFor(type);
        }

        /**
         * 选择当前可用弹药。
         * 优先最近输入的弹种；不足时按 ammo() 注册顺序寻找下一个。
         */
        protected Liquid selectAmmoLiquid(boolean requireEnough) {
            if (preferredAmmoLiquid != null) {
                BulletType preferredType = ammoTypes.get(preferredAmmoLiquid);

                if (preferredType != null
                    && (!requireEnough || enoughLiquid(preferredAmmoLiquid, preferredType))) {
                    return preferredAmmoLiquid;
                }
            }

            for (ObjectMap.Entry<Liquid, BulletType> entry : ammoTypes.entries()) {
                if (!requireEnough || enoughLiquid(entry.key, entry.value)) {
                    preferredAmmoLiquid = entry.key;
                    return entry.key;
                }
            }

            return null;
        }

        protected Liquid displayAmmoLiquid() {
            Liquid selected = selectAmmoLiquid(true);
            if (selected != null) return selected;

            selected = selectAmmoLiquid(false);
            if (selected != null && liquids != null && liquids.get(selected) > 0.000001f) {
                return selected;
            }

            return null;
        }

        protected boolean hasLiquidAmmoOnly() {
            return selectAmmoLiquid(true) != null;
        }

        @Override
        public boolean hasAmmo() {
            if (!canConsume()) return false;

            Liquid liquid = selectAmmoLiquid(true);
            if (liquid == null) return false;

            BulletType type = ammoTypes.get(liquid);
            return type != null
                && enoughLiquid(liquid, type)
                && hasEnergyFor(type);
        }

        @Override
        public BulletType peekAmmo() {
            Liquid liquid = selectAmmoLiquid(true);

            if (liquid == null && cheating()) {
                liquid = selectAmmoLiquid(false);
            }

            return liquid == null ? null : ammoTypes.get(liquid);
        }

        @Override
        public BulletType useAmmo() {
            Liquid liquid = selectAmmoLiquid(true);
            if (liquid == null) return null;

            BulletType type = ammoTypes.get(liquid);
            if (type == null) return null;

            if (cheating()) return type;

            float required = liquidRequiredFor(type);
            if (!enoughLiquid(liquid, type) || !hasEnergyFor(type)) {
                return null;
            }

            // 两种资源均足够后再扣除。
            if (!consumeEnergyFor(type)) return null;

            liquids.remove(liquid, required);
            return type;
        }

        @Override
        public UnlockableContent getAmmoContent() {
            return displayAmmoLiquid();
        }

        @Override
        public Object senseObject(LAccess sensor) {
            if (sensor == LAccess.currentAmmoType) {
                return displayAmmoLiquid();
            }

            return super.senseObject(sensor);
        }

        /**
         * 流体弹药层恢复 @ammo 的“物理弹药”语义：
         * 返回当前弹种的液体单位，而不是焦耳。
         */
        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.ammo) {
                Liquid liquid = displayAmmoLiquid();
                return liquid == null ? 0d : liquids.get(liquid);
            }

            if (sensor == LAccess.ammoCapacity) {
                return liquidCapacity;
            }

            return super.sense(sensor);
        }

        @Override
        public float getAmmoFraction() {
            Liquid liquid = displayAmmoLiquid();

            return liquid == null || liquidCapacity <= 0f
                ? 0f
                : Math.min(1f, liquids.get(liquid) / liquidCapacity);
        }

        @Override
        public boolean acceptItem(Building source, mindustry.type.Item item) {
            return false;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            return ammoTypes.containsKey(liquid)
                && liquids != null
                && liquids.get(liquid) < liquidCapacity - 0.0001f;
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount) {
            if (ammoTypes.containsKey(liquid)) {
                preferredAmmoLiquid = liquid;
            }

            super.handleLiquid(source, liquid, amount);
        }
    }
}
