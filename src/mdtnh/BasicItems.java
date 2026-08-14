package mdtnh;

import arc.graphics.Color;
import mindustry.type.Item;

public class BasicItems {

    // 内部包装类
    public static class Metal {
    // 原有形态
    public final Item ingot;
    public final Item block;
    public final Item nugget;
    public final Item powder;
    public final Item smallPilePowder;
    public final Item pinchPowder;
    public final Item plate;
    public final Item foil;

    // 新增形态
    public final Item densePlate;
    public final Item superDensePlate;
    public final Item doublePlate;      // 2重板
    public final Item triplePlate;      // 3重板
    public final Item quadruplePlate;   // 4重板
    public final Item quintuplePlate;   // 5重板
    public final Item nanoSwarm;
    public final Item hotIngot;

    public final Item rod;
    public final Item longRod;
    public final Item bolt;
    public final Item screw;
    public final Item ring;
    public final Item round;
    public final Item spring;
    public final Item smallSpring;
    public final Item gear;
    public final Item smallGear;
    public final Item  casing;
    public final Item rotor;
    public final Item railing;
    public final Item frame;

    public final Item fineWire;

    // 导线数组：索引0~5 对应 1,2,4,8,12,16 芯
    public final Item[] wires;
    // 线缆数组：索引0~5 对应 1,2,4,8,12,16 芯
    public final Item[] cables;

    // 管道：分为流体管道和物品管道，每个数组索引0~6 对应 微型、小型、中型、大型、巨型、4联、9联
    public final Item[] fluidPipes;
    public final Item[] itemPipes;

    private Metal(String metalName, Color baseColor) {
        // ----- 原有 -----
        ingot = new Item(metalName + "-ingot", baseColor) {{ cost = 1.2f; }};
        block = new Item(metalName + "-block", baseColor.cpy().mul(0.7f)) {{ cost = 3.0f; }};
        nugget = new Item(metalName + "-nugget", baseColor.cpy().mul(1.2f)) {{ cost = 0.2f; }};
        powder = new Item(metalName + "-powder", baseColor.cpy().mul(0.9f)) {{ cost = 0.5f; }};
        smallPilePowder = new Item(metalName + "-small-pile-powder", baseColor.cpy().mul(0.8f)) {{ cost = 0.15f; }};
        pinchPowder = new Item(metalName + "-pinch-powder", baseColor.cpy().mul(0.7f)) {{ cost = 0.08f; }};
        plate = new Item(metalName + "-plate", baseColor.cpy().mul(1.1f)) {{ cost = 1.8f; }};
        foil = new Item(metalName + "-foil", baseColor.cpy().mul(1.3f)) {{ cost = 0.8f; }};

        // ----- 新增板材变种 -----
        densePlate = new Item(metalName + "-dense-plate", baseColor.cpy().mul(0.6f)) {{ cost = 4.5f; }};
        superDensePlate = new Item(metalName + "-super-dense-plate", baseColor.cpy().mul(0.4f)) {{ cost = 8.0f; }};
        doublePlate = new Item(metalName + "-2x-plate", baseColor.cpy().mul(0.9f)) {{ cost = 3.6f; }};
        triplePlate = new Item(metalName + "-3x-plate", baseColor.cpy().mul(0.85f)) {{ cost = 5.4f; }};
        quadruplePlate = new Item(metalName + "-4x-plate", baseColor.cpy().mul(0.8f)) {{ cost = 7.2f; }};
        quintuplePlate = new Item(metalName + "-5x-plate", baseColor.cpy().mul(0.75f)) {{ cost = 9.0f; }};

        // ----- 特殊 -----
        nanoSwarm = new Item(metalName + "-nano-swarm", baseColor.cpy().mul(1.5f)) {{ cost = 12.0f; }};
        hotIngot = new Item(metalName + "-hot-ingot", baseColor.cpy().mul(1.6f)) {{ cost = 2.4f; }};

        // ----- 零件 -----
        rod = new Item(metalName + "-rod", baseColor.cpy().mul(0.9f)) {{ cost = 0.6f; }};
        longRod = new Item(metalName + "-long-rod", baseColor.cpy().mul(0.85f)) {{ cost = 1.2f; }};
        bolt = new Item(metalName + "-bolt", baseColor.cpy().mul(1.0f)) {{ cost = 0.3f; }};
        screw = new Item(metalName + "-screw", baseColor.cpy().mul(0.95f)) {{ cost = 0.25f; }};
        ring = new Item(metalName + "-ring", baseColor.cpy().mul(0.9f)) {{ cost = 0.4f; }};
        round = new Item(metalName + "-round", baseColor.cpy().mul(0.8f)) {{ cost = 0.5f; }};
        spring = new Item(metalName + "-spring", baseColor.cpy().mul(1.1f)) {{ cost = 0.7f; }};
        smallSpring = new Item(metalName + "-small-spring", baseColor.cpy().mul(1.2f)) {{ cost = 0.35f; }};
        gear = new Item(metalName + "-gear", baseColor.cpy().mul(0.8f)) {{ cost = 1.5f; }};
        smallGear = new Item(metalName + "-small-gear", baseColor.cpy().mul(0.9f)) {{ cost = 0.75f; }};
         casing = new Item(metalName + "- casing", baseColor.cpy().mul(0.7f)) {{ cost = 2.0f; }};
        rotor = new Item(metalName + "-rotor", baseColor.cpy().mul(0.8f)) {{ cost = 2.5f; }};
        railing = new Item(metalName + "-railing", baseColor.cpy().mul(0.6f)) {{ cost = 1.2f; }};
        frame = new Item(metalName + "-frame", baseColor.cpy().mul(0.5f)) {{ cost = 3.0f; }};

        // ----- 细导线 -----
        fineWire = new Item(metalName + "-fine-wire", baseColor.cpy().mul(1.3f)) {{ cost = 0.2f; }};

        // ----- 导线 (芯数: 1,2,4,8,12,16) -----
        int[] wireCounts = {1, 2, 4, 8, 12, 16};
        wires = new Item[wireCounts.length];
        for (int i = 0; i < wireCounts.length; i++) {
            int count = wireCounts[i];
            float cost = 0.3f * count;  // 按比例定价
            wires[i] = new Item(metalName + "-wire-" + count, baseColor.cpy().mul(1.0f)) {{ this.cost = cost; }};
        }

        // ----- 线缆 (芯数: 1,2,4,8,12,16) -----
        cables = new Item[wireCounts.length];
        for (int i = 0; i < wireCounts.length; i++) {
            int count = wireCounts[i];
            float cost = 0.5f * count;  // 线缆比导线稍贵
            cables[i] = new Item(metalName + "-cable-" + count, baseColor.cpy().mul(0.9f)) {{ this.cost = cost; }};
        }

        // ----- 管道 (尺寸: 微型,小型,中型,大型,巨型,4联,9联) -----
        String[] pipeSizes = {"micro", "small", "medium", "large", "giant", "quad", "nine"};
        float[] pipeCosts = {0.5f, 1.0f, 2.0f, 4.0f, 8.0f, 16.0f, 32.0f}; // 粗略指数增长

        fluidPipes = new Item[pipeSizes.length];
        itemPipes = new Item[pipeSizes.length];
        for (int i = 0; i < pipeSizes.length; i++) {
            String size = pipeSizes[i];
            float cost = pipeCosts[i];
            // 流体管道
            fluidPipes[i] = new Item(metalName + "-fluid-pipe-" + size, baseColor.cpy().mul(0.8f)) {{ this.cost = cost; }};
            // 物品管道
            itemPipes[i] = new Item(metalName + "-item-pipe-" + size, baseColor.cpy().mul(0.7f)) {{ this.cost = cost * 1.2f; }};
        }
    }
}

