package mdtnh.modui.buildui;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Tex;
import mindustry.gen.Building;
import mindustry.type.ItemStack;
import mindustry.type.Category;
import mindustry.ui.Styles;
import mindustry.ui.fragments.PlacementFragment;
import mindustry.world.Block;

public final class MdtBuildMenuFragment {
    private static final String entryName = "mdtnh-build-menu-entry";
    private final BuildMenuRegistry registry;
    private BuildMenuNode current;
    private ImageButton entryButton;
    private Table popup;

    private Table hoverInfo;
    private Block hoveredBlock;

    private boolean opened;
    private boolean installed;
    private Block lastSelected;
    private final Vec2 tmp = new Vec2();

    public MdtBuildMenuFragment(BuildMenuRegistry registry) {
        this.registry = registry;
        this.current = registry.root;
    }

    public void install() {
        if (installed) return;
        installed = true;
        buildPopup();
        Events.run(Trigger.update, () -> {
            if (entryButton == null || entryButton.getScene() == null)
                injectEntryButton();
            syncSelectedBlock();
        });
        Core.app.post(this::injectEntryButton);
    }

    private void buildPopup() {
        popup = new Table(Tex.pane);
        popup.touchable = Touchable.enabled;
        popup.visible = false;
        Vars.ui.hudGroup.addChild(popup);

        hoverInfo = new Table(Tex.buttonEdge2);
        hoverInfo.touchable = Touchable.disabled;
        hoverInfo.visible = false;
        Vars.ui.hudGroup.addChild(hoverInfo);

        popup.update(() -> {
            boolean visible = opened && Vars.ui.hudfrag.shown
                    && entryButton != null && entryButton.getScene() != null;

            popup.visible = visible;

            if (visible) {
                updatePopupPosition();
                updateHoverInfoPosition();
            }

            hoverInfo.visible = visible && hoveredBlock != null;
        });
    }

    private void injectEntryButton() {
        if (Vars.ui == null || Vars.ui.hudfrag == null) return;
        Element existing = Core.scene.find(entryName);
        if (existing instanceof ImageButton) {
            entryButton = (ImageButton) existing;
            return;
        }
        Table categoryTable = findVanillaCategoryTable();
        if (categoryTable == null) return;
        if (registry.root.icon == null) return;
        categoryTable.row();
        entryButton = categoryTable.button(registry.root.icon, Styles.clearTogglei, this::toggle).size(50f).get();
        entryButton.name = entryName;
        categoryTable.add().size(50f);
        entryButton.update(() -> entryButton.setChecked(opened));
        Vars.ui.addDescTooltip(entryButton, "MDT 多级建造菜单");
        categoryTable.invalidateHierarchy();
    }

    private Table findVanillaCategoryTable() {
        for (Category category : Category.values()) {
            Element element = Core.scene.find("category-" + category.name());
            if (element == null) continue;
            if (element.parent instanceof Table)
                return (Table) element.parent;
        }
        return null;
    }

    private void toggle() {
        opened = !opened;
        clearHoveredBlock();

        if (opened) {
            rebuildPopup();
            popup.visible = true;
            popup.toFront();
            hoverInfo.toFront();
            updatePopupPosition();
        } else {
            popup.visible = false;
            hoverInfo.visible = false;
        }
    }

    private void rebuildPopup() {
        clearHoveredBlock();

        popup.clearChildren();
        popup.margin(6f);
        buildHeader();
        popup.row();
        popup.pane(this::buildContent).width(300f).maxHeight(380f);
        popup.pack();
    }

    private void buildHeader() {
        popup.table(header -> {
            if (current != registry.root) {
                header.button("<", Styles.cleart, () -> {
                    if (current.parent != null) {
                        current = current.parent;
                        rebuildPopup();
                    }
                }).size(42f);
            } else {
                header.add().size(42f);
            }
            header.add(getBreadcrumb()).left().growX().padLeft(6f).padRight(6f);
            header.button("X", Styles.cleart, () -> opened = false).size(42f);
        }).growX();
    }

    private String getBreadcrumb() {
        if (current == registry.root) return registry.root.title;
        Seq<BuildMenuNode> path = new Seq<>();
        BuildMenuNode node = current;
        while (node != null && node != registry.root) {
            path.insert(0, node);
            node = node.parent;
        }
        StringBuilder builder = new StringBuilder("MDT");
        for (BuildMenuNode item : path) {
            builder.append(" > ").append(item.title);
        }
        return builder.toString();
    }

    private void buildContent(Table table) {
        table.top().left();
        buildChildren(table);
        if (hasAvailableBlocks(current) && hasAvailableChildren(current)) {
            table.add().height(5f);
            table.row();
        }
        buildBlocks(table);
        if (table.getChildren().isEmpty()) {
            table.add("（该分类下暂无可用方块）").pad(10f);
        }
    }

    private void buildChildren(Table table) {
        for (BuildMenuNode child : current.children) {
            if (!hasAvailableContent(child)) continue;
            table.button(child.title + "  >", Styles.cleart, () -> {
                current = child;
                rebuildPopup();
            }).height(44f).growX();
            table.row();
        }
    }

    private void buildBlocks(Table table) {
        Table grid = new Table();
        int index = 0;
        for (Block block : current.blocks) {
            if (!available(block)) continue;
            if (index > 0 && index % 4 == 0) grid.row();
            ImageButton button = grid.button(
                    new TextureRegionDrawable(block.uiIcon), Styles.selecti, () -> select(block)
            ).size(56f).get();
            button.resizeImage(40f);
            button.update(() -> button.setChecked(Vars.control.input.block == block));

            button.hovered(() -> setHoveredBlock(block));
            button.exited(() -> {
                if (hoveredBlock == block) clearHoveredBlock();
            });

            index++;
        }
        int rest = index % 4;
        if (rest != 0) {
            for (int i = rest; i < 4; i++) grid.add().size(56f);
        }
        table.add(grid).left();
    }

