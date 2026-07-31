package mdtnh;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.Eachable;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawDefault;

import java.util.Map;
import java.util.Vector;

public class MultiblockStructer extends Block {

    public DrawBlock drawer = new DrawDefault();
    public TextureRegion region;

    public static class LevelStruct{
        public Map<pos,Integer>struct;
        public Vector<Vector<Block>>Mapping;
    }
    Vector<LevelStruct>levels;

    @Override
    public void load() {
        super.load();
        region = Core.atlas.find(name);
    }
    // 静态绘制（底座）
    @Override
    public void drawBase(Tile tile) {
        // DrawDefault 不会在 drawBase 中做特殊处理，直接绘制贴图
        Draw.rect(region, tile.worldx(), tile.worldy());
    }

    // 建造计划预览
    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
        if (drawer != null) {
            drawer.drawPlan(this, plan, list);
        } else {
            Draw.rect(region, plan.drawx(), plan.drawy(), plan.rotation * 90);
        }
    }

    public MultiblockStructer(String name){
        super(name);
        rotate=true;
        update = true;              // 启用建筑更新
        solid = true;
        buildType = MultiblockStructerBuilding::new;
        drawer = new DrawDefault();
    }
    public static class pos {
        int x, y;

        public pos(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            pos pos = (pos) o;
            return x == pos.x && y == pos.y;
        }

        @Override
        public int hashCode() {
            return x * 31 + y;
        }
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("level",(MultiblockStructerBuilding build)->new Bar(
                ()->{
                    if(build.Molded){
                        return "level:"+build.level;
                    }
                    return "未成形";
                },
                () -> Pal.accent,
                ()->1f
        ));
    }

    public class MultiblockStructerBuilding extends Building{
        public boolean Molded;
        public int level;
        public void CheckStruct() {
            level = 0;
            Molded = false;
            for (int i = 1; i <= levels.size(); i++) {
                LevelStruct now = levels.get(i - 1);
                boolean accept = true;
                for (Map.Entry<pos, Integer> ps : now.struct.entrySet()) {
                    int dx = ps.getKey().x;
                    int dy = ps.getKey().y;
                    // 通过世界坐标获取格子
                    Tile checkTile = Vars.world.tile(tile.x + dx, tile.y + dy);
                    if (checkTile == null) {
                        accept = false;
                        break;
                    }
                    Block blockThere = checkTile.block();
                    int typeIndex = ps.getValue();
                    Vector<Block> allowed = now.Mapping.get(typeIndex);
                    if (allowed == null || !allowed.contains(blockThere)) {
                        accept = false;
                        break;
                    }
                }
                if (accept) {
                    level = i;
                    Molded = true;
                    // 如果需要只取最高等级，注释掉 break
                }
            }
        }
        @Override
        public void updateTile() {
            super.updateTile();
            if (timer(0, 60f)) {
                CheckStruct();
            }
        }
        @Override
        public void draw() {
            // 主体绘制委托给 drawer
            if (drawer != null) {
                drawer.draw(this);
            } else {
                Draw.rect(region, x, y);
            }
        }

        // 光源绘制（如果你的方块需要发光效果，保留此重写）
        @Override
        public void drawLight() {
            if (drawer != null) {
                // 只有 DrawBlock 子类实现了 drawLight 时才有效，否则用空实现
                drawer.drawLight(this);
            }
        }

    }
}
