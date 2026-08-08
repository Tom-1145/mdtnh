package mdtnh;

import arc.graphics.Color;
import mindustry.type.Item;

import java.util.TreeSet;

public class TagItem extends Item {
    public TreeSet<ItemTag> Tags;
    public TagItem(String name, Color color) {
        super(name, color);
    }
    public TagItem(String name) {
        super(name);
    }
    public void addTag(ItemTag t){
        Tags.add(t);
    }
    public void removeTag(ItemTag t){
       Tags.remove(t);
    }
    public boolean hasTag(ItemTag t){
        return Tags.contains(t);
    }
}
