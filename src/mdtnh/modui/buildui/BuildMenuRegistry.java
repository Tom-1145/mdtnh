package mdtnh.modui.buildui;

import arc.scene.style.Drawable;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.world.Block;

/**
 * MDT 多级建造菜单的结构注册表。
 *
 * <p>注册表同时维护正向和反向索引：分类路径用于创建和查找节点，
 * 方块反向索引用于从已选方块快速定位其所在分类。</p>
 *
 * <p>子分类注册前必须先存在父分类。该约束可以在内容加载阶段直接发现
 * 分类路径拼写或注册顺序错误。</p>
 */
public final class BuildMenuRegistry {

    /** 菜单树根节点；根路径固定为空字符串。 */
    public final BuildMenuNode root;

    /** 完整分类路径到节点的映射。 */
    private final ObjectMap<String, BuildMenuNode> nodes = new ObjectMap<>();

    /**
     * 方块到所属分类节点的反向映射。
     *
     * <p>同一方块允许出现在多个分类中，因此映射值使用节点序列。</p>
     */
    private final ObjectMap<Block, Seq<BuildMenuNode>> reverse = new ObjectMap<>();

    /**
     * 创建注册表并初始化根节点。
     *
     * @param rootTitle 根节点显示名称
     */
    public BuildMenuRegistry(String rootTitle) {
        root = new BuildMenuNode("", rootTitle, null);
        nodes.put("", root);
    }

    /**
     * 注册分类，并按路径自动连接到父分类。
     *
     * <p>父路径取最后一个点号之前的部分，例如
     * {@code production.voltage} 的父路径为 {@code production}；
     * 顶级分类没有点号，因此直接挂到根节点。</p>
     *
     * @param path  唯一分类路径
     * @param title 玩家可见标题
     * @param icon  分类图标
     * @return 新创建的分类节点
     * @throws IllegalArgumentException 路径重复或父分类不存在
     */
    public BuildMenuNode category(String path, String title, Drawable icon) {
        if (nodes.containsKey(path))
            throw new IllegalArgumentException("Build menu category already exists: " + path);
        String parentPath = parentPath(path);
        BuildMenuNode parent = nodes.get(parentPath);
        if (parent == null)
            throw new IllegalArgumentException("Unknown parent build menu category: " + parentPath);
        BuildMenuNode node = new BuildMenuNode(path, title, icon);
        node.parent = parent;
        parent.children.add(node);
        nodes.put(path, node);
        return node;
    }

    /**
     * 将一个或多个方块加入指定分类，并同步维护反向索引。
     *
     * <p>同一个方块实例不会重复加入同一分类；传入的 {@code null} 元素会被忽略。</p>
     *
     * @param path   目标分类路径
     * @param blocks 要加入的方块
     * @throws IllegalArgumentException 分类路径不存在
     */
    public void add(String path, Block... blocks) {
        BuildMenuNode node = nodes.get(path);
        if (node == null)
            throw new IllegalArgumentException("Unknown build menu category: " + path);
        for (Block block : blocks) {
            if (block == null) continue;
            if (!node.blocks.contains(block, true))
                node.blocks.add(block);
            Seq<BuildMenuNode> list = reverse.get(block);
            if (list == null) {
                list = new Seq<>();
                reverse.put(block, list);
            }
            if (!list.contains(node, true))
                list.add(node);
        }
    }

    /**
     * 查询方块的主要所属分类。
     *
     * <p>如果一个方块被注册到多个分类，以最先注册的分类作为主要节点。
     * 该结果用于界面定位，不影响方块继续显示在其它分类中。</p>
     *
     * @param block 要查询的方块
     * @return 主要分类；未注册时返回 {@code null}
     */
    public BuildMenuNode primaryNode(Block block) {
        if (block == null) return null;
        Seq<BuildMenuNode> list = reverse.get(block);
        return (list == null || list.isEmpty()) ? null : list.first();
    }

    /**
     * 从完整分类路径中截取父路径。
     *
     * @param path 完整分类路径
     * @return 父路径；没有点号时返回根路径 {@code ""}
     */
    private String parentPath(String path) {
        int index = path.lastIndexOf('.');
        return index < 0 ? "" : path.substring(0, index);
    }
}