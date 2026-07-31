package mdtnh;

import arc.graphics.Color;
import mindustry.type.Item;

public class BasicItems {

    // 内部包装类
    public static class Metal {
        public final Item ingot;
        public final Item block;
        public final Item granule;
        public final Item powder;
        public final Item smallPilePowder;
        public final Item pinchPowder;
        public final Item plate;
        public final Item foil;

        private Metal(String metalName, Color baseColor) {
            ingot = new Item(metalName + "-ingot", baseColor) {{ cost = 1.2f; }};
            block = new Item(metalName + "-block", baseColor.cpy().mul(0.7f)) {{ cost = 3.0f; }};
            granule = new Item(metalName + "-granule", baseColor.cpy().mul(1.2f)) {{ cost = 0.2f; }};
            powder = new Item(metalName + "-powder", baseColor.cpy().mul(0.9f)) {{ cost = 0.5f; }};
            smallPilePowder = new Item(metalName + "-small-pile-powder", baseColor.cpy().mul(0.8f)) {{ cost = 0.15f; }};
            pinchPowder = new Item(metalName + "-pinch-powder", baseColor.cpy().mul(0.7f)) {{ cost = 0.08f; }};
            plate = new Item(metalName + "-plate", baseColor.cpy().mul(1.1f)) {{ cost = 1.8f; }};
            foil = new Item(metalName + "-foil", baseColor.cpy().mul(1.3f)) {{ cost = 0.8f; }};
        }
    }

    // 声明所有金属
    public static Metal iron;
    public static Metal copper;
    public static Metal lead;
    public static Metal tin;
    // 可继续添加...

    // 特殊物品
    public static Item nanoSwarm;
    public static Item hotIngot;

    public static void load() {
        iron = new Metal("iron", Color.valueOf("C0C0C0"));
        copper = new Metal("copper", Color.valueOf("B87333"));
        lead = new Metal("lead", Color.valueOf("6B6B6B"));
        tin = new Metal("tin", Color.valueOf("C0D0D0"));

        nanoSwarm = new Item("nano-swarm", Color.valueOf("00FFAA")) {{
            radioactivity = 0.7f;
            cost = 5.0f;
        }};
        hotIngot = new Item("hot-ingot", Color.valueOf("FF4500")) {{
            flammability = 0.3f;
            cost = 2.0f;
        }};
    }
}