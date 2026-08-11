package mdtnh.turret;

import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Bullet;

/**
 * 每次成功穿透后按比例降低剩余直击伤害。
 *
 * <p>damageRetention=0.5 表示第一目标承受 100%，第二目标 50%，
 * 第三目标 25%……。pierceCap=-1 时可无限穿透，直到剩余伤害低于 minDamage。</p>
 */
public class HalvingPierceBulletType extends BasicBulletType {

    public float damageRetention = 0.5f;
    public float minDamage = 1f;

    public HalvingPierceBulletType(float speed, float damage) {
        super(speed, damage);
        pierce = true;
        pierceBuilding = true;
        pierceCap = -1;
        removeAfterPierce = false;
    }

    public HalvingPierceBulletType() {
        this(1f, 1f);
    }

    @Override
    public void handlePierce(Bullet b, float initialHealth, float x, float y) {
        b.damage *= damageRetention;

        if (b.damage < minDamage) {
            b.hit = true;
            b.remove();
        }
    }
}
