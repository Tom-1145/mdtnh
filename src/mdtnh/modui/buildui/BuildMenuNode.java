package mdtnh.modui.buildui;

import arc.scene.style.Drawable;
import arc.struct.Seq;
import mindustry.world.Block;

public final class BuildMenuNode {
    public final String path;
    public final String title;
    public Drawable icon;
    public BuildMenuNode parent;
    public final Seq<BuildMenuNode> children = new Seq<>();
    public final Seq<Block> blocks = new Seq<>();

    public BuildMenuNode(String path, String title, Drawable icon) {
        this.path = path;
        this.title = title;
        this.icon = icon;
    }

    public boolean isRoot() {
        return parent == null;
    }
}