package mdtnh;

import arc.graphics.Color;
import mindustry.type.Item;

public class ModItems {
    public static Item iron_ingot;

    public static void load(){
        iron_ingot=new Item("iron-ingot", Color.white);
    }
}
