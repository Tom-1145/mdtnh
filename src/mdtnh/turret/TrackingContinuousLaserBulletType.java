package mdtnh.turret;

import arc.math.Angles;
import arc.math.Mathf;
import mindustry.entities.bullet.ContinuousLaserBulletType;
import mindustry.gen.Bullet;
import mindustry.world.blocks.defense.turrets.Turret;

/**
 * 会在存续期间追随所属炮台当前瞄准点的持续激光。
 *
 * <p>伤害会按炮口到当前瞄准点的距离线性衰减；
 * 默认 pierceCap=1，因此只伤害射线上最先命中的一个敌对目标。</p>
 */
public class TrackingContinuousLaserBulletType extends ContinuousLaserBulletType {

    /** 距离小于该值时保持完整伤害。 */
    public float falloffStart = 120f;

    /** 距离达到该值时衰减到 minDamageMultiplier。 */
    public float falloffEnd = 520f;

    /** 最远处最低伤害倍率。 */
    public float minDamageMultiplier = 0.30f;

    /** 每 tick 最大转向角；<=0 表示瞬时跟踪。 */
    public float trackingSpeed = 8f;

    public TrackingContinuousLaserBulletType(float damage) {
        super(damage);
        pierceCap = 1;
    }

    public TrackingContinuousLaserBulletType() {
        this(1f);
    }

    @Override
    public void update(Bullet b) {
        float factor = 1f;

        if (b.owner instanceof Turret.TurretBuild turret) {
            float tx = turret.targetPos.x;
            float ty = turret.targetPos.y;

            if (!(tx == 0f && ty == 0f)) {
                float desired = Angles.angle(b.x, b.y, tx, ty);

                if (trackingSpeed <= 0f) {
                    b.rotation(desired);
                } else {
                    b.rotation(Angles.moveToward(b.rotation(), desired, trackingSpeed));
                }

                float dst = Mathf.dst(b.x, b.y, tx, ty);
                float denom = Math.max(0.0001f, falloffEnd - falloffStart);
                float t = Mathf.clamp((dst - falloffStart) / denom);
                factor = Mathf.lerp(1f, minDamageMultiplier, t);
            }
        }

        float originalDamage = b.damage;
        b.damage = originalDamage * factor;
        super.update(b);
        b.damage = originalDamage;
    }
}
