package mdtnh.turret;

import arc.Events;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.bullet.BulletType;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Building;
import mindustry.gen.Teamc;
import mindustry.logic.LAccess;
import mindustry.type.Item;
import mindustry.ui.Bar;
import mindustry.ui.MultiReqImage;
import mindustry.ui.ReqImage;
import mindustry.world.consumers.ConsumeItemFilter;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.StatValues;

/**
 * 物品弹药 + MDT 能量的炮台层。
 *
 * <p>物品装填、ammoMultiplier、maxAmmo、ammoPerShot 等语义与原版 ItemTurret
 * 保持一致；额外要求 EnergyState 中有足够焦耳。</p>
 */
public class MdtEnergyItemTurret extends MdtEnergyTurret {

    public ObjectMap<Item, BulletType> ammoTypes = new OrderedMap<>();

    public MdtEnergyItemTurret(String name) {
        super(name);
        hasItems = true;
        buildType = MdtEnergyItemTurretBuild::new;
    }

    /** 格式：[item1, bullet1, item2, bullet2, ...] */
    public void ammo(Object... objects) {
        ammoTypes = OrderedMap.of(objects);
    }

    public void limitRange() {
        limitRange(9f);
    }

    public void limitRange(float margin) {
        for (ObjectMap.Entry<Item, BulletType> entry : ammoTypes.entries()) {
            limitRange(entry.value, margin);
        }
    }

    @Override
    public void setStats() {
        super.setStats();

        stats.remove(Stat.itemCapacity);
        stats.add(Stat.ammo, StatValues.ammo(ammoTypes, name));
        stats.add(
            Stat.ammoCapacity,
            maxAmmo / Math.max(ammoPerShot, 1),
            StatUnit.shots
        );
    }

    @Override
    public void setBars() {
        super.setBars();

        addBar("ammo", build -> {
            MdtEnergyItemTurretBuild turret = (MdtEnergyItemTurretBuild) build;

            return new Bar(
                "stat.ammo",
                mindustry.graphics.Pal.ammo,
                () -> maxAmmo <= 0 ? 0f : (float) turret.totalAmmo / maxAmmo
            );
        });
    }

    @Override
    public void init() {
        consume(new ConsumeItemFilter(ammoTypes::containsKey) {
            @Override
            public void build(Building build, Table table) {
                MultiReqImage image = new MultiReqImage();

                Vars.content.items().each(
                    item -> filter.get(item) && item.unlockedNow(),
                    item -> image.add(new ReqImage(
                        new Image(item.uiIcon),
                        () -> build instanceof MdtEnergyItemTurretBuild turret
                            && !turret.ammo.isEmpty()
                            && ((ItemEntry) turret.ammo.peek()).item == item
                    ))
                );

                table.add(image).size(8f * 4f);
            }

            @Override
            public float efficiency(Building build) {
                return build instanceof MdtEnergyItemTurretBuild turret
                    && turret.hasItemAmmoOnly()
                    ? 1f
                    : 0f;
            }

            @Override
            public void display(mindustry.world.meta.Stats stats) {
                // 弹药统计由本炮台自己的 Stat.ammo 显示。
            }
        });

        if (targetGround) {
            ammoTypes.each((item, type) ->
                placeOverlapRange = Math.max(
                    placeOverlapRange,
                    range + type.rangeChange + placeOverlapMargin
                )
            );
        }

        super.init();
    }

    public class MdtEnergyItemTurretBuild extends MdtEnergyTurretBuild {

        protected void selectUsableItemAmmo() {
            if (ammo.size >= 2 && ammo.peek().amount < ammoPerShot) {
                for (int i = 0; i < ammo.size; i++) {
                    if (ammo.get(i).amount >= ammoPerShot) {
                        ammo.swap(ammo.size - 1, i);
                        break;
                    }
                }
            }
        }

        protected boolean hasItemAmmoOnly() {
            selectUsableItemAmmo();

            if (cheating()) {
                return ammoTypes.size > 0;
            }

            return ammo.size > 0 && ammo.peek().amount >= ammoPerShot;
        }

        @Override
        public void onProximityAdded() {
            super.onProximityAdded();

            // 与原版 ItemTurret 一致：沙盒无限资源时补一个弹种用于决定 BulletType。
            if (!hasItemAmmoOnly() && cheating() && ammoTypes.size > 0) {
                handleItem(this, ammoTypes.keys().next());
            }
        }

        @Override
        public boolean hasAmmo() {
            if (!canConsume()) return false;

            selectUsableItemAmmo();

            BulletType type = peekAmmo();
            if (type == null) return false;

            return (cheating() || ammo.peek().amount >= ammoPerShot)
                && hasEnergyFor(type);
        }

