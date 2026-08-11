package mdtnh.turret;

import mdtnh.ModItems;
import mdtnh.ModLiquids;
import mindustry.content.Items;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.LiquidBulletType;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.Block;

/**
 * 新弹药层的最小使用示例。
 *
 * <p>仅用于展示 API 接法；伤害/射速数值请按你的炮台表继续平衡。</p>
 */
public final class MdtAmmoTurretsExample {

    public static Block accumulatedBoltDemo;
    public static Block steamWhistleDemo;

    private MdtAmmoTurretsExample() {}

    public static void load() {
        Item copperIngot = ModItems.get("copper", "ingot");
        Item bronzeIngot = ModItems.get("bronze", "ingot");
        Item ironIngot = ModItems.get("iron", "ingot");

        // -------- 物品弹药 + 蒸汽能源：类似“蓄积” --------
        accumulatedBoltDemo = new MdtSteamItemTurret("accumulated-bolt-demo") {{
            localizedName = "蓄积-弹药层示例";
            description = "金属锭作为弹药，蒸汽只负责提供射击能量。";

            size = 2;
            health = 520;
            range = 220f;
            reload = 70f;

            energyCapacityJ = 360f;
            energyPerShotJ = 90f;

            liquidCapacity = 24f;
            joulesPerSteamUnit = 72f;
            maxSteamUsePerSecond = 2f;
            steam = ModLiquids.steam;

            maxAmmo = 18;
            ammoPerShot = 1;

            ammo(
                copperIngot, new BasicBulletType(5.5f, 55f) {{
                    ammoMultiplier = 1f;
                    width = 9f;
                    height = 14f;
                    pierce = true;
                }},
                bronzeIngot, new BasicBulletType(5.8f, 70f) {{
                    ammoMultiplier = 1f;
                    width = 10f;
                    height = 15f;
                    pierce = true;
                }},
                ironIngot, new BasicBulletType(6.1f, 85f) {{
                    ammoMultiplier = 1f;
                    width = 10f;
                    height = 16f;
                    pierce = true;
                }}
            );

            requirements(Category.turret, ItemStack.with(
                Items.copper, 70,
                Items.lead, 45
            ));
        }};

        // -------- 流体弹药 + 蒸汽能源：蒸汽同时是燃料和弹药 --------
        steamWhistleDemo = new MdtSteamLiquidTurret("steam-whistle-demo") {{
            localizedName = "气鸣-弹药层示例";
            description = "蒸汽既进入能量转换器，也作为喷射弹药。";

            size = 2;
            health = 460;
            range = 110f;
            reload = 3f;
            targetAir = false;
            targetGround = true;
            recoil = 0f;
            inaccuracy = 5f;
            shootCone = 50f;

            liquidCapacity = 24f;

            energyCapacityJ = 180f;
            energyPerShotJ = 6f;

            steam = ModLiquids.steam;
            joulesPerSteamUnit = 72f;
            maxSteamUsePerSecond = 2.5f;

            // -1 = 自动保留一发蒸汽弹药。
            steamReserveForAmmo = -1f;

            ammo(
                ModLiquids.steam,
                new LiquidBulletType(ModLiquids.steam) {{
                    ammoMultiplier = 4f; // 每发消耗 0.25 单位蒸汽。
                    damage = 8f;
                    knockback = 0.9f;
                    drag = 0.01f;
                    speed = 3.5f;
                    lifetime = 34f;
                    collidesAir = false;
                }}
            );

            requirements(Category.turret, ItemStack.with(
                Items.copper, 55,
                Items.lead, 35
            ));
        }};
    }
}
