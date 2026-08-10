package mdtnh.modui.buildui;

import arc.scene.style.Drawable;
import arc.struct.Seq;
import mindustry.world.Block;

/**
 * 多级建造菜单中的单个分类节点。
 *
 * <p>节点既可以直接保存方块，也可以包含下级分类，因此所有节点组合后构成一棵树。
 * 本类只保存结构数据，不负责 HUD 绘制和玩家交互。</p>
 *
 * <p>{@code path} 是注册表内部使用的稳定路径，例如
 * {@code production.voltage.furnace}；{@code title} 是玩家实际看到的分类名称。
 * 内部路径与显示标题分离后，调整本地化文本不会破坏分类关系。</p>
 */
public final class BuildMenuNode {

    /** 分类完整路径；根节点使用空字符串。 */
    public final String path;

    /** 菜单中展示给玩家的分类标题。 */
    public final String title;

    /** 分类图标；根节点图标同时可作为 MDT 菜单入口图标。 */
    public Drawable icon;

    /** 父分类；根节点没有父节点，因此为 {@code null}。 */
    public BuildMenuNode parent;

    /** 直接子分类，顺序与注册顺序一致。 */
    public final Seq<BuildMenuNode> children = new Seq<>();

    /** 直接归属于当前分类的方块，不递归包含子分类中的方块。 */
    public final Seq<Block> blocks = new Seq<>();

    /**
     * 创建菜单节点。
     *
     * <p>构造函数只保存节点自身信息，父子关系由注册表建立。</p>
     *
     * @param path  分类完整路径
     * @param title 玩家可见标题
     * @param icon  分类图标
     */
    public BuildMenuNode(String path, String title, Drawable icon) {
        this.path = path;
        this.title = title;
        this.icon = icon;
    }

    /**
     * 判断当前节点是否为根节点。
     *
     * @return 没有父节点时返回 {@code true}
     */
    public boolean isRoot() {
        return parent == null;
    }
}