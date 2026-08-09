package mdtnh;

import mdtnh.hatch.*;
import mdtnh.energy.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.consumers.ConsumeItemFlammable;
import mindustry.world.draw.DrawDefault;
import mindustry.world.meta.*;
import java.util.*;

public class ModCrafters {
    public static GenericCrafter Small_Coal_Fired_Boiler;
    public static RecipeCrafter multiFactory;
    @Deprecated
    public static RecipeCrafter test;

    public static SteamRecipeCrafter steamFactory;  // 蒸汽多配方工厂

    // ---------- 多方块舱室 ----------
    public static ItemInputHatch copperInputHatch;
    public static ItemOutputHatch productOutputHatch;

    public static LiquidInputHatch liquidInputHatch;
    public static LiquidOutputHatch liquidOutputHatch;

    public static EnergyInputHatch energyInputHatch;
    public static SteamInputHatch steamInputHatch;

    // ---------- 多方块核心 ----------
    public static MultiblockStructer poweredAltar;

    public static void load() {
        Small_Coal_Fired_Boiler = new GenericCrafter("small-coal-fired-boiler") {{
            health = 100;
            size = 2;
            requirements(Category.crafting, ItemStack.with(Items.copper, 50));
            outputLiquid = new LiquidStack(ModLiquids.steam, 1);
            craftTime = 60;
            drawer = new DrawDefault();
            consume(new ConsumeItemFlammable());
            consumeLiquid(Liquids.water, 1);
        }};

        // 多配方工厂
        multiFactory = new RecipeCrafter("multi-factory") {{
            size = 2;
            health = 300;
            requirements(Category.crafting, ItemStack.with(Items.copper, 80, Items.silicon, 40));
            energySpec.role = EnergySpec.Role.consumer;
            energySpec.voltageV = 12f;
            energySpec.capacityJ = 720f;
            energySpec.maxInputA = 12;
            energySpec.maxOutputA = 0;

            RecipeGroup groupMetals = new RecipeGroup("metals", new RecipeCrafter.Recipe[]{
                    RecipeCrafter.Recipe.items(new ItemStack[]{new ItemStack(Items.copper,3), new ItemStack(Items.lead,2)}, new ItemStack(Items.graphite,1), 60f).energy(144f),
                    RecipeCrafter.Recipe.items(new ItemStack[]{new ItemStack(Items.titanium,2)}, new ItemStack(Items.silicon,2), 50f).energy(200f)
            });
            RecipeGroup groupElectronics = new RecipeGroup("electronics", new RecipeCrafter.Recipe[]{
                    RecipeCrafter.Recipe.items(new ItemStack[]{new ItemStack(Items.copper,1), new ItemStack(Items.silicon,2)}, new ItemStack(Items.metaglass,2), 90f).energy(360f),
                    RecipeCrafter.Recipe.withLiquid(new ItemStack[]{new ItemStack(Items.silicon,3)}, new LiquidStack[]{new LiquidStack(Liquids.water,0.1f)}, new ItemStack(Items.surgeAlloy,1), null, 120f).energy(500f)
            });
            groups = new RecipeCrafter.RecipeGroup[]{groupMetals, groupElectronics};
        }};
        test = multiFactory;

        // 蒸汽多配方工厂（假设已在 SteamRecipeCrafter 中完成定义）
        steamFactory = new SteamRecipeCrafter("steam-factory") {{
            size = 2;
            health = 300;
            requirements(Category.crafting, ItemStack.with(Items.copper, 70, Items.lead, 50));
            // 具体配方在 SteamRecipeCrafter 构造中定义
        }};

        // 注册物品舱室
        copperInputHatch = new ItemInputHatch("copper-input-hatch") {{
            localizedName = "通用输入仓";
            itemCapacity = 20;
            requirements(Category.distribution, ItemStack.with(Items.copper, 30, Items.lead, 15));
        }};
        productOutputHatch = new ItemOutputHatch("product-output-hatch") {{
            localizedName = "通用输出仓";
            itemCapacity = 20;
            requirements(Category.distribution, ItemStack.with(Items.copper, 30, Items.lead, 15));
        }};

        // 注册液体舱室
        liquidInputHatch = new LiquidInputHatch("liquid-input-hatch") {{
            localizedName = "液体输入仓";
            liquidCapacity = 20f;
            requirements(Category.liquid, ItemStack.with(Items.copper, 30, Items.metaglass, 15));
        }};
        liquidOutputHatch = new LiquidOutputHatch("liquid-output-hatch") {{
            localizedName = "液体输出仓";
            liquidCapacity = 20f;
            requirements(Category.liquid, ItemStack.with(Items.copper, 30, Items.metaglass, 15));
        }};

        // 注册能源舱室
        energyInputHatch = new EnergyInputHatch("energy-input-hatch") {{
            localizedName = "能源输入仓";
            requirements(Category.power, ItemStack.with(Items.copper, 50, Items.silicon, 20));
            energySpec.voltageV = 12f;
            energySpec.capacityJ = 4800f;
            energySpec.maxInputA = 32;
        }};

        steamInputHatch = new SteamInputHatch("steam-input-hatch") {{
            localizedName = "蒸汽输入仓";
            requirements(Category.power, ItemStack.with(Items.copper, 50, Items.graphite, 20));
        }};

        // 多方块核心
        poweredAltar = new MultiblockStructer("powered-altar") {{
            size = 1;
            requirements(Category.crafting, ItemStack.with(Items.copper, 100, Items.silicon, 50));
            buildVisibility = BuildVisibility.shown;

            Vector<Block> core = new Vector<>(); core.add(this);
            Vector<Block> in   = new Vector<>(); in.add(copperInputHatch);
            Vector<Block> out  = new Vector<>(); out.add(productOutputHatch);
            Vector<Block> energy = new Vector<>(); energy.add(energyInputHatch);
            Vector<Block> air  = new Vector<>(); air.add(Blocks.air);

            List<List<Block>> mapping = new Vector<>();
            mapping.add(core);   // 0
            mapping.add(in);     // 1
            mapping.add(out);    // 2
            mapping.add(energy); // 3
            mapping.add(air);    // 4

            LevelStruct level1 = new LevelStruct();
            level1.struct = new HashMap<>();
            level1.struct.put(new pos(0,0), 0);
            level1.struct.put(new pos(1,0), 1);
            level1.struct.put(new pos(-1,0), 1);
            level1.struct.put(new pos(0,1), 2);
            level1.struct.put(new pos(0,-1), 4);
            level1.struct.put(new pos(2,0), 3);
            level1.Mapping = mapping;

            levels = new Vector<>();
            levels.add(level1);

            groups = new RecipeGroup[]{
                    new RecipeGroup("smelting", new Recipe[]{
                            Recipe.items(new ItemStack[]{new ItemStack(Items.copper,4), new ItemStack(Items.lead,2)}, new ItemStack(Items.silicon,2), 180f).energy(360f),
                            Recipe.items(new ItemStack[]{new ItemStack(Items.titanium,3), new ItemStack(Items.silicon,2)}, new ItemStack(Items.surgeAlloy,1), 240f).energy(720f)
                    }),
                    new RecipeGroup("advanced", new Recipe[]{
                            Recipe.items(new ItemStack[]{new ItemStack(Items.surgeAlloy,1), new ItemStack(Items.phaseFabric,1)}, new ItemStack(Items.plastanium,2), 300f).energy(1200f)
                    })
            };
            groups[0].Texture_name = "programming-circuit1";
            groups[1].Texture_name = "programming-circuit2";
        }};
    }
}