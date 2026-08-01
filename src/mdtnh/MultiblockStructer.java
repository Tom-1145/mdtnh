package mdtnh;

import arc.Core;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.draw.*;

import java.util.*;

public class MultiblockStructer extends Block {

    public DrawBlock drawer = new DrawDefault();
    public TextureRegion region;

    /** 配方（仅物品） */
    public static class Recipe {
        public ItemStack[] inputItems;
        public ItemStack outputItem;
        public float craftTime;

        public Recipe(ItemStack[] inputItems, ItemStack outputItem, float craftTime) {
            this.inputItems = inputItems;
            this.outputItem = outputItem;
            this.craftTime = craftTime;
        }

        public static Recipe items(ItemStack[] in, ItemStack out, float time) {
            return new Recipe(in, out, time);
        }
    }

    /** 配方组（字段与 RecipeCrafter 完全一致） */
    public static class RecipeGroup {
        public String name;
        public TextureRegion icon;
        public String Texture_name;
        public Recipe[] recipes;

        public RecipeGroup(String name, Recipe[] recipes) {
            this.name = name;
            this.recipes = recipes;
        }
    }

    /** 结构等级定义 */
    public static class LevelStruct {
        public Map<pos, Integer> struct;
        public List<List<Block>> Mapping;
        public Recipe recipe;     // 保留但不再使用，由全局 groups 接管
        public pos[] inputs;
        public pos[] outputs;
    }

    public List<LevelStruct> levels = new ArrayList<>();
    public RecipeGroup[] groups = new RecipeGroup[]{};

    public MultiblockStructer(String name) {
        super(name);
        rotate = true;
        update = true;
        solid = true;
        buildType = MultiblockStructerBuilding::new;
        drawer = new DrawDefault();

        configurable = true;
        saveConfig = true;
        copyConfig = true;

        config(Integer.class, (MultiblockStructerBuilding build, Integer groupIdx) -> {
            build.selectedGroup = groupIdx;
            build.currentRecipe = -1;
            build.progress = 0f;
        });
    }

    @Override
    public void load() {
        super.load();
        region = Core.atlas.find(name);
        for (int i = 0; i < groups.length; i++) {
            groups[i].Texture_name = "programming-circuit"+String.valueOf(i+1);
        }
    }

    // 绘制相关保持不变
    @Override
    public void drawBase(Tile tile) {
        Draw.rect(region, tile.worldx(), tile.worldy());
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
        if (drawer != null) {
            drawer.drawPlan(this, plan, list);
        } else {
            Draw.rect(region, plan.drawx(), plan.drawy(), plan.rotation * 90);
        }
    }

