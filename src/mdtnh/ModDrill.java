package mdtnh;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Log;
import arc.util.Timer;
import mdtnh.draw.DrawerManager;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.OreBlock;
import mindustry.world.draw.DrawBlock;
import mindustry.world.meta.BuildVisibility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.lang.Math.max;

public class ModDrill extends Block {

    private DrawerManager drawerManager = new DrawerManager();
    private TextureRegion region;

    public int top = 1, button = 1, left = 1, right = 1;
    public float drillTime = 60f; // 每次产出的 tick 数

    public ModDrill(String name) {
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        buildVisibility = BuildVisibility.shown;
        requirements(Category.crafting, ItemStack.with(Items.copper, 50));
        itemCapacity = 20;
        buildType = ModDrillBuilding::new;
    }

    @Override
    public void load() {
        super.load();
        region = Core.atlas.find(name);
    }

    public void setDrawer(DrawBlock drawer) {
        drawerManager.setDrawer(drawer);
    }

    public DrawBlock getDrawer() {
        return drawerManager.getDrawer();
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
        Seq<BuildPlan> plans = new Seq<>();
        list.each(plans::add);
        drawerManager.drawPlan(this, plan, plans);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);

        float tile = Vars.tilesize;
        float size = this.size * tile;
        float cx = x * tile + size / 2f;
        float cy = y * tile + size / 2f;

        float leftX   = cx - size/2f - left * tile-tile/2f;
        float rightX  = cx + size/2f + right * tile-tile/2f;
        float bottomY = cy - size/2f - button * tile-tile/2f;
        float topY    = cy + size/2f + top * tile-tile/2f;

        float rectWidth  = rightX - leftX;
        float rectHeight = topY - bottomY;

        if (rectWidth <= 0 || rectHeight <= 0) return;

        Draw.z(Layer.plans + 1f);
        Draw.color(Color.yellow, 0.2f);
        Fill.rect((leftX + rightX) / 2f, (bottomY + topY) / 2f, rectWidth, rectHeight);
        Draw.color(Color.yellow);
        Drawf.dashRect(Color.yellow, leftX, bottomY, rectWidth, rectHeight);
        Draw.reset();
    }
    @Override
    public void setBars(){
        super.setBars();
        addBar("process",(ModDrillBuilding build)->new Bar(
                ()->"process",
                ()->Color.valueOf("ffd37f"),
                ()->build.progress/drillTime
        ));
    }

    // ----- 内部建筑类 -----
    public class ModDrillBuilding extends Building {

        /** 存储每个矿石格子的信息：坐标和对应的物品 */
        private static class OreSlot {
            final int x, y;
            final Item item;
            OreSlot(int x, int y, Item item) {
                this.x = x;
                this.y = y;
                this.item = item;
            }
        }

        /** 当前扫描到的所有矿石格子列表 */
        private List<OreSlot> oreSlots = new ArrayList<>();

        /** 生产进度（tick 累计） */
        private float progress = 0f;

        private int sumHardness = 0;

        /**
         * 重新扫描范围内的矿石格子
         */
        private void rescanOres() {
            oreSlots.clear();
            sumHardness=0;
            for (int dx = -left; dx <= right; dx++) {
                for (int dy = -button; dy <= top; dy++) {
                    Tile checkTile = Vars.world.tile(tile.x + dx, tile.y + dy);
                    if (checkTile == null) continue;

                    Block overlay = checkTile.overlay();
                    if (overlay instanceof OreBlock) {
                        Item drop = ((OreBlock) overlay).itemDrop;
                        if (drop != null) {
                            oreSlots.add(new OreSlot(checkTile.x, checkTile.y, drop));
                            sumHardness+=drop.hardness;
                        }
                    }
                }
            }

            Log.info("钻头 @ 重新扫描，发现 @ 个矿石格子",
                    tile.x + "," + tile.y, oreSlots.size());
        }

        @Override
        public void placed() {
            super.placed();
            rescanOres();
        }

        @Override
        public void updateTile() {

            for(var i:oreSlots){
                if(items.has(i.item))dump(i.item);
            }

            if (oreSlots.isEmpty()) {
                progress = 0f;
                return;
            }

            // 累计进度
            progress += delta()/max(1,sumHardness);

            // 达到产出周期 -> 批量产出
            if (progress >= drillTime) {
                progress -= drillTime;
                // 遍历所有有效的格子，每个格子产出 1 个对应矿物
                for (OreSlot slot : oreSlots) {
                    // 再次检查（防止遍历过程中被修改）
                    Tile t = Vars.world.tile(slot.x, slot.y);
                    if (t == null || !(t.overlay() instanceof OreBlock)) continue;

                    Item item = slot.item;
                    if (item == null) continue;

                    // 检查仓库是否有空间
                    if (items.get(item) >= itemCapacity) continue;
                    // 产出 1 个
                    items.add(item, 1);
                }
            }
        }

        /**
         * 当周围方块发生变化时（如矿石被挖），重新扫描
         */
        @Override
        public void onProximityUpdate() {
            super.onProximityUpdate();
            // 延迟一帧重新扫描，避免频繁更新
            Timer.schedule(this::rescanOres, 0.5f);
        }

        @Override
        public void draw() {
            drawerManager.drawBuilding(this);
        }

        @Override
        public void drawLight() {
            drawerManager.drawLight(this);
        }
    }

    @Override
    public void drawBase(Tile tile) {
        Draw.rect(region, tile.worldx(), tile.worldy());
    }
}