    private void setHoveredBlock(Block block) {
        if (block == null || hoveredBlock == block) return;
        hoveredBlock = block;
        rebuildHoverInfo(block);
        hoverInfo.visible = opened && popup.visible;
        hoverInfo.toFront();
        updateHoverInfoPosition();
    }

    private void clearHoveredBlock() {
        hoveredBlock = null;
        if (hoverInfo != null) {
            hoverInfo.visible = false;
            hoverInfo.clearChildren();
        }
    }

    private void rebuildHoverInfo(Block block) {
        hoverInfo.clearChildren();
        hoverInfo.top().left();
        hoverInfo.margin(6f);

        hoverInfo.table(header -> {
            header.left();
            header.image(block.uiIcon).size(40f).padRight(8f);
            header.add(block.localizedName).left().growX();
        }).growX().left();

        hoverInfo.row();

        hoverInfo.table(costs -> {
            costs.top().left();
            costs.add("[lightgray]建造花费[]").left().padBottom(3f);
            costs.row();

            if (block.requirements == null || block.requirements.length == 0) {
                costs.add("无").left().padLeft(2f);
                return;
            }

            for (ItemStack stack : block.requirements) {
                int required = Math.round(stack.amount * Vars.state.rules.buildCostMultiplier);
                costs.table(line -> {
                    line.left();
                    line.image(stack.item.uiIcon).size(20f).padRight(4f);
                    line.add(stack.item.localizedName).left().width(150f).get().setEllipsis(true);
                    line.label(() -> formatRequirement(stack, required)).right().padLeft(6f);
                }).growX().left();
                costs.row();
            }
        }).growX().left().padTop(4f);

        hoverInfo.pack();
    }

    private String formatRequirement(ItemStack stack, int required) {
        Building core = Vars.player == null ? null : Vars.player.core();
        if (core == null || Vars.state.rules.infiniteResources) {
            return "[white]" + required;
        }
        int amount = core.items.get(stack.item);
        String color = amount < required / 2f ? "[scarlet]"
                : amount < required ? "[accent]"
                : "[white]";
        return color + amount + "[white]/" + required;
    }

    private void updateHoverInfoPosition() {
        if (hoverInfo == null || popup == null || hoveredBlock == null) return;
        float desiredWidth = Math.max(popup.getWidth(), hoverInfo.getPrefWidth());
        hoverInfo.setWidth(desiredWidth);
        hoverInfo.invalidate();

        float x = popup.x;
        float y = popup.y + popup.getHeight() + 6f;
        float maxX = Math.max(0f, Vars.ui.hudGroup.getWidth() - hoverInfo.getWidth());
        float maxY = Math.max(0f, Vars.ui.hudGroup.getHeight() - hoverInfo.getHeight());

        x = Mathf.clamp(x, 0f, maxX);
        y = Mathf.clamp(y, 0f, maxY);
        hoverInfo.setPosition(x, y);
    }

    private void select(Block block) {
        if (!available(block)) return;
        if (Vars.control.input.block == block) {
            Vars.control.input.block = null;
            opened = false;
            clearHoveredBlock();
            return;
        }
        opened = false;
        clearHoveredBlock();
        Core.app.post(() -> {
            if (Vars.ui == null || Vars.ui.hudfrag == null) return;
            PlacementFragment placement = Vars.ui.hudfrag.blockfrag;
            Vars.control.input.block = block;
            if (placement.currentCategory != block.category) {
                placement.currentCategory = block.category;
                placement.rebuild();
            }
        });
    }

    private void syncSelectedBlock() {
        Block selected = Vars.control.input.block;
        if (selected == lastSelected) return;
        lastSelected = selected;
        if (selected == null) return;
        BuildMenuNode node = registry.primaryNode(selected);
        if (node == null || node == current) return;
        current = node;
        if (opened) rebuildPopup();
    }

    private boolean available(Block block) {
        if (block == null) return false;
        return block.isVisible()
                && block.unlockedNowHost()
                && block.placeablePlayer
                && block.environmentBuildable()
                && block.supportsEnv(Vars.state.rules.env);
    }

    private boolean hasAvailableBlocks(BuildMenuNode node) {
        for (Block block : node.blocks) if (available(block)) return true;
        return false;
    }

    private boolean hasAvailableChildren(BuildMenuNode node) {
        for (BuildMenuNode child : node.children) if (hasAvailableContent(child)) return true;
        return false;
    }

    private boolean hasAvailableContent(BuildMenuNode node) {
        if (hasAvailableBlocks(node)) return true;
        for (BuildMenuNode child : node.children) if (hasAvailableContent(child)) return true;
        return false;
    }

    private void updatePopupPosition() {
        if (entryButton == null || entryButton.getScene() == null) return;
        tmp.set(0f, 0f);
        entryButton.localToStageCoordinates(tmp);
        Vars.ui.hudGroup.stageToLocalCoordinates(tmp);
        float width = popup.getWidth();
        float height = popup.getHeight();
        float x = tmp.x - width - 8f;
        float y = tmp.y;
        x = Mathf.clamp(x, 0f, Math.max(0f, Vars.ui.hudGroup.getWidth() - width));
        y = Mathf.clamp(y, 0f, Math.max(0f, Vars.ui.hudGroup.getHeight() - height));
        popup.setPosition(x, y);
    }
}