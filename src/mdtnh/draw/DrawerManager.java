package mdtnh.draw;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.struct.Seq;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawDefault;


public class DrawerManager {
    private DrawBlock drawer;

    public DrawerManager() {
        this.drawer = new DrawDefault(); // 默认绘制器
    }

    /** 替换当前绘制器 */
    public void setDrawer(DrawBlock drawer) {
        this.drawer = drawer != null ? drawer : new DrawDefault();
    }

    /** 获取当前绘制器（只读） */
    public DrawBlock getDrawer() {
        return drawer;
    }

    /** 在静态底图阶段绘制核心贴图（仅当绘制器不支持自身绘制时使用） */
    public void drawBase(Block block, TextureRegion region, float x, float y) {
        if (drawer instanceof DrawDefault) {
            Draw.rect(region, x, y);
        } else {
            // 自定义绘制器可能已覆盖此逻辑，但 drawBase 一般由块自己处理，这里留空或委托
            // 实际上 drawBase 在 MultiblockStructer 中已经直接绘制 region，未使用 drawer
        }
    }

    /** 绘制建造预览 */
    public void drawPlan(Block block, BuildPlan plan, Seq<BuildPlan> list) {
        if (drawer != null) {
            drawer.drawPlan(block, plan, list);
        }
    }

    /** 绘制已放置的建筑 */
    public void drawBuilding(Building building) {
        if (drawer != null) {
            drawer.draw(building);
        }
    }

    /** 绘制光照 */
    public void drawLight(Building building) {
        if (drawer != null) {
            drawer.drawLight(building);
        }
    }
}