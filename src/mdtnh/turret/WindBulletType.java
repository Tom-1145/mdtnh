package mdtnh.turret;

import arc.util.Tmp;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Hitboxc;
import mindustry.gen.Unit;

/**
 * 风压弹：基础击退对所有单位有效，对空中单位施加额外冲量。
 */
public class WindBulletType extends BasicBulletType {

    /** 空军总击退倍率；1 表示与地面单位相同。 */
    public float airKnockbackMultiplier = 2.5f;

    public WindBulletType(float speed, float damage) {
        super(speed, damage);
    }

    public WindBulletType() {
        this(1f, 0f);
    }

    @Override
    public void hitEntity(Bullet b, Hitboxc entity, float health) {
        super.hitEntity(b, entity, health);

        if (entity instanceof Unit unit && !unit.isGrounded() && airKnockbackMultiplier > 1f) {
            Tmp.v1.set(unit.x - b.x, unit.y - b.y);

            if (!Tmp.v1.isZero()) {
                Tmp.v1.nor().scl(knockback * 80f * (airKnockbackMultiplier - 1f));
                unit.impulse(Tmp.v1);
            }
        }
    }
}