    public static class pos {
        public int x, y;
        public pos() {}
        public pos(int x, int y) { this.x = x; this.y = y; }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            pos pos = (pos) o;
            return x == pos.x && y == pos.y;
        }
        @Override
        public int hashCode() { return x * 31 + y; }
    }

    @Override
    public void setBars() {
        super.setBars();

        addBar("level", (MultiblockStructerBuilding build) -> new Bar(
                () -> build.Molded ? "level:" + build.level : Core.bundle.get("bar.unformed", "未成形"),
                () -> Pal.accent,
                () -> 1f
        ));

        addBar("progress", (MultiblockStructerBuilding build) -> new Bar(
                () -> {
                    if (!build.Molded || build.selectedGroup < 0 || build.selectedGroup >= groups.length)
                        return Core.bundle.get("bar.idle", "空闲");
                    RecipeGroup group = groups[build.selectedGroup];
                    String groupName = Core.bundle.get("group." + group.name, group.name);
                    if (build.currentRecipe >= 0 && build.currentRecipe < group.recipes.length) {
                        Recipe r = group.recipes[build.currentRecipe];
                        String itemName = r.outputItem != null ? r.outputItem.item.localizedName : "???";
                        return groupName + " - " + itemName + " " + (int)(build.progress * 100) + "%";
                    }
                    return groupName + " - 空闲";
                },
                () -> Pal.accent,
                () -> build.Molded ? build.progress : 0f
        ));
    }

    public class MultiblockStructerBuilding extends Building {
        public boolean Molded;
        public int level;
        public float progress;
        public int selectedGroup = -1;
        public int currentRecipe = -1;

        // ---------- 结构检测（不变） ----------
        public void CheckStruct() {
            level = 0;
            Molded = false;
            for (int i = 1; i <= levels.size(); i++) {
                LevelStruct now = levels.get(i - 1);
                boolean accept = true;
                for (Map.Entry<pos, Integer> ps : now.struct.entrySet()) {
                    int dx = ps.getKey().x;
                    int dy = ps.getKey().y;
                    Tile checkTile = Vars.world.tile(tile.x + dx, tile.y + dy);
                    if (checkTile == null) { accept = false; break; }
                    Block blockThere = checkTile.block();
                    int typeIndex = ps.getValue();
                    List<Block> allowed = now.Mapping.get(typeIndex);
                    if (allowed == null || !allowed.contains(blockThere)) { accept = false; break; }
                }
                if (accept) { level = i; Molded = true; }
            }
        }

        private LevelStruct currentLevel() {
            return (level > 0 && level <= levels.size()) ? levels.get(level - 1) : null;
        }

        // ---------- 舱室交互（不变） ----------
        private int takeFromInputs(Item item, int amount) {
            LevelStruct lvl = currentLevel();
            if (lvl == null || lvl.inputs == null) return 0;
            int remaining = amount;
            for (pos offset : lvl.inputs) {
                if (remaining <= 0) break;
                Tile t = Vars.world.tile(tile.x + offset.x, tile.y + offset.y);
                if (t != null && t.build != null && t.build.block.hasItems) {
                    int canTake = Math.min(t.build.items.get(item), remaining);
                    t.build.items.remove(item, canTake);
                    remaining -= canTake;
                }
            }
            return amount - remaining;
        }

        private boolean inputsHave(ItemStack[] items) {
            if (items == null || items.length == 0) return true;
            LevelStruct lvl = currentLevel();
            if (lvl == null || lvl.inputs == null) return false;
            Map<Item, Integer> needed = new HashMap<>();
            for (ItemStack stack : items) needed.merge(stack.item, stack.amount, Integer::sum);
            Map<Item, Integer> available = new HashMap<>();
            for (pos offset : lvl.inputs) {
                Tile t = Vars.world.tile(tile.x + offset.x, tile.y + offset.y);
                if (t != null && t.build != null && t.build.block.hasItems) {
                    for (Item item : needed.keySet())
                        available.merge(item, t.build.items.get(item), Integer::sum);
                }
            }
            for (Map.Entry<Item, Integer> entry : needed.entrySet())
                if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) return false;
            return true;
        }

        private int putToOutputs(Item item, int amount) {
            LevelStruct lvl = currentLevel();
            if (lvl == null || lvl.outputs == null) return 0;
            int remaining = amount;
            for (pos offset : lvl.outputs) {
                if (remaining <= 0) break;
                Tile t = Vars.world.tile(tile.x + offset.x, tile.y + offset.y);
                if (t != null && t.build != null && t.build.block.hasItems) {
                    int space = t.build.block.itemCapacity - t.build.items.get(item);
                    int canPut = Math.min(space, remaining);
                    if (canPut > 0) {
                        t.build.items.add(item, canPut);
                        remaining -= canPut;
                    }
                }
            }
            return amount - remaining;
        }

        private boolean outputsFullFor(Item item, int amount) {
            LevelStruct lvl = currentLevel();
            if (lvl == null || lvl.outputs == null) return true;
            int totalSpace = 0;
            for (pos offset : lvl.outputs) {
                Tile t = Vars.world.tile(tile.x + offset.x, tile.y + offset.y);
                if (t != null && t.build != null && t.build.block.hasItems)
                    totalSpace += Math.max(0, t.build.block.itemCapacity - t.build.items.get(item));
            }
            return totalSpace < amount;
        }

        // ---------- 生产逻辑（不变） ----------
        @Override
        public void updateTile() {
            super.updateTile();

            if (timer(0, 60f)) {
                boolean wasMolded = Molded;
                int oldLevel = level;
                CheckStruct();
                if (!Molded || level != oldLevel) {
                    progress = 0f;
                    currentRecipe = -1;
                }
            }

            if (!Molded) return;
            if (groups.length == 0) return;
            if (selectedGroup < 0 || selectedGroup >= groups.length) return;

            Recipe[] activeRecipes = groups[selectedGroup].recipes;
            if (activeRecipes.length == 0) return;

            if (currentRecipe >= 0 && currentRecipe < activeRecipes.length) {
                Recipe active = activeRecipes[currentRecipe];
                if (!inputsHave(active.inputItems) || outputsFullFor(active.outputItem.item, active.outputItem.amount))
                    currentRecipe = -1;
            }

            if (currentRecipe == -1) {
                for (int i = 0; i < activeRecipes.length; i++) {
                    Recipe r = activeRecipes[i];
                    if (inputsHave(r.inputItems) && !outputsFullFor(r.outputItem.item, r.outputItem.amount)) {
                        currentRecipe = i;
                        progress = 0f;
                        break;
                    }
                }
            }

            if (currentRecipe >= 0 && currentRecipe < activeRecipes.length) {
                Recipe active = activeRecipes[currentRecipe];
                progress += (1f / active.craftTime) * delta();

                if (progress >= 1f) {
                    if (!inputsHave(active.inputItems) || outputsFullFor(active.outputItem.item, active.outputItem.amount)) {
                        progress = 0f;
                        currentRecipe = -1;
                        return;
                    }

                    for (ItemStack stack : active.inputItems)
                        takeFromInputs(stack.item, stack.amount);

                    putToOutputs(active.outputItem.item, active.outputItem.amount);

                    progress %= 1f;

                    if (!inputsHave(active.inputItems) || outputsFullFor(active.outputItem.item, active.outputItem.amount))
                        currentRecipe = -1;
                }
            } else {
                progress = 0f;
            }
        }

        // ---------- 配方组选择界面（与 RecipeCrafter 完全一致） ----------
        @Override
        public void buildConfiguration(Table table) {
            table.clear();

            if (groups.length == 0) return;

            TextureRegion errorRegion = Core.atlas.find("error");
            String modName = Vars.mods.getMod(MainMod.class).name;

            for (int i = 0; i < groups.length; i++) {
                final int idx = i;
                RecipeGroup group = groups[i];

                TextureRegion icon = null;

                if (group.Texture_name != null && !group.Texture_name.isEmpty()) {
                    String atlasName = modName + "-" + group.Texture_name;
                    TextureRegion loaded = Core.atlas.find(atlasName);

                    if (loaded != null && loaded != errorRegion) {
                        icon = loaded;
                        Log.info("Group @ loaded custom icon: @", i, atlasName);
                    } else {
                        Log.warn("Group @ failed to load icon '@'; atlas has region: @", i, atlasName, Core.atlas.has(atlasName));
                    }
                }

                if (icon == null && group.recipes != null && group.recipes.length > 0) {
                    Recipe first = group.recipes[0];
                    if (first.outputItem != null) {
                        icon = first.outputItem.item.uiIcon;
                    }
                }

                if (icon == null || icon == errorRegion) {
                    icon = errorRegion;
                }

                group.icon = icon; // 缓存图标

                TextureRegionDrawable drawable = new TextureRegionDrawable(icon);

                ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle(Styles.defaulti);
                style.imageUp = drawable;
                style.imageChecked = drawable;
                style.imageDisabled = drawable;

                ImageButton button = new ImageButton(style);
                button.clicked(() -> configure(idx));
                button.setChecked(idx == selectedGroup);

                table.add(button).size(50f).pad(4f);
                table.add(Core.bundle.get("group." + group.name, group.name)).pad(4f);
                table.row();
            }
        }

        @Override
        public Object config() {
            return selectedGroup;
        }

        // ---------- 存档 ----------
        @Override
        public byte version() {
            return 1;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(selectedGroup);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            if (revision >= 1) selectedGroup = read.i();
        }

        // ---------- 绘制 ----------
        @Override
        public void draw() {
            if (drawer != null) drawer.draw(this);
            else Draw.rect(region, x, y);
        }

        @Override
        public void drawLight() {
            if (drawer != null) drawer.drawLight(this);
        }
    }
}