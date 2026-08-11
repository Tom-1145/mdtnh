package mdtnh.turret;

import mdtnh.ModItems;
import mdtnh.ModLiquids;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.entities.bullet.ArtilleryBulletType;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.pattern.ShootSpread;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.Block;

/**
 * 根据“炮台.txt”实现目前已有物品/液体体系能够支撑的剩余炮台。
 *
 * <p>注意：文件未给出 MV/HV 的具体额定电压、最低输入电压和最高输入电压，
 * 所以“激发/阔剑”这里只实现武器与能源机制，不擅自写死 MV/HV 电压值。
 * 请在你的电压等级表确定后补充 voltageV/minInputVoltageV/maxInputVoltageV。</p>
 */
public final class MdtImplementedTurrets {

    public static Block accumulated;   // 蓄积
    public static Block ironWave;      // 铁浪
    public static Block thrower;       // 抛掷

    public static Block electricArc;   // 电击
    public static Block magneticRail;  // 磁轨
    public static Block dispersal;     // 驱散
    public static Block electrode;     // 电极

    public static Block excitation;    // 激发
    public static Block broadsword;    // 阔剑

    private MdtImplementedTurrets() {}

    public static void load() {
        MdtTurretStatusEffects.load();

        Item copperIngot = ModItems.get("copper", "ingot");
        Item bronzeIngot = ModItems.get("bronze", "ingot");
        Item ironIngot = ModItems.get("iron", "ingot");
        Item steelIngot = ModItems.get("steel", "ingot");
        Item tungstenIngot = ModItems.get("tungsten", "ingot");

        // “金属滚珠”统一改为粒（granule）。
        Item copperGranule = ModItems.get("copper", "granule");
        Item bronzeGranule = ModItems.get("bronze", "granule");
        Item ironGranule = ModItems.get("iron", "granule");
        Item steelGranule = ModItems.get("steel", "granule");

        // ============================================================
        // 蒸汽
        // ============================================================

        accumulated = new MdtSteamItemTurret("accumulated") {{
            localizedName = "蓄积";
            description = "以蒸汽蓄力发射金属螺栓；无限穿透，每穿透一次剩余伤害减半。";

            size = 2;
            health = 620;
            range = 270f;
            reload = 105f;
            rotateSpeed = 3f;
            shootCone = 8f;

            // 蓄力。
            shoot.firstShotDelay = 42f;
            moveWhileCharging = false;
            reloadWhileCharging = false;

            steam = ModLiquids.steam;
            liquidCapacity = 28f;
            joulesPerSteamUnit = 80f;
            maxSteamUsePerSecond = 2.2f;

            energyCapacityJ = 420f;
            energyPerShotJ = 120f;

            maxAmmo = 18;
            ammoPerShot = 1;

            ammo(
                copperIngot, new HalvingPierceBulletType(6.2f, 95f) {{
                    ammoMultiplier = 1f;
                    lifetime = 46f;
                    width = 8f;
                    height = 16f;
                    damageRetention = 0.5f;
                    minDamage = 1f;
                }},
                bronzeIngot, new HalvingPierceBulletType(6.5f, 120f) {{
                    ammoMultiplier = 1f;
                    lifetime = 46f;
                    width = 9f;
                    height = 17f;
                    damageRetention = 0.5f;
                    minDamage = 1f;
                }},
                ironIngot, new HalvingPierceBulletType(6.8f, 145f) {{
                    ammoMultiplier = 1f;
                    lifetime = 46f;
                    width = 9f;
                    height = 18f;
                    damageRetention = 0.5f;
                    minDamage = 1f;
                }}
            );

            requirements(Category.turret, ItemStack.with(
                Items.copper, 90,
                Items.lead, 70,
                Items.graphite, 35
            ));
        }};

        ironWave = new MdtSteamItemTurret("iron-wave") {{
            localizedName = "铁浪";
            description = "以蒸汽快速发射金属粒，可对地/对空。";

            size = 2;
            health = 560;
            range = 165f;
            reload = 4.5f;
            rotateSpeed = 8f;
            inaccuracy = 4f;

            steam = ModLiquids.steam;
            liquidCapacity = 30f;
            joulesPerSteamUnit = 68f;
            maxSteamUsePerSecond = 3f;

            energyCapacityJ = 210f;
            energyPerShotJ = 7f;

            maxAmmo = 90;
            ammoPerShot = 1;

            ammo(
                copperGranule, new BasicBulletType(5.2f, 11f) {{
                    ammoMultiplier = 1f;
                    width = height = 6f;
                    lifetime = 34f;
                }},
                bronzeGranule, new BasicBulletType(5.5f, 14f) {{
                    ammoMultiplier = 1f;
                    width = height = 6.5f;
                    lifetime = 34f;
                }},
                ironGranule, new BasicBulletType(5.8f, 18f) {{
                    ammoMultiplier = 1f;
                    width = height = 7f;
                    lifetime = 34f;
                    armorMultiplier = 0.85f;
                }}
            );

            requirements(Category.turret, ItemStack.with(
                Items.copper, 75,
                Items.lead, 50
            ));
        }};

        thrower = new MdtSteamItemTurret("thrower") {{
            localizedName = "抛掷";
            description = "投掷金属锭进行抛射轰击；消耗大量蒸汽，仅对地。";

            size = 3;
            health = 980;
            range = 240f; // 接近当前原版 Hail 的弹道距离。
            minRange = 45f;
            reload = 85f;
            rotateSpeed = 2.2f;
            targetAir = false;
            targetGround = true;

            steam = ModLiquids.steam;
            liquidCapacity = 45f;
            joulesPerSteamUnit = 85f;
            maxSteamUsePerSecond = 5f;

            energyCapacityJ = 680f;
            energyPerShotJ = 240f;

            maxAmmo = 24;
            ammoPerShot = 1;

            // “金属锭”没有进一步限定材料，这里给出铁/钢/钨三个可配置档位。
            ammo(
                ironIngot, new ArtilleryBulletType(3f, 65f) {{
                    ammoMultiplier = 1f;
                    lifetime = 80f;
                    width = height = 13f;
                    splashDamage = 55f;
                    splashDamageRadius = 28f;
                    knockback = 1.2f;
                }},
                steelIngot, new ArtilleryBulletType(3f, 90f) {{
                    ammoMultiplier = 1f;
                    lifetime = 80f;
                    width = height = 14f;
                    splashDamage = 75f;
                    splashDamageRadius = 30f;
                    knockback = 1.4f;
                }},
                tungstenIngot, new ArtilleryBulletType(3f, 120f) {{
                    ammoMultiplier = 1f;
                    lifetime = 80f;
                    width = height = 15f;
                    splashDamage = 100f;
                    splashDamageRadius = 32f;
                    knockback = 1.6f;
                }}
            );

            requirements(Category.turret, ItemStack.with(
                Items.copper, 150,
                Items.lead, 120,
                Items.graphite, 80
            ));
        }};

        // ============================================================
        // LV
        // ============================================================

        electricArc = new MdtArcTurret("electric-arc") {{
            localizedName = "电击";
            description = "尖端放电同时攻击范围内多个敌人；目标越多，实际耗电越高。";

            size = 2;
            health = 530;
            range = 125f;
            reload = 24f;
            rotateSpeed = 7f;

            targetBlocks = false;
            targetAir = true;
            targetGround = true;

            voltageV = 12f;
            minInputVoltageV = 10f;
            maxInputVoltageV = 14f;
            maxInputA = 6;

            energyCapacityJ = 260f;
            energyPerTargetJ = 14f;
            damagePerTarget = 34f;
            maxTargets = 0;

            requirements(Category.turret, ItemStack.with(
                Items.copper, 85,
                Items.lead, 55,
                Items.silicon, 30
            ));
        }};

        magneticRail = new MdtElectricItemTurret("magnetic-rail") {{
            localizedName = "磁轨";
            description = "通过电磁铁加速钢粒攻击，最多穿透 3 个目标。";

            size = 3;
            health = 980;
            range = 285f;
            reload = 52f;
            rotateSpeed = 3f;
            shootCone = 6f;

            voltageV = 12f;
            minInputVoltageV = 10f;
            maxInputVoltageV = 14f;
            maxInputA = 8;

            energyCapacityJ = 520f;
            energyPerShotJ = 90f;

            maxAmmo = 30;
            ammoPerShot = 1;

            ammo(
                steelGranule, new BasicBulletType(8.5f, 92f) {{
                    ammoMultiplier = 1f;
                    lifetime = 36f;
                    width = height = 9f;
                    pierce = true;
                    pierceBuilding = true;
                    pierceCap = 3;
                    armorMultiplier = 0.65f;
                }}
            );

            requirements(Category.turret, ItemStack.with(
                Items.copper, 160,
                Items.lead, 110,
                Items.silicon, 80
            ));
        }};

        dispersal = new MdtElectricTurret("dispersal") {{
            localizedName = "驱散";
            description = "发出强风击退敌人，对空军产生更强击退。";

            size = 3;
            health = 840;
            range = 175f;
            reload = 11f;
            rotateSpeed = 5f;
            shootCone = 18f;
            inaccuracy = 8f;

            voltageV = 12f;
            minInputVoltageV = 10f;
            maxInputVoltageV = 14f;
            maxInputA = 5;

            energyCapacityJ = 300f;
            energyPerShotJ = 20f;

            shootType = new WindBulletType(4.8f, 2f) {{
                lifetime = 38f;
                width = 16f;
                height = 10f;
                knockback = 3.2f;
                airKnockbackMultiplier = 3f;
                pierce = true;
                pierceCap = 4;
                hitEffect = Fx.none;
                despawnEffect = Fx.none;
            }};

            requirements(Category.turret, ItemStack.with(
                Items.copper, 130,
                Items.lead, 80,
                Items.silicon, 45
            ));
        }};

        electrode = new MdtElectricItemTurret("electrode") {{
            localizedName = "电极";
            description = "发射带正/负电荷的金属粒；正负电荷相遇会触发高额电击伤害。";

            size = 3;
            health = 930;
            range = 215f;
            reload = 18f;
            rotateSpeed = 4f;

            voltageV = 12f;
            minInputVoltageV = 10f;
            maxInputVoltageV = 14f;
            maxInputA = 7;

            energyCapacityJ = 460f;
            energyPerShotJ = 42f;

            maxAmmo = 50;
            ammoPerShot = 1;

            ammo(
                copperGranule, new BasicBulletType(6f, 30f) {{
                    ammoMultiplier = 1f;
                    lifetime = 38f;
                    width = height = 8f;
                    status = MdtTurretStatusEffects.positiveCharge;
                    statusDuration = 60f * 5f;
                }},
                ironGranule, new BasicBulletType(6f, 34f) {{
                    ammoMultiplier = 1f;
                    lifetime = 38f;
                    width = height = 8f;
                    status = MdtTurretStatusEffects.negativeCharge;
                    statusDuration = 60f * 5f;
                }}
            );

            requirements(Category.turret, ItemStack.with(
                Items.copper, 170,
                Items.lead, 120,
                Items.silicon, 95
            ));
        }};

        // ============================================================
        // MV
        // ============================================================

        excitation = new MdtElectricTurret("excitation") {{
            localizedName = "激发";
            description = "持续索敌单个目标的远程激光；目标越远，伤害越低。";

            size = 2;
            health = 720;
            range = 520f;
            reload = 34f;
            rotateSpeed = 3.2f;
            shootCone = 5f;

            /*
             * 炮台.txt 没有给 MV 的具体电压窗口。
             * 这里不覆盖 voltageV/minInputVoltageV/maxInputVoltageV；
             * 接入正式 MV 电网前请按你的等级表设置。
             */
            maxInputA = 8;

            energyCapacityJ = 760f;
            energyPerShotJ = 110f;

            shootType = new TrackingContinuousLaserBulletType(18f) {{
                length = 525f;
                lifetime = 34f;
                damageInterval = 5f;
                width = 8f;

                falloffStart = 120f;
                falloffEnd = 520f;
                minDamageMultiplier = 0.28f;
                trackingSpeed = 7f;

                pierceCap = 1;
            }};

            requirements(Category.turret, ItemStack.with(
                Items.copper, 190,
                Items.lead, 130,
                Items.silicon, 140
            ));
        }};

        // ============================================================
        // HV
        // ============================================================

        broadsword = new MdtElectricItemTurret("broadsword") {{
            localizedName = "阔剑";
            description = "向前方扇形区域一次性发射大量钢粒。";

            size = 4;
            health = 1650;
            range = 205f;
            reload = 9.5f;
            rotateSpeed = 2f;
            shootCone = 35f;
            recoil = 4f;
            shake = 2f;

            /*
             * 炮台.txt 没有给 HV 的具体电压窗口。
             * 这里同样只实现能源消耗机制，不擅自定义 HV 电压。
             */
            maxInputA = 16;

            energyCapacityJ = 1800f;
            energyPerShotJ = 14f;

            maxAmmo = 180;
            ammoPerShot = 1;

            // 每一颗钢粒都独立扣 1 个粒 + 14 J。
            consumeAmmoOnce = false;
            shoot = new ShootSpread(24, 2.35f);

            ammo(
                steelGranule, new BasicBulletType(6.6f, 260f) {{
                    ammoMultiplier = 1f;
                    lifetime = 33f;
                    width = height = 7.5f;
                    knockback = 0.45f;
                    armorMultiplier = 0.8f;
                }}
            );

            requirements(Category.turret, ItemStack.with(
                Items.copper, 320,
                Items.lead, 240,
                Items.silicon, 220,
                Items.titanium, 160
            ));
        }};
    }
}
