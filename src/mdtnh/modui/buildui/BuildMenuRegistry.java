package mdtnh.modui.buildui;

import arc.scene.style.Drawable;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.world.Block;

public final class BuildMenuRegistry {
    public final BuildMenuNode root;
    private final ObjectMap<String, BuildMenuNode> nodes = new ObjectMap<>();
    private final ObjectMap<Block, Seq<BuildMenuNode>> reverse = new ObjectMap<>();

    public BuildMenuRegistry(String rootTitle) {
        root = new BuildMenuNode("", rootTitle, null);
        nodes.put("", root);
    }

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

    public BuildMenuNode primaryNode(Block block) {
        if (block == null) return null;
        Seq<BuildMenuNode> list = reverse.get(block);
        return (list == null || list.isEmpty()) ? null : list.first();
    }

    private String parentPath(String path) {
        int index = path.lastIndexOf('.');
        return index < 0 ? "" : path.substring(0, index);
    }
}