package mdtnh;

import arc.graphics.Color;
import arc.util.Log;
import mindustry.graphics.Pal;
import mindustry.type.Item;

import java.util.HashMap;
import java.util.Map;

public class ModItems {
    public static Map<String, Item> items = new HashMap<>();
    public static Item tinyPileOfDarkAsh;

    // 完整金属颜色映射表
    private static final Map<String, Color> METAL_COLORS = new HashMap<>();

    static {
        // 前四种特殊颜色（保持与原来一致）
        METAL_COLORS.put("iron", Color.valueOf("C0C0C0"));
        METAL_COLORS.put("copper", Color.valueOf("B87333"));
        METAL_COLORS.put("lead", Color.valueOf("6B6B6B"));
        METAL_COLORS.put("tin", Color.valueOf("C0D0D0"));

        // 其余金属基于真实外观
        METAL_COLORS.put("actinium", Color.valueOf("C0C0C0"));      // 银白色[reference:0]
        METAL_COLORS.put("aluminum", Color.valueOf("D9DAD9"));      // 银白色[reference:1]
        METAL_COLORS.put("americium", Color.valueOf("C0C0C0"));     // 银白色[reference:2]
        METAL_COLORS.put("antimony", Color.valueOf("A0B0C8"));      // 蓝白色[reference:3]
        METAL_COLORS.put("rodium", Color.valueOf("C0C0C0"));        // 银白色[reference:4]
        METAL_COLORS.put("berkelium", Color.valueOf("C0C0C0"));     // 银白色[reference:5]
        METAL_COLORS.put("beryllium", Color.valueOf("A0A0A0"));     // 钢灰色[reference:6]
        METAL_COLORS.put("bismuth", Color.valueOf("C8C0C0"));       // 灰白色带粉红[reference:7]
        METAL_COLORS.put("bohrium", Color.valueOf("C0C0C0"));       // 推测银灰色[reference:8]
        METAL_COLORS.put("cadmium", Color.valueOf("C8D0D8"));       // 蓝白色[reference:9]
        METAL_COLORS.put("calcium", Color.valueOf("D0D0C8"));       // 银灰色[reference:10]
        METAL_COLORS.put("californium", Color.valueOf("C0C0C0"));   // 银白色[reference:11]
        METAL_COLORS.put("cerium", Color.valueOf("C8C8C8"));        // 铁灰色[reference:12]
        METAL_COLORS.put("cesium", Color.valueOf("C8C0B0"));        // 银金色[reference:13]
        METAL_COLORS.put("chromium", Color.valueOf("C0C8D0"));      // 钢灰色[reference:14]
        METAL_COLORS.put("cobalt", Color.valueOf("C8D0D8"));        // 银蓝色[reference:15]
        METAL_COLORS.put("copernicium", Color.valueOf("C0C0C0"));   // 推测银白色[reference:16]
        METAL_COLORS.put("curium", Color.valueOf("C0C0C0"));        // 银白色[reference:17]
        METAL_COLORS.put("darmstadtium", Color.valueOf("C0C0C0"));  // 推测银白色[reference:18]
        METAL_COLORS.put("dubnium", Color.valueOf("C0C0C0"));       // 推测银白色[reference:19]
        METAL_COLORS.put("dysprosium", Color.valueOf("C8C8C8"));    // 银白色[reference:20]
        METAL_COLORS.put("einsteinium", Color.valueOf("C0C0C0"));   // 银色[reference:21]
        METAL_COLORS.put("erbium", Color.valueOf("C8C0C0"));        // 银白色[reference:22]
        METAL_COLORS.put("europium", Color.valueOf("C8C0B0"));      // 银白色[reference:23]
        METAL_COLORS.put("fermium", Color.valueOf("C0C0C0"));       // 推测银白色[reference:24]
        METAL_COLORS.put("flerovium", Color.valueOf("C0C0C0"));     // 推测银白色[reference:25]
        METAL_COLORS.put("francium", Color.valueOf("C8C8C8"));      // 银白色[reference:26]
        METAL_COLORS.put("gadolinium", Color.valueOf("C0C0C0"));    // 银白色[reference:27]
        METAL_COLORS.put("gallium", Color.valueOf("C8C8D0"));       // 银白色[reference:28]
        METAL_COLORS.put("gold", Color.valueOf("FFD700"));          // 金色[reference:29]
        METAL_COLORS.put("hafnium", Color.valueOf("C8C8C8"));       // 银灰色[reference:30]
        METAL_COLORS.put("hassium", Color.valueOf("C0C0C0"));       // 推测银白色[reference:31]
        METAL_COLORS.put("holmium", Color.valueOf("C8C8C8"));       // 银白色[reference:32]
        METAL_COLORS.put("indium", Color.valueOf("C0C8C8"));        // 银白色[reference:33]
        METAL_COLORS.put("iridium", Color.valueOf("D0D0D0"));       // 银白色[reference:34]
        METAL_COLORS.put("lanthanum", Color.valueOf("C0C0C0"));     // 银白色[reference:35]
        METAL_COLORS.put("lawrencium", Color.valueOf("C0C0C0"));    // 推测银白色[reference:36]
        METAL_COLORS.put("lithium", Color.valueOf("C8C8C8"));       // 银白色[reference:37]
        METAL_COLORS.put("livermorium", Color.valueOf("C0C0C0"));   // 推测银白色[reference:38]
        METAL_COLORS.put("lutetium", Color.valueOf("C0C0C0"));      // 银白色[reference:39]
        METAL_COLORS.put("magnesium", Color.valueOf("D0D0D0"));     // 银白色[reference:40]
        METAL_COLORS.put("mendelevium", Color.valueOf("C0C0C0"));   // 推测银白色[reference:41]
        METAL_COLORS.put("manganese", Color.valueOf("C8C0C0"));     // 灰白色带粉红[reference:42]
        METAL_COLORS.put("meitnerium", Color.valueOf("C0C0C0"));    // 推测银白色[reference:43]
        METAL_COLORS.put("mercury", Color.valueOf("D0D0D0"));       // 银白色[reference:44]
        METAL_COLORS.put("molybdenum", Color.valueOf("C8C8C8"));     // 银白色[reference:45]
        METAL_COLORS.put("moscovium", Color.valueOf("C8C8C8"));     // 推测灰白色[reference:46]
        METAL_COLORS.put("neodymium", Color.valueOf("C8C0C0"));     // 银白色[reference:47]
        METAL_COLORS.put("neptunium", Color.valueOf("C0C0C0"));     // 银色[reference:48]
        METAL_COLORS.put("nickel", Color.valueOf("C8C8C0"));        // 银白色带淡金[reference:49]
        METAL_COLORS.put("niobium", Color.valueOf("C8C8C8"));       // 银白色[reference:50]
        METAL_COLORS.put("nobelium", Color.valueOf("C0C0C0"));      // 推测银白色[reference:51]
        METAL_COLORS.put("osmium", Color.valueOf("B0C8D8"));        // 蓝白色[reference:52]
        METAL_COLORS.put("palladium", Color.valueOf("C8C8C8"));     // 银白色[reference:53]
        METAL_COLORS.put("polonium", Color.valueOf("C8C8C8"));      // 银灰色[reference:54]
        METAL_COLORS.put("platinum", Color.valueOf("D0D0D0"));      // 银白色[reference:55]
        METAL_COLORS.put("plutonium", Color.valueOf("C8C8C8"));     // 银灰色[reference:56]
        METAL_COLORS.put("potassium", Color.valueOf("C8C8C8"));     // 银白色[reference:57]
        METAL_COLORS.put("praseodymium", Color.valueOf("C8C8A0"));  // 银黄色[reference:58]
        METAL_COLORS.put("promethium", Color.valueOf("C0C0C0"));    // 银白色[reference:59]
        METAL_COLORS.put("protactinium", Color.valueOf("C8C8C8"));  // 银灰色[reference:60]
        METAL_COLORS.put("radium", Color.valueOf("D0D0D0"));        // 亮白色[reference:61]
        METAL_COLORS.put("rhodium", Color.valueOf("C8C8C8"));       // 银白色[reference:62]
        METAL_COLORS.put("roentgenium", Color.valueOf("C0C0C0"));   // 推测银白色[reference:63]
        METAL_COLORS.put("rubidium", Color.valueOf("C8C8C8"));      // 银白色[reference:64]
        METAL_COLORS.put("ruthenium", Color.valueOf("C8C8C8"));     // 银白色[reference:65]
        METAL_COLORS.put("rutherfordium", Color.valueOf("C0C0C0")); // 推测银白色[reference:66]
        METAL_COLORS.put("samarium", Color.valueOf("C8C8C8"));      // 银白色[reference:67]
        METAL_COLORS.put("scandium", Color.valueOf("C8C8C8"));      // 银白色[reference:68]
        METAL_COLORS.put("seaborgium", Color.valueOf("C0C0C0"));    // 推测银白色[reference:69]
        METAL_COLORS.put("silver", Color.valueOf("C0C0C0"));        // 银白色[reference:70]
        METAL_COLORS.put("sodium", Color.valueOf("C8C8C8"));        // 银白色[reference:71]
        METAL_COLORS.put("strontium", Color.valueOf("C8C8C0"));     // 银白色[reference:72]
        METAL_COLORS.put("tantalum", Color.valueOf("B0C0D0"));      // 蓝灰色[reference:73]
        METAL_COLORS.put("technetium", Color.valueOf("C8C8C8"));    // 银灰色[reference:74]
        METAL_COLORS.put("terbium", Color.valueOf("C8C8C8"));       // 银灰色[reference:75]
        METAL_COLORS.put("thallium", Color.valueOf("B0C8D0"));      // 蓝白色[reference:76]
        METAL_COLORS.put("thorium", Color.valueOf("C0C0C0"));       // 银白色[reference:77]
        METAL_COLORS.put("thulium", Color.valueOf("C8C8C8"));       // 银白色[reference:78]
        METAL_COLORS.put("titanium", Color.valueOf("C8C8C8"));      // 银白色[reference:79]
        METAL_COLORS.put("tungsten", Color.valueOf("C8C8C8"));      // 钢灰色[reference:80]
        METAL_COLORS.put("uranium", Color.valueOf("C8C8C8"));       // 银白色[reference:81]
        METAL_COLORS.put("vanadium", Color.valueOf("C8C8C8"));      // 银灰色[reference:82]
        METAL_COLORS.put("ytterbium", Color.valueOf("C8C8C8"));     // 银白色[reference:83]
        METAL_COLORS.put("yttrium", Color.valueOf("C8C8C8"));       // 银灰色[reference:84]
        METAL_COLORS.put("zinc", Color.valueOf("C8D0D8"));          // 蓝白色[reference:85]
        METAL_COLORS.put("zirconium", Color.valueOf("C8C8C8"));     // 银灰色[reference:86]
    }

