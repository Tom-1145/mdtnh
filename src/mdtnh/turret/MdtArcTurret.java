package mdtnh.turret;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.struct.Seq;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Posc;
import mindustry.gen.Unit;

/**
 * “电击”专用多目标尖端放电炮台。
 *
 * <p>一次射击会攻击射程内所有符合目标条件、且当前储能能够负担的敌方单位。
 * 实际耗能 = 命中目标数 * energyPerTargetJ。</p>
 */
public class MdtArcTurret extends MdtElectricTurret {

    public float energyPerTargetJ = 8f;
    public float damagePerTarget = 24f;

    /** 0 表示不设硬上限，只受当前储能限制。 */
    public int maxTargets = 0;

    public Effect arcEffect = new Effect(10f, e -> {
        if (!(e.data instanceof Posc target)) return;

        Draw.color(e.color);
        Lines.stroke(2.2f * e.fout());
        Lines.line(e.x, e.y, target.x(), target.y());
        Draw.reset();
    });

    public MdtArcTurret(String name) {
        super(name);

        // 只作为 Turret 流程里的“可射击类型占位符”，不会真正生成子弹。
        shootType = new BulletType(0f, 0f) {{
            instantDisappear = true;
            collides = false;
            hittable = false;
            absorbable = false;
            shootEffect = Fx.none;
            smokeEffect = Fx.none;
            despawnEffect = Fx.none;
        }};

        buildType = MdtArcTurretBuild::new;
    }

    public class MdtArcTurretBuild extends MdtElectricTurretBuild {

        protected Seq<Unit> collectArcTargets() {
            Seq<Unit> result = new Seq<>();

            Units.nearbyEnemies(team, x, y, range(), unit -> {
                if (unit.dead()) return;
                if (!unitFilter.get(unit)) return;
                if (unit.isGrounded() && !targetGround) return;
                if (!unit.isGrounded() && !targetAir) return;

                result.add(unit);
            });

            result.sort((a, b) ->
                Float.compare(a.dst2(x, y), b.dst2(x, y))
            );

            return result;
        }

        @Override
        public boolean hasAmmo() {
            return shootType != null
                && canConsume()
                && (cheating() || nodeState.has(Math.max(0f, energyPerTargetJ)));
        }

        @Override
        protected void shoot(BulletType ignored) {
            Seq<Unit> targets = collectArcTargets();
            if (targets.isEmpty()) return;

            int limit = targets.size;

            if (maxTargets > 0) {
                limit = Math.min(limit, maxTargets);
            }

            if (!cheating() && energyPerTargetJ > 0f) {
                int affordable = (int)Math.floor(
                    (nodeState.energyJ + 0.0001f) / energyPerTargetJ
                );
                limit = Math.min(limit, affordable);
            }

            if (limit <= 0) return;

            float cost = energyPerTargetJ * limit;
            if (!cheating() && !nodeState.consume(cost)) return;

            for (int i = 0; i < limit; i++) {
                Unit unit = targets.get(i);
                unit.damage(damagePerTarget);
                arcEffect.at(x, y, 0f, team.color, unit);
            }

            shootSound.at(
                x,
                y,
                Mathf.random(soundPitchMin, soundPitchMax),
                shootSoundVolume
            );

            curRecoil = 1f;
            heat = 1f;
            totalShots++;
        }

        @Override
        public BulletType useAmmo() {
            // 实际耗能由 shoot() 按命中目标数量统一扣除。
            return shootType;
        }
    }
}