    // 声明所有金属
    public static Metal actinium; // 锕
    public static Metal aluminum; // 铝
    public static Metal americium; // 镅
    public static Metal antimony; // 锑
    public static Metal rodium; // 钡
    public static Metal berkelium; // 锫
    public static Metal beryllium; // 铍
    public static Metal bismuth; // 铋
    public static Metal bohrium; // 𬭛
    public static Metal cesium; // 铯
    public static Metal calcium; // 钙
    public static Metal californium; // 锎
    public static Metal cadmium; // 镉
    public static Metal cerium; // 铈
    public static Metal chromium; // 铬
    public static Metal cobalt; // 钴
    public static Metal copernicium; // 鿔
    public static Metal copper; // 铜
    public static Metal curium; // 锔
    public static Metal darmstadtium; // 𫟼
    public static Metal dubnium; // 𬭊
    public static Metal dysprosium; // 镝
    public static Metal einsteinium; // 锿
    public static Metal erbium; // 铒
    public static Metal europium; // 铕
    public static Metal fermium; // 镄
    public static Metal flerovium; // 𫓧
    public static Metal francium; // 钫
    public static Metal gadolinium; // 钆
    public static Metal gallium; // 镓
    public static Metal gold; // 金
    public static Metal hafnium; // 铪
    public static Metal hassium; // 𬭶
    public static Metal holmium; // 钬
    public static Metal indium; // 铟
    public static Metal iridium; // 铱
    public static Metal iron; // 铁
    public static Metal lanthanum; // 镧
    public static Metal lawrencium; // 铹
    public static Metal lead; // 铅
    public static Metal lithium; // 锂
    public static Metal livermorium; // 𫟷
    public static Metal lutetium; // 镥
    public static Metal magnesium; // 镁
    public static Metal mendelevium; // 钔
    public static Metal manganese; // 锰
    public static Metal meitnerium; // 鿏
    public static Metal mercury; // 汞
    public static Metal molybdenum; // 钼
    public static Metal moscovium; // 镆
    public static Metal neodymium; // 钕
    public static Metal neptunium; // 镎
    public static Metal nickel; // 镍
    public static Metal niobium; // 铌
    public static Metal nobelium; // 锘
    public static Metal osmium; // 锇
    public static Metal palladium; // 钯
    public static Metal polonium; // 钋
    public static Metal platinum; // 铂
    public static Metal plutonium; // 钚
    public static Metal potassium; // 钾
    public static Metal praseodymium; // 镨
    public static Metal promethium; // 钷
    public static Metal protactinium; // 镤
    public static Metal radium; // 镭
    public static Metal rhodium; // 铑
    public static Metal roentgenium; // 𬬭
    public static Metal rubidium; // 铷
    public static Metal ruthenium; // 钌
    public static Metal rutherfordium; // 𬬻
    public static Metal samarium; // 钐
    public static Metal scandium; // 钪
    public static Metal seaborgium; // 𬭳
    public static Metal silver; // 银
    public static Metal sodium; // 钠
    public static Metal strontium; // 锶
    public static Metal tantalum; // 钽
    public static Metal technetium; // 锝
    public static Metal terbium; // 铽
    public static Metal thorium; // 钍
    public static Metal thallium; // 铊
    public static Metal thulium; // 铥
    public static Metal tin; // 锡
    public static Metal titanium; // 钛
    public static Metal tungsten; // 钨
    public static Metal uranium; // 铀
    public static Metal vanadium; // 钒
    public static Metal ytterbium; // 镱
    public static Metal yttrium; // 钇
    public static Metal zinc; // 锌
    public static Metal zirconium; // 锆