    // 合金颜色映射表
    private static final Map<String, Color> ALLOY_COLORS = new HashMap<>();

    static {
        ALLOY_COLORS.put("annealedCopper", Color.valueOf("B87333"));              // 退火铜
        ALLOY_COLORS.put("batteryAlloy", Color.valueOf("A0A0A0"));                // 电池合金
        ALLOY_COLORS.put("brass", Color.valueOf("C8A850"));                       // 黄铜
        ALLOY_COLORS.put("bronze", Color.valueOf("B88040"));                      // 青铜
        ALLOY_COLORS.put("cupronickel", Color.valueOf("C8C8B0"));                 // 白铜
        ALLOY_COLORS.put("electrum", Color.valueOf("D8C850"));                    // 金银合金
        ALLOY_COLORS.put("invar", Color.valueOf("B8B8B8"));                       // 因瓦合金
        ALLOY_COLORS.put("kanthal", Color.valueOf("A0A0A0"));                     // 康泰尔合金
        ALLOY_COLORS.put("magnesiumAluminumAlloy", Color.valueOf("C8C8C8"));      // 镁铝合金
        ALLOY_COLORS.put("nichrome", Color.valueOf("B8B8B8"));                    // 镍铬合金
        ALLOY_COLORS.put("niobiumTitaniumAlloy", Color.valueOf("B8B8C0"));        // 铌钛合金
        ALLOY_COLORS.put("crudePlatinum", Color.valueOf("C0C0C0"));               // 粗铂
        ALLOY_COLORS.put("sterlingSilver", Color.valueOf("C8C8C8"));              // 纯银
        ALLOY_COLORS.put("roseGold", Color.valueOf("D0A080"));                    // 玫瑰金
        ALLOY_COLORS.put("blackBronze", Color.valueOf("5A4A3A"));                 // 黑青铜
        ALLOY_COLORS.put("bismuthBronze", Color.valueOf("A89888"));               // 铋青铜
        ALLOY_COLORS.put("rutheniumTungstenMolybdenumAlloy", Color.valueOf("A8A8A8")); // 钌钨钼合金
        ALLOY_COLORS.put("rutheniumIridiumAlloy", Color.valueOf("B8B8C0"));       // 钌铱合金
        ALLOY_COLORS.put("solder", Color.valueOf("A8A8A8"));                      // 焊料
        ALLOY_COLORS.put("stainlessSteel", Color.valueOf("C8C8C8"));              // 不锈钢
        ALLOY_COLORS.put("steel", Color.valueOf("A0A0A0"));                       // 钢
        ALLOY_COLORS.put("tinIronAlloy", Color.valueOf("B0B8B8"));                // 锡铁合金
        ALLOY_COLORS.put("hastelloy", Color.valueOf("A8A8A8"));                   // 哈氏合金
        ALLOY_COLORS.put("vanadiumGalliumAlloy", Color.valueOf("B0B0B0"));        // 钒镓合金
        ALLOY_COLORS.put("wroughtIron", Color.valueOf("787878"));                 // 熟铁
        ALLOY_COLORS.put("iridiumOsmiumAlloy", Color.valueOf("B0B8C0"));          // 铱锇合金
        ALLOY_COLORS.put("sodiumPotassiumAlloy", Color.valueOf("B8B8B8"));        // 钠钾合金
        ALLOY_COLORS.put("magnetizedIron", Color.valueOf("686868"));              // 磁化铁
        ALLOY_COLORS.put("magnetizedNeodymium", Color.valueOf("686868"));         // 磁化钕
        ALLOY_COLORS.put("magnetizedSamarium", Color.valueOf("686868"));          // 磁化钐
        ALLOY_COLORS.put("indiumTinBariumTitaniumCopperOxideAlloy", Color.valueOf("808080")); // 复杂氧化物合金
        ALLOY_COLORS.put("uraniumRhodiumNaquadahAlloy", Color.valueOf("808080")); // 铀铑纳夸达合金
        ALLOY_COLORS.put("enrichedNaquadahKaijinEuropiumKenguraniumAlloy", Color.valueOf("808080")); // 增强合金
        ALLOY_COLORS.put("inertMetalMixture", Color.valueOf("888888"));           // 惰性金属混合物
        ALLOY_COLORS.put("metalMixture", Color.valueOf("888888"));                // 金属混合物
        ALLOY_COLORS.put("blackSteel", Color.valueOf("505050"));                  // 黑钢
        ALLOY_COLORS.put("damascusSteel", Color.valueOf("707070"));               // 大马士革钢
        ALLOY_COLORS.put("tungstenSteel", Color.valueOf("808080"));               // 钨钢
        ALLOY_COLORS.put("cobaltBrass", Color.valueOf("B09850"));                 // 钴黄铜
        ALLOY_COLORS.put("magnetizedSteel", Color.valueOf("585858"));             // 磁化钢
        ALLOY_COLORS.put("vanadiumSteel", Color.valueOf("888888"));               // 钒钢
        ALLOY_COLORS.put("crudeBronzeAlloy", Color.valueOf("A07848"));            // 粗青铜合金
        ALLOY_COLORS.put("naquadahAlloy", Color.valueOf("808080"));               // 纳夸达合金
        ALLOY_COLORS.put("crudePalladium", Color.valueOf("A8A8A8"));              // 粗钯
        ALLOY_COLORS.put("rareMetalMixture", Color.valueOf("888888"));            // 稀有金属混合物
        ALLOY_COLORS.put("rhodiumPlatedPalladium", Color.valueOf("B8B8B8"));      // 镀铑钯
        ALLOY_COLORS.put("redSteel", Color.valueOf("8A4040"));                    // 红钢
        ALLOY_COLORS.put("blueSteel", Color.valueOf("404080"));                   // 蓝钢
        ALLOY_COLORS.put("highSpeedSteelG", Color.valueOf("909090"));             // 高速钢G
        ALLOY_COLORS.put("redAlloy", Color.valueOf("A04040"));                    // 红合金
        ALLOY_COLORS.put("highSpeedSteelE", Color.valueOf("909090"));             // 高速钢E
        ALLOY_COLORS.put("highSpeedSteelS", Color.valueOf("909090"));             // 高速钢S
        ALLOY_COLORS.put("iridiumSlag", Color.valueOf("707070"));                 // 铱渣
        ALLOY_COLORS.put("blueAlloy", Color.valueOf("4060A0"));                   // 蓝合金
        ALLOY_COLORS.put("hslaSteel", Color.valueOf("888888"));                   // HSLA钢
        ALLOY_COLORS.put("waterproofSteel", Color.valueOf("808080"));             // 防水钢
        ALLOY_COLORS.put("heatResistantChromiumIronAlloyMa956", Color.valueOf("888888")); // 耐热铬铁合金
        ALLOY_COLORS.put("maragingSteel300", Color.valueOf("888888"));            // 马氏体时效钢300
        ALLOY_COLORS.put("hastelloyX", Color.valueOf("888888"));                  // 哈氏合金X
        ALLOY_COLORS.put("stellite100", Color.valueOf("888888"));                 // 司太立100
        ALLOY_COLORS.put("hastelloyC276", Color.valueOf("888888"));               // 哈氏合金C-276
    }

