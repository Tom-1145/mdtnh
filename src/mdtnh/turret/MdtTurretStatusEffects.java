package mdtnh.turret;

import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.units.StatusEntry;
import mindustry.gen.Unit;
import mindustry.type.StatusEffect;

/**
 * “电极”炮台使用的正/负电荷状态。
 */
public final class MdtTurretStatusEffects {

    public static StatusEffect positiveCharge;
    public static StatusEffect negativeCharge;

    /** 同时触发正负电荷时的高额电击伤害。 */
    public static float chargeReactionDamage = 140f;

    private static boolean loaded;

    private MdtTurretStatusEffects() {}

    public static void load() {
        if (loaded) return;
        loaded = true;

        positiveCharge = new StatusEffect("positive-charge") {{
            localizedName = "正电荷";
            color = Color.valueOf("ffd45e");
            speedMultiplier = 0.96f;
            effectChance = 0.08f;
            effect = Fx.electrified;
        }};

        negativeCharge = new StatusEffect("negative-charge") {{
            localizedName = "负电荷";
            color = Color.valueOf("7aa8ff");
            speedMultiplier = 0.96f;
            effectChance = 0.08f;
            effect = Fx.electrified;
        }};

        positiveCharge.init(() ->
            positiveCharge.affinity(
                negativeCharge,
                MdtTurretStatusEffects::reactCharges
            )
        );

        negativeCharge.init(() ->
            negativeCharge.affinity(
                positiveCharge,
                MdtTurretStatusEffects::reactCharges
            )
        );
    }

    private static void reactCharges(Unit unit, StatusEntry result, float time) {
        unit.damagePierce(chargeReactionDamage);
        Fx.hitLancer.at(unit.x, unit.y, 0f, Color.white);

        // 反应后清除电荷组合，留下短暂“带电”效果。
        result.set(StatusEffects.electrified, Math.max(90f, time * 0.35f));
    }
}