        @Override
        public BulletType peekAmmo() {
            selectUsableItemAmmo();
            return ammo.isEmpty() ? null : ammo.peek().type();
        }

        @Override
        public BulletType useAmmo() {
            selectUsableItemAmmo();

            if (ammo.isEmpty()) return null;

            AmmoEntry raw = ammo.peek();
            BulletType type = raw.type();
            if (type == null) return null;

            if (cheating()) return type;

            if (raw.amount < ammoPerShot || !hasEnergyFor(type)) {
                return null;
            }

            // 两种资源都确认足够后再开始扣除，避免只扣一边。
            if (!consumeEnergyFor(type)) return null;

            raw.amount -= ammoPerShot;
            if (raw.amount <= 0) ammo.pop();

            totalAmmo -= ammoPerShot;
            totalAmmo = Math.max(totalAmmo, 0);

            return type;
        }

        @Override
        public UnlockableContent getAmmoContent() {
            selectUsableItemAmmo();
            return ammo.isEmpty() ? null : ((ItemEntry) ammo.peek()).item;
        }

        @Override
        public Object senseObject(LAccess sensor) {
            if (sensor == LAccess.currentAmmoType) {
                selectUsableItemAmmo();
                return ammo.isEmpty() ? null : ((ItemEntry) ammo.peek()).item;
            }

            return super.senseObject(sensor);
        }

        /** 物品炮台恢复原版 @ammo 语义；焦耳通过 MDT energy rod 查看。 */
        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.ammo) return totalAmmo;
            if (sensor == LAccess.ammoCapacity) return maxAmmo;
            return super.sense(sensor);
        }

        @Override
        public float getAmmoFraction() {
            return maxAmmo <= 0 ? 0f : (float) totalAmmo / maxAmmo;
        }

        @Override
        public int acceptStack(Item item, int amount, Teamc source) {
            BulletType type = ammoTypes.get(item);
            if (type == null) return 0;

            return Math.min(
                (int) ((maxAmmo - totalAmmo) / type.ammoMultiplier),
                amount
            );
        }

        @Override
        public void handleStack(Item item, int amount, Teamc source) {
            for (int i = 0; i < amount; i++) {
                handleItem(null, item);
            }
        }

        @Override
        public int removeStack(Item item, int amount) {
            // 与原版 ItemTurret 一样，不支持从炮台主动取出已装填弹药。
            return 0;
        }

        @Override
        public void handleItem(Building source, Item item) {
            BulletType type = ammoTypes.get(item);
            if (type == null) return;

            if (item == Items.pyratite) {
                Events.fire(Trigger.flameAmmo);
            }

            if (totalAmmo == 0) {
                Events.fire(Trigger.resupplyTurret);
            }

            totalAmmo += type.ammoMultiplier;

            for (int i = 0; i < ammo.size; i++) {
                ItemEntry entry = (ItemEntry) ammo.get(i);

                if (entry.item == item) {
                    entry.amount += type.ammoMultiplier;
                    ammo.swap(i, ammo.size - 1);
                    return;
                }
            }

            ammo.add(new ItemEntry(item, (int) type.ammoMultiplier));
        }

        @Override
        public boolean acceptItem(Building source, Item item) {
            BulletType type = ammoTypes.get(item);
            return type != null
                && totalAmmo + type.ammoMultiplier <= maxAmmo;
        }

        @Override
        public void write(Writes write) {
            super.write(write);

            write.b(ammo.size);
            for (AmmoEntry raw : ammo) {
                ItemEntry entry = (ItemEntry) raw;
                write.s(entry.item.id);
                write.s(entry.amount);
            }
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);

            ammo.clear();
            totalAmmo = 0;

            if (revision < 3) {
                return;
            }

            int amount = read.ub();
            for (int i = 0; i < amount; i++) {
                Item item = Vars.content.item(read.s());
                int itemAmount = Math.min(read.s(), maxAmmo);

                if (item != null && ammoTypes.containsKey(item)) {
                    totalAmmo += itemAmount;
                    ammo.add(new ItemEntry(item, itemAmount));
                }
            }
        }

        @Override
        public byte version() {
            return 3;
        }
    }

    public class ItemEntry extends AmmoEntry {
        public Item item;

        public ItemEntry(Item item, int amount) {
            this.item = item;
            this.amount = amount;
        }

        @Override
        public BulletType type() {
            return ammoTypes.get(item);
        }
    }
}