    public static void load() {
        for (Map.Entry<String, Color> entry : METAL_COLORS.entrySet()) {
            String metal = entry.getKey();
            Color base = entry.getValue();

            // 基础形态
            items.put(metal + "_ingot", new Item(metal + "-ingot", base) {{ cost = 1.2f; }});
            items.put(metal + "_block", new Item(metal + "-block", base.cpy().mul(0.7f)) {{ cost = 3.0f; }});
            items.put(metal + "_nugget", new Item(metal + "-nugget", base.cpy().mul(1.2f)) {{ cost = 0.2f; }});
            items.put(metal + "_powder", new Item(metal + "-powder", base.cpy().mul(0.9f)) {{ cost = 0.5f; }});
            items.put(metal + "_small-pile-powder", new Item(metal + "-small-pile-powder", base.cpy().mul(0.8f)) {{ cost = 0.15f; }});
            items.put(metal + "_pinch-powder", new Item(metal + "-pinch-powder", base.cpy().mul(0.7f)) {{ cost = 0.08f; }});
            items.put(metal + "_plate", new Item(metal + "-plate", base.cpy().mul(1.1f)) {{ cost = 1.8f; }});
            items.put(metal + "_foil", new Item(metal + "-foil", base.cpy().mul(1.3f)) {{ cost = 0.8f; }});
            items.put(metal + "_rod", new Item(metal + "-rod", base.cpy().mul(1.1f)) {{ cost = 1.8f; }});

            // 板材变种
            items.put(metal + "_dense_plate", new Item(metal + "-dense-plate", base.cpy().mul(0.6f)) {{ cost = 4.5f; }});
            items.put(metal + "_super_dense_plate", new Item(metal + "-super-dense-plate", base.cpy().mul(0.4f)) {{ cost = 8.0f; }});
            items.put(metal + "_2x_plate", new Item(metal + "-2x-plate", base.cpy().mul(0.9f)) {{ cost = 3.6f; }});
            items.put(metal + "_3x_plate", new Item(metal + "-3x-plate", base.cpy().mul(0.85f)) {{ cost = 5.4f; }});
            items.put(metal + "_4x_plate", new Item(metal + "-4x-plate", base.cpy().mul(0.8f)) {{ cost = 7.2f; }});
            items.put(metal + "_5x_plate", new Item(metal + "-5x-plate", base.cpy().mul(0.75f)) {{ cost = 9.0f; }});

            // 特殊
            items.put(metal + "_nano_swarm", new Item(metal + "-nano-swarm", base.cpy().mul(1.5f)) {{ cost = 12.0f; }});
            items.put(metal + "_hot_ingot", new Item(metal + "-hot-ingot", base.cpy().mul(1.6f)) {{ cost = 2.4f; }});

            // 机械零件
            items.put(metal + "_long-rod", new Item(metal + "-long-rod", base.cpy().mul(0.85f)) {{ cost = 1.2f; }});
            items.put(metal + "_bolt", new Item(metal + "-bolt", base.cpy().mul(1.0f)) {{ cost = 0.3f; }});
            items.put(metal + "_screw", new Item(metal + "-screw", base.cpy().mul(0.95f)) {{ cost = 0.25f; }});
            items.put(metal + "_ring", new Item(metal + "-ring", base.cpy().mul(0.9f)) {{ cost = 0.4f; }});
            items.put(metal + "_round", new Item(metal + "-round", base.cpy().mul(0.8f)) {{ cost = 0.5f; }});
            items.put(metal + "_spring", new Item(metal + "-spring", base.cpy().mul(1.1f)) {{ cost = 0.7f; }});
            items.put(metal + "_small-spring", new Item(metal + "-small-spring", base.cpy().mul(1.2f)) {{ cost = 0.35f; }});
            items.put(metal + "_gear", new Item(metal + "-gear", base.cpy().mul(0.8f)) {{ cost = 1.5f; }});
            items.put(metal + "_small-gear", new Item(metal + "-small-gear", base.cpy().mul(0.9f)) {{ cost = 0.75f; }});
            items.put(metal + "_ casing", new Item(metal + "- casing", base.cpy().mul(0.7f)) {{ cost = 2.0f; }});
            items.put(metal + "_rotor", new Item(metal + "-rotor", base.cpy().mul(0.8f)) {{ cost = 2.5f; }});
            items.put(metal + "_railing", new Item(metal + "-railing", base.cpy().mul(0.6f)) {{ cost = 1.2f; }});
            items.put(metal + "_frame", new Item(metal + "-frame", base.cpy().mul(0.5f)) {{ cost = 3.0f; }});

            // 细导线
            items.put(metal + "_fine-wire", new Item(metal + "-fine-wire", base.cpy().mul(1.3f)) {{ cost = 0.2f; }});

            // 导线
            int[] wireCounts = {1, 2, 4, 8, 12, 16};
            for (int count : wireCounts) {
                items.put(metal + "_wire-" + count,
                        new Item(metal + "-wire-" + count, base.cpy().mul(1.0f)) {{ this.cost = 0.3f * count; }});
            }

            // 线缆
            for (int count : wireCounts) {
                items.put(metal + "_cable_" + count,
                        new Item(metal + "-cable-" + count, base.cpy().mul(0.9f)) {{ this.cost = 0.5f * count; }});
            }

            // 管道
            String[] pipeSizes = {"micro", "small", "medium", "large", "giant", "quad", "nine"};
            float[] pipeCosts = {0.5f, 1.0f, 2.0f, 4.0f, 8.0f, 16.0f, 32.0f};
            for (int j = 0; j < pipeSizes.length; j++) {
                String size = pipeSizes[j];
                float cost = pipeCosts[j];
                items.put(metal + "_fluid-pipe-" + size,
                        new Item(metal + "-fluid-pipe-" + size, base.cpy().mul(0.8f)) {{ this.cost = cost; }});
                items.put(metal + "_item_pipe_" + size,
                        new Item(metal + "-item-pipe-" + size, base.cpy().mul(0.7f)) {{ this.cost = cost * 1.2f; }});
            }
        }

        // 合金
        for (Map.Entry<String, Color> entry : ALLOY_COLORS.entrySet()) {
            String metal = entry.getKey();
            Color base = entry.getValue();

            // 与金属相同的所有形态
            items.put(metal + "_ingot", new Item(metal + "-ingot", base) {{ cost = 1.2f; }});
            items.put(metal + "_block", new Item(metal + "-block", base.cpy().mul(0.7f)) {{ cost = 3.0f; }});
            items.put(metal + "_nugget", new Item(metal + "-nugget", base.cpy().mul(1.2f)) {{ cost = 0.2f; }});
            items.put(metal + "_dust", new Item(metal + "-dust", base.cpy().mul(0.9f)) {{ cost = 0.5f; }});
            items.put(metal + "_small-pile-of-dust", new Item(metal + "-small-pile-of-dust", base.cpy().mul(0.8f)) {{ cost = 0.15f; }});
            items.put(metal + "_tiny-pile-of-dust", new Item(metal + "-tiny-pile-of-dust", base.cpy().mul(0.7f)) {{ cost = 0.08f; }});
            items.put(metal + "_plate", new Item(metal + "-plate", base.cpy().mul(1.1f)) {{ cost = 1.8f; }});
            items.put(metal + "_foil", new Item(metal + "-foil", base.cpy().mul(1.3f)) {{ cost = 0.8f; }});
            items.put(metal + "_rod", new Item(metal + "-rod", base.cpy().mul(1.1f)) {{ cost = 1.8f; }});

            items.put(metal + "_dense_plate", new Item(metal + "-dense-plate", base.cpy().mul(0.6f)) {{ cost = 4.5f; }});
            items.put(metal + "_super_dense_plate", new Item(metal + "-super-dense-plate", base.cpy().mul(0.4f)) {{ cost = 8.0f; }});
            items.put(metal + "_2x_plate", new Item(metal + "-2x-plate", base.cpy().mul(0.9f)) {{ cost = 3.6f; }});
            items.put(metal + "_3x_plate", new Item(metal + "-3x-plate", base.cpy().mul(0.85f)) {{ cost = 5.4f; }});
            items.put(metal + "_4x_plate", new Item(metal + "-4x-plate", base.cpy().mul(0.8f)) {{ cost = 7.2f; }});
            items.put(metal + "_5x_plate", new Item(metal + "-5x-plate", base.cpy().mul(0.75f)) {{ cost = 9.0f; }});

            items.put(metal + "_nano_swarm", new Item(metal + "-nano-swarm", base.cpy().mul(1.5f)) {{ cost = 12.0f; }});
            items.put(metal + "_hot_ingot", new Item(metal + "-hot-ingot", base.cpy().mul(1.6f)) {{ cost = 2.4f; }});

            items.put(metal + "_long-rod", new Item(metal + "-long-rod", base.cpy().mul(0.85f)) {{ cost = 1.2f; }});
            items.put(metal + "_bolt", new Item(metal + "-bolt", base.cpy().mul(1.0f)) {{ cost = 0.3f; }});
            items.put(metal + "_screw", new Item(metal + "-screw", base.cpy().mul(0.95f)) {{ cost = 0.25f; }});
            items.put(metal + "_ring", new Item(metal + "-ring", base.cpy().mul(0.9f)) {{ cost = 0.4f; }});
            items.put(metal + "_round", new Item(metal + "-round", base.cpy().mul(0.8f)) {{ cost = 0.5f; }});
            items.put(metal + "_spring", new Item(metal + "-spring", base.cpy().mul(1.1f)) {{ cost = 0.7f; }});
            items.put(metal + "_small-spring", new Item(metal + "-small-spring", base.cpy().mul(1.2f)) {{ cost = 0.35f; }});
            items.put(metal + "_gear", new Item(metal + "-gear", base.cpy().mul(0.8f)) {{ cost = 1.5f; }});
            items.put(metal + "_small-gear", new Item(metal + "-small-gear", base.cpy().mul(0.9f)) {{ cost = 0.75f; }});
            items.put(metal + "_casing", new Item(metal + "-casing", base.cpy().mul(0.7f)) {{ cost = 2.0f; }});
            items.put(metal + "_rotor", new Item(metal + "-rotor", base.cpy().mul(0.8f)) {{ cost = 2.5f; }});
            items.put(metal + "_bars", new Item(metal + "-bars", base.cpy().mul(0.6f)) {{ cost = 1.2f; }});
            items.put(metal + "_frame", new Item(metal + "-frame", base.cpy().mul(0.5f)) {{ cost = 3.0f; }});

            items.put(metal + "_fine-wire", new Item(metal + "-fine-wire", base.cpy().mul(1.3f)) {{ cost = 0.2f; }});

            int[] wireCounts = {1, 2, 4, 8, 12, 16};
            for (int count : wireCounts) {
                items.put(metal + "_wire-" + count,
                        new Item(metal + "-wire-" + count, base.cpy().mul(1.0f)) {{ this.cost = 0.3f * count; }});
            }
            for (int count : wireCounts) {
                items.put(metal + "_cable_" + count,
                        new Item(metal + "-cable-" + count, base.cpy().mul(0.9f)) {{ this.cost = 0.5f * count; }});
            }

            String[] pipeSizes = {"micro", "small", "medium", "large", "giant", "quad", "nine"};
            float[] pipeCosts = {0.5f, 1.0f, 2.0f, 4.0f, 8.0f, 16.0f, 32.0f};
            for (int j = 0; j < pipeSizes.length; j++) {
                String size = pipeSizes[j];
                float cost = pipeCosts[j];
                items.put(metal + "_fluid-pipe-" + size,
                        new Item(metal + "-fluid-pipe-" + size, base.cpy().mul(0.8f)) {{ this.cost = cost; }});
                items.put(metal + "_item_pipe_" + size,
                        new Item(metal + "-item-pipe-" + size, base.cpy().mul(0.7f)) {{ this.cost = cost * 1.2f; }});
            }
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
        tinyPileOfDarkAsh = new Item("tiny-pile-of-dark-ash", Color.valueOf("000000"));

        Log.info("ModItems loaded, total items: " + items.size());
    }

    public static Item get(String metal, String form) {
        return items.get(metal + "_" + form);
    }
}