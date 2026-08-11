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
            Color base = Pal.darkMetal;
            if (i < 4) base = COLORS[i];
            // 应急措施。。。

            // ----- 原有基础形态（不变） -----
            items.put(metal + "_ingot", new Item(metal + "-ingot", base) {{ cost = 1.2f; }});
            items.put(metal + "_block", new Item(metal + "-block", base.cpy().mul(0.7f)) {{ cost = 3.0f; }});
            items.put(metal + "_granule", new Item(metal + "-granule", base.cpy().mul(1.2f)) {{ cost = 0.2f; }});
            items.put(metal + "_powder", new Item(metal + "-powder", base.cpy().mul(0.9f)) {{ cost = 0.5f; }});
            items.put(metal + "_small-pile-powder", new Item(metal + "-small-pile-powder", base.cpy().mul(0.8f)) {{ cost = 0.15f; }});
            items.put(metal + "_pinch-powder", new Item(metal + "-pinch-powder", base.cpy().mul(0.7f)) {{ cost = 0.08f; }});
            items.put(metal + "_plate", new Item(metal + "-plate", base.cpy().mul(1.1f)) {{ cost = 1.8f; }});
            items.put(metal + "_foil", new Item(metal + "-foil", base.cpy().mul(1.3f)) {{ cost = 0.8f; }});
            items.put(metal + "_bar", new Item(metal + "-bar", base.cpy().mul(1.1f)) {{ cost = 1.8f; }});  // 杆

            // ----- 新增：板材变种 -----
            items.put(metal + "_dense_plate", new Item(metal + "-dense-plate", base.cpy().mul(0.6f)) {{ cost = 4.5f; }});
            items.put(metal + "_super_dense_plate", new Item(metal + "-super-dense-plate", base.cpy().mul(0.4f)) {{ cost = 8.0f; }});
            items.put(metal + "_2x_plate", new Item(metal + "-2x-plate", base.cpy().mul(0.9f)) {{ cost = 3.6f; }});
            items.put(metal + "_3x_plate", new Item(metal + "-3x-plate", base.cpy().mul(0.85f)) {{ cost = 5.4f; }});
            items.put(metal + "_4x_plate", new Item(metal + "-4x-plate", base.cpy().mul(0.8f)) {{ cost = 7.2f; }});
            items.put(metal + "_5x_plate", new Item(metal + "-5x-plate", base.cpy().mul(0.75f)) {{ cost = 9.0f; }});

            // ----- 特殊：纳米蜂群、热锭 -----
            items.put(metal + "_nano_swarm", new Item(metal + "-nano-swarm", base.cpy().mul(1.5f)) {{ cost = 12.0f; }});
            items.put(metal + "_hot_ingot", new Item(metal + "-hot-ingot", base.cpy().mul(1.6f)) {{ cost = 2.4f; }});

            // ----- 机械零件（长杆、螺栓、螺丝、环、滚珠、弹簧、小弹簧、齿轮、小齿轮、外壳、转子、栏杆、框架）-----
            items.put(metal + "_long_bar", new Item(metal + "-long-bar", base.cpy().mul(0.85f)) {{ cost = 1.2f; }}); // 长杆
            items.put(metal + "_bolt", new Item(metal + "-bolt", base.cpy().mul(1.0f)) {{ cost = 0.3f; }});
            items.put(metal + "_screw", new Item(metal + "-screw", base.cpy().mul(0.95f)) {{ cost = 0.25f; }});
            items.put(metal + "_ring", new Item(metal + "-ring", base.cpy().mul(0.9f)) {{ cost = 0.4f; }});
            items.put(metal + "_ball", new Item(metal + "-ball", base.cpy().mul(0.8f)) {{ cost = 0.5f; }});
            items.put(metal + "_spring", new Item(metal + "-spring", base.cpy().mul(1.1f)) {{ cost = 0.7f; }});
            items.put(metal + "_small_spring", new Item(metal + "-small-spring", base.cpy().mul(1.2f)) {{ cost = 0.35f; }});
            items.put(metal + "_gear", new Item(metal + "-gear", base.cpy().mul(0.8f)) {{ cost = 1.5f; }});
            items.put(metal + "_small_gear", new Item(metal + "-small-gear", base.cpy().mul(0.9f)) {{ cost = 0.75f; }});
            items.put(metal + "_shell", new Item(metal + "-shell", base.cpy().mul(0.7f)) {{ cost = 2.0f; }});
            items.put(metal + "_rotor", new Item(metal + "-rotor", base.cpy().mul(0.8f)) {{ cost = 2.5f; }});
            items.put(metal + "_railing", new Item(metal + "-railing", base.cpy().mul(0.6f)) {{ cost = 1.2f; }});
            items.put(metal + "_frame", new Item(metal + "-frame", base.cpy().mul(0.5f)) {{ cost = 3.0f; }});

            // ----- 细导线 -----
            items.put(metal + "_fine_wire", new Item(metal + "-fine-wire", base.cpy().mul(1.3f)) {{ cost = 0.2f; }});

            // ----- 导线（芯数：1,2,4,8,12,16）-----
            int[] wireCounts = {1, 2, 4, 8, 12, 16};
            for (int count : wireCounts) {
                items.put(metal + "_wire_" + count,
                    new Item(metal + "-wire-" + count, base.cpy().mul(1.0f)) {{ this.cost = 0.3f * count; }});
            }

            // ----- 线缆（芯数：1,2,4,8,12,16）-----
            for (int count : wireCounts) {
                items.put(metal + "_cable_" + count,
                    new Item(metal + "-cable-" + count, base.cpy().mul(0.9f)) {{ this.cost = 0.5f * count; }});
            }

            // ----- 流体管道 & 物品管道（尺寸：微型、小型、中型、大型、巨型、4联、9联）-----
            String[] pipeSizes = {"micro", "small", "medium", "large", "giant", "quad", "nine"};
            float[] pipeCosts = {0.5f, 1.0f, 2.0f, 4.0f, 8.0f, 16.0f, 32.0f};
            for (int j = 0; j < pipeSizes.length; j++) {
                String size = pipeSizes[j];
                float cost = pipeCosts[j];
                items.put(metal + "_fluid_pipe_" + size,
                    new Item(metal + "-fluid-pipe-" + size, base.cpy().mul(0.8f)) {{ this.cost = cost; }});
                items.put(metal + "_item_pipe_" + size,
                    new Item(metal + "-item-pipe-" + size, base.cpy().mul(0.7f)) {{ this.cost = cost * 1.2f; }});
            }
        }
        for (int i = 0; i < ALLOYS.length; i++) {
            String metal = ALLOYS[i];
            Color base = Pal.darkMetal;   // 合金统一使用暗金属色
            // 应急措施。。。

            // ----- 原有基础形态（不变） -----
            items.put(metal + "_ingot", new Item(metal + "-ingot", base) {{ cost = 1.2f; }});
            items.put(metal + "_block", new Item(metal + "-block", base.cpy().mul(0.7f)) {{ cost = 3.0f; }});
            items.put(metal + "_granule", new Item(metal + "-granule", base.cpy().mul(1.2f)) {{ cost = 0.2f; }});
            items.put(metal + "_powder", new Item(metal + "-powder", base.cpy().mul(0.9f)) {{ cost = 0.5f; }});
            items.put(metal + "_small-pile-powder", new Item(metal + "-small-pile-powder", base.cpy().mul(0.8f)) {{ cost = 0.15f; }});
            items.put(metal + "_pinch-powder", new Item(metal + "-pinch-powder", base.cpy().mul(0.7f)) {{ cost = 0.08f; }});
            items.put(metal + "_plate", new Item(metal + "-plate", base.cpy().mul(1.1f)) {{ cost = 1.8f; }});
            items.put(metal + "_foil", new Item(metal + "-foil", base.cpy().mul(1.3f)) {{ cost = 0.8f; }});
            items.put(metal + "_bar", new Item(metal + "-bar", base.cpy().mul(1.1f)) {{ cost = 1.8f; }});  // 杆

            // ----- 新增：板材变种 -----
            items.put(metal + "_dense_plate", new Item(metal + "-dense-plate", base.cpy().mul(0.6f)) {{ cost = 4.5f; }});
            items.put(metal + "_super_dense_plate", new Item(metal + "-super-dense-plate", base.cpy().mul(0.4f)) {{ cost = 8.0f; }});
            items.put(metal + "_2x_plate", new Item(metal + "-2x-plate", base.cpy().mul(0.9f)) {{ cost = 3.6f; }});
            items.put(metal + "_3x_plate", new Item(metal + "-3x-plate", base.cpy().mul(0.85f)) {{ cost = 5.4f; }});
            items.put(metal + "_4x_plate", new Item(metal + "-4x-plate", base.cpy().mul(0.8f)) {{ cost = 7.2f; }});
            items.put(metal + "_5x_plate", new Item(metal + "-5x-plate", base.cpy().mul(0.75f)) {{ cost = 9.0f; }});

            // ----- 特殊：纳米蜂群、热锭 -----
            items.put(metal + "_nano_swarm", new Item(metal + "-nano-swarm", base.cpy().mul(1.5f)) {{ cost = 12.0f; }});
            items.put(metal + "_hot_ingot", new Item(metal + "-hot-ingot", base.cpy().mul(1.6f)) {{ cost = 2.4f; }});

            // ----- 机械零件（长杆、螺栓、螺丝、环、滚珠、弹簧、小弹簧、齿轮、小齿轮、外壳、转子、栏杆、框架）-----
            items.put(metal + "_long_bar", new Item(metal + "-long-bar", base.cpy().mul(0.85f)) {{ cost = 1.2f; }});
            items.put(metal + "_bolt", new Item(metal + "-bolt", base.cpy().mul(1.0f)) {{ cost = 0.3f; }});
            items.put(metal + "_screw", new Item(metal + "-screw", base.cpy().mul(0.95f)) {{ cost = 0.25f; }});
            items.put(metal + "_ring", new Item(metal + "-ring", base.cpy().mul(0.9f)) {{ cost = 0.4f; }});
            items.put(metal + "_ball", new Item(metal + "-ball", base.cpy().mul(0.8f)) {{ cost = 0.5f; }});
            items.put(metal + "_spring", new Item(metal + "-spring", base.cpy().mul(1.1f)) {{ cost = 0.7f; }});
            items.put(metal + "_small_spring", new Item(metal + "-small-spring", base.cpy().mul(1.2f)) {{ cost = 0.35f; }});
            items.put(metal + "_gear", new Item(metal + "-gear", base.cpy().mul(0.8f)) {{ cost = 1.5f; }});
            items.put(metal + "_small_gear", new Item(metal + "-small-gear", base.cpy().mul(0.9f)) {{ cost = 0.75f; }});
            items.put(metal + "_shell", new Item(metal + "-shell", base.cpy().mul(0.7f)) {{ cost = 2.0f; }});
            items.put(metal + "_rotor", new Item(metal + "-rotor", base.cpy().mul(0.8f)) {{ cost = 2.5f; }});
            items.put(metal + "_railing", new Item(metal + "-railing", base.cpy().mul(0.6f)) {{ cost = 1.2f; }});
            items.put(metal + "_frame", new Item(metal + "-frame", base.cpy().mul(0.5f)) {{ cost = 3.0f; }});

            // ----- 细导线 -----
            items.put(metal + "_fine_wire", new Item(metal + "-fine-wire", base.cpy().mul(1.3f)) {{ cost = 0.2f; }});

            // ----- 导线（芯数：1,2,4,8,12,16）-----
            int[] wireCounts = {1, 2, 4, 8, 12, 16};
            for (int count : wireCounts) {
                items.put(metal + "_wire_" + count,
                    new Item(metal + "-wire-" + count, base.cpy().mul(1.0f)) {{ this.cost = 0.3f * count; }});
            }

            // ----- 线缆（芯数：1,2,4,8,12,16）-----
            for (int count : wireCounts) {
                items.put(metal + "_cable_" + count,
                    new Item(metal + "-cable-" + count, base.cpy().mul(0.9f)) {{ this.cost = 0.5f * count; }});
            }

            // ----- 流体管道 & 物品管道（尺寸：微型、小型、中型、大型、巨型、4联、9联）-----
            String[] pipeSizes = {"micro", "small", "medium", "large", "giant", "quad", "nine"};
            float[] pipeCosts = {0.5f, 1.0f, 2.0f, 4.0f, 8.0f, 16.0f, 32.0f};
            for (int j = 0; j < pipeSizes.length; j++) {
                String size = pipeSizes[j];
                float cost = pipeCosts[j];
                items.put(metal + "_fluid_pipe_" + size,
                    new Item(metal + "-fluid-pipe-" + size, base.cpy().mul(0.8f)) {{ this.cost = cost; }});
                items.put(metal + "_item_pipe_" + size,
                    new Item(metal + "-item-pipe-" + size, base.cpy().mul(0.7f)) {{ this.cost = cost * 1.2f; }});
            }
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