package mdtnh;

import arc.graphics.Color;
import arc.util.Log;
import mindustry.type.Item;

import java.util.HashMap;
import java.util.Map;

public class ModItems {
    public static Map<String, Item> items = new HashMap<>();

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

    // ---------- 合并 ----------
    private static final String[] ALL_MATERIALS;
    static {
        ALL_MATERIALS = new String[METALS.length + ALLOYS.length];
        System.arraycopy(METALS, 0, ALL_MATERIALS, 0, METALS.length);
        System.arraycopy(ALLOYS, 0, ALL_MATERIALS, METALS.length, ALLOYS.length);
    }

    // ---------- 工具方法 ----------
    private static String toKey(String s) {
        return s.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private static String toName(String s) {
        return s.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    private static Color generateColor(String name) {
        float hue = (name.hashCode() & 0xFF) / 255f;
        return Color.HSVtoRGB(hue, 0.7f, 0.8f);
    }

    // ---------- 加载入口 ----------
    public static void load() {
        for (String material : ALL_MATERIALS) {
            String key = toKey(material);
            String name = toName(material);
            Color base = generateColor(material);

            items.put(key + "_ingot", new Item(name + "-ingot", base) {{ cost = 1.2f; }});
            items.put(key + "_block", new Item(name + "-block", base.cpy().mul(0.7f)) {{ cost = 3.0f; }});
            items.put(key + "_granule", new Item(name + "-granule", base.cpy().mul(1.2f)) {{ cost = 0.2f; }});
            items.put(key + "_powder", new Item(name + "-powder", base.cpy().mul(0.9f)) {{ cost = 0.5f; }});
            items.put(key + "_small-pile-powder", new Item(name + "-small-pile-powder", base.cpy().mul(0.8f)) {{ cost = 0.15f; }});
            items.put(key + "_pinch-powder", new Item(name + "-pinch-powder", base.cpy().mul(0.7f)) {{ cost = 0.08f; }});
            items.put(key + "_plate", new Item(name + "-plate", base.cpy().mul(1.1f)) {{ cost = 1.8f; }});
            items.put(key + "_foil", new Item(name + "-foil", base.cpy().mul(1.3f)) {{ cost = 0.8f; }});
        }

        // 特殊物品
        items.put("nano_swarm", new Item("nano-swarm", Color.valueOf("00FFAA")) {{
            radioactivity = 0.7f;
            cost = 5.0f;
        }});
        items.put("hot_ingot", new Item("hot-ingot", Color.valueOf("FF4500")) {{
            flammability = 0.3f;
            cost = 2.0f;
        }});

        Log.info("ModItems loaded, total items: " + items.size());
        // 游戏自动使用 item.color 生成彩色方块图标，无需额外代码
    }

    public static Item get(String material, String form) {
        return items.get(toKey(material) + "_" + form);
    }
}