    // 合金及自定义材料
    public static Metal annealedCopper; // 退火铜
    public static Metal batteryAlloy; // 电池合金
    public static Metal brass; // 黄铜
    public static Metal bronze; // 青铜
    public static Metal cupronickel; // 白铜
    public static Metal electrum; // 琥珀金
    public static Metal invar; // 殷钢
    public static Metal kanthal; // 坎塔尔合金
    public static Metal magnesiumAluminumAlloy; // 镁铝合金
    public static Metal nichrome; // 镍铬合金
    public static Metal niobiumTitaniumAlloy; // 铌钛合金
    public static Metal crudePlatinum; // 粗铂
    public static Metal sterlingSilver; // 标准纯银
    public static Metal roseGold; // 玫瑰金
    public static Metal blackBronze; // 黑青铜
    public static Metal bismuthBronze; // 铋青铜
    public static Metal rutheniumTungstenMolybdenumAlloy; // 钌钨钼合金
    public static Metal rutheniumIridiumAlloy; // 钌铱合金
    public static Metal solder; // 焊锡
    public static Metal stainlessSteel; // 不锈钢
    public static Metal steel; // 钢
    public static Metal tinIronAlloy; // 锡铁合金
    public static Metal hastelloy; // 哈氏合金
    public static Metal vanadiumGalliumAlloy; // 钒镓合金
    public static Metal wroughtIron; // 锻铁
    public static Metal iridiumOsmiumAlloy; // 铱锇合金
    public static Metal sodiumPotassiumAlloy; // 钠钾合金
    public static Metal magnetizedIron; // 磁化铁
    public static Metal magnetizedNeodymium; // 磁化钕
    public static Metal magnetizedSamarium; // 磁化钐
    public static Metal indiumTinBariumTitaniumCopperOxideAlloy; // 铟锡钡钛铜氧合金
    public static Metal uraniumRhodiumNaquadahAlloy; // 铀铑硅岩合金
    public static Metal enrichedNaquadahKaijinEuropiumKenguraniumAlloy; // 富集硅岩凯金铕铿铀合金
    public static Metal inertMetalMixture; // 惰性金属混合物
    public static Metal metalMixture; // 金属混合物
    public static Metal blackSteel; // 黑钢
    public static Metal damascusSteel; // 大马士革钢
    public static Metal tungstenSteel; // 钨钢
    public static Metal cobaltBrass; // 钴黄铜
    public static Metal magnetizedSteel; // 磁化钢
    public static Metal vanadiumSteel; // 钒钢
    public static Metal crudeBronzeAlloy; // 粗青铜合金
    public static Metal naquadahAlloy; // 硅岩合金
    public static Metal crudePalladium; // 粗钯
    public static Metal rareMetalMixture; // 稀有金属混合物
    public static Metal rhodiumPlatedPalladium; // 镀铑钯
    public static Metal redSteel; // 红钢
    public static Metal blueSteel; // 蓝钢
    public static Metal highSpeedSteelG; // 高速钢-G
    public static Metal redAlloy; // 红色合金
    public static Metal highSpeedSteelE; // 高速钢-E
    public static Metal highSpeedSteelS; // 高速钢-S
    public static Metal iridiumSlag; // 铱金属渣
    public static Metal blueAlloy; // 蓝色合金
    public static Metal hslaSteel; // HSLA钢
    public static Metal waterproofSteel; // 防水钢
    public static Metal heatResistantChromiumIronAlloyMa956; // 耐热铬铁合金_MA_956
    public static Metal maragingSteel300; // 马氏体时效钢_300
    public static Metal hastelloyX; // 哈斯特洛依合金_X
    public static Metal stellite100; // 司太立_100
    public static Metal hastelloyC276; // 哈斯特洛依合金_C_276

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