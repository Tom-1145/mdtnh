package mdtnh;

import arc.graphics.Color;
import arc.util.Log;
import mindustry.graphics.Pal;
import mindustry.type.Item;

import java.util.HashMap;
import java.util.Map;

public class ModItems {
    // 公开的静态 Map，供工厂使用
    public static Map<String, Item> items = new HashMap<>();
    public static Item tinyPileOfDarkAsh;
    // 金属列表（可按需增删）

    private static final Color[] COLORS = {
            Color.valueOf("C0C0C0"), // 铁
            Color.valueOf("B87333"), // 铜
            Color.valueOf("6B6B6B"), // 铅
            Color.valueOf("C0D0D0")  // 锡
    };
    // ---------- 所有纯金属 ----------
    private static final String[] METALS = {
            "actinium", "aluminum", "americium", "antimony", "barium", "berkelium", "beryllium", "bismuth", "bohrium",
            "cesium", "calcium", "californium", "cadmium", "cerium", "chromium", "cobalt", "copernicium", "copper",
            "curium", "darmstadtium", "dubnium", "dysprosium", "einsteinium", "erbium", "europium", "fermium",
            "flerovium", "francium", "gadolinium", "gallium", "gold", "hafnium", "hassium", "holmium", "indium",
            "iridium", "iron", "lanthanum", "lawrencium", "lead", "lithium", "livermorium", "lutetium", "magnesium",
            "mendelevium", "manganese", "meitnerium", "mercury", "molybdenum", "moscovium", "neodymium", "neptunium",
            "nickel", "niobium", "nobelium", "osmium", "palladium", "polonium", "platinum", "plutonium", "potassium",
            "praseodymium", "promethium", "protactinium", "radium", "rhodium", "roentgenium", "rubidium", "ruthenium",
            "rutherfordium", "samarium", "scandium", "seaborgium", "silver", "sodium", "strontium", "tantalum",
            "technetium", "terbium", "thorium", "thallium", "thulium", "tin", "titanium", "tungsten", "uranium",
            "vanadium", "ytterbium", "yttrium", "zinc", "zirconium"
    };

    // ---------- 所有合金 ----------
    private static final String[] ALLOYS = {
            "annealedCopper", "batteryAlloy", "brass", "bronze", "cupronickel", "electrum", "invar",
            "kanthal", "magnesiumAluminumAlloy", "nichrome", "niobiumTitaniumAlloy", "crudePlatinum",
            "sterlingSilver", "roseGold", "blackBronze", "bismuthBronze", "rutheniumTungstenMolybdenumAlloy",
            "rutheniumIridiumAlloy", "solder", "stainlessSteel", "steel", "tinIronAlloy", "hastelloy",
            "vanadiumGalliumAlloy", "wroughtIron", "iridiumOsmiumAlloy", "sodiumPotassiumAlloy",
            "magnetizedIron", "magnetizedNeodymium", "magnetizedSamarium",
            "indiumTinBariumTitaniumCopperOxideAlloy", "uraniumRhodiumNaquadahAlloy",
            "enrichedNaquadahKaijinEuropiumKenguraniumAlloy", "inertMetalMixture", "metalMixture",
            "blackSteel", "damascusSteel", "tungstenSteel", "cobaltBrass", "magnetizedSteel",
            "vanadiumSteel", "crudeBronzeAlloy", "naquadahAlloy", "crudePalladium", "rareMetalMixture",
            "rhodiumPlatedPalladium", "redSteel", "blueSteel", "highSpeedSteelG", "redAlloy",
            "highSpeedSteelE", "highSpeedSteelS", "iridiumSlag", "blueAlloy", "hslaSteel",
            "waterproofSteel", "heatResistantChromiumIronAlloyMa956", "maragingSteel300",
            "hastelloyX", "stellite100", "hastelloyC276"
    };

    public static void load() {
        for (int i = 0; i < METALS.length; i++) {
            String metal = METALS[i];
            Color base= Pal.darkMetal;
            if(i<4)base = COLORS[i];
            //应急措施。。。

            // 锭
            items.put(metal + "_ingot", new Item(metal + "-ingot", base) {{ cost = 1.2f; }});
            // 块
            items.put(metal + "_block", new Item(metal + "-block", base.cpy().mul(0.7f)) {{ cost = 3.0f; }});
            // 粒
            items.put(metal + "_granule", new Item(metal + "-granule", base.cpy().mul(1.2f)) {{ cost = 0.2f; }});
            // 粉
            items.put(metal + "_powder", new Item(metal + "-powder", base.cpy().mul(0.9f)) {{ cost = 0.5f; }});
            // 小堆粉
            items.put(metal + "_small-pile-powder", new Item(metal + "-small-pile-powder", base.cpy().mul(0.8f)) {{ cost = 0.15f; }});
            // 小撮粉
            items.put(metal + "_pinch-powder", new Item(metal + "-pinch-powder", base.cpy().mul(0.7f)) {{ cost = 0.08f; }});
            // 板
            items.put(metal + "_plate", new Item(metal + "-plate", base.cpy().mul(1.1f)) {{ cost = 1.8f; }});
            // 箔
            items.put(metal + "_foil", new Item(metal + "-foil", base.cpy().mul(1.3f)) {{ cost = 0.8f; }});

            // 杆
            items.put(metal + "_bar", new Item(metal + "-bar", base.cpy().mul(1.1f)) {{ cost = 1.8f; }});
        }

        // 特殊物品（可选）
        items.put("nano_swarm", new Item("nano-swarm", Color.valueOf("00FFAA")) {{
            radioactivity = 0.7f;
            cost = 5.0f;
        }});
        items.put("hot_ingot", new Item("hot-ingot", Color.valueOf("FF4500")) {{
            flammability = 0.3f;
            cost = 2.0f;
        }});
        tinyPileOfDarkAsh = new Item ("tiny-pile-of-dark-ash", Color.valueOf("000000"));
        Log.info("ModItems loaded, total items: " + items.size());
    }

    // 便捷获取（可选）
    public static Item get(String metal, String form) {
        return items.get(metal + "_" + form);
    }
}