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

/**
 * MDT 多级建造菜单的 HUD 控制器。
 *
 * <p>该类读取 {@link BuildMenuRegistry} 中的树形数据，将入口按钮注入原版建造分类栏，
 * 并负责分类导航、方块选择、悬浮信息和弹窗定位。它不创建任何游戏内容。</p>
 *
 * <p>界面由三部分组成：原版分类栏中的 MDT 入口按钮、分类/方块弹窗、
 * 以及鼠标悬停方块时显示的建造花费面板。</p>
 */
public final class MdtBuildMenuFragment {

    /** 注入场景树时使用的唯一名称，用于检测并复用已有入口按钮。 */
    private static final String entryName = "mdtnh-build-menu-entry";

    /** 菜单数据来源。 */
    private final BuildMenuRegistry registry;

    /** 当前正在浏览的分类节点。 */
    private BuildMenuNode current;

    /** 注入原版建造分类栏的 MDT 入口按钮。 */
    private ImageButton entryButton;

    /** 分类和方块主体弹窗。 */
    private Table popup;

    /** 当前悬停方块的详情面板。 */
    private Table hoverInfo;

    /** 当前鼠标悬停方块；没有悬停对象时为 {@code null}。 */
    private Block hoveredBlock;

    /** 菜单是否处于打开状态。 */
    private boolean opened;

    /** 防止重复安装 HUD 元素和 update 回调。 */
    private boolean installed;

    /** 上一帧原版建造系统选中的方块，用于检测选择变化。 */
    private Block lastSelected;

    /** 坐标换算复用向量，避免位置更新时频繁创建临时对象。 */
    private final Vec2 tmp = new Vec2();

    /**
     * 创建菜单控制器，并从注册表根节点开始浏览。
     *
     * @param registry 已完成内容注册的菜单注册表
     */
    public MdtBuildMenuFragment(BuildMenuRegistry registry) {
        this.registry = registry;
        this.current = registry.root;
    }

    /**
     * 将 MDT 菜单安装到客户端 HUD。
     *
     * <p>安装后每帧确认入口按钮仍在场景中；如果原版 HUD 重建导致按钮消失，
     * 会自动重新注入。同时同步原版当前选中方块与 MDT 菜单分类。</p>
     */
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

    /**
     * 创建主弹窗和悬浮信息面板，并挂到 HUD 根容器。
     *
     * <p>这里只建立容器与可见性更新逻辑，具体分类内容在打开菜单时生成。</p>
     */
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

    /**
     * 在原版建造分类栏中注入 MDT 入口按钮。
     *
     * <p>先按唯一名称寻找已有元素以避免重复创建；若不存在，
     * 则定位原版分类按钮的父表格并在末尾追加 MDT 按钮。</p>
     */
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

    /**
     * 定位 Mindustry 原版建造分类按钮所在表格。
     *
     * @return 分类表格；HUD 尚未构建完成时返回 {@code null}
     */
    private Table findVanillaCategoryTable() {
        for (Category category : Category.values()) {
            Element element = Core.scene.find("category-" + category.name());
            if (element == null) continue;
            if (element.parent instanceof Table)
                return (Table) element.parent;
        }
        return null;
    }

    /**
     * 切换菜单打开状态。
     *
     * <p>打开时重建当前分类并把弹窗置于前景；关闭时同时隐藏悬浮信息。</p>
     */
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

    /**
     * 按当前分类重新生成整个菜单弹窗。
     *
     * <p>标题区提供返回上级、面包屑路径和关闭按钮；主体使用滚动面板，
     * 避免内容过多时超出屏幕。</p>
     */
    private void rebuildPopup() {
        clearHoveredBlock();

        popup.clearChildren();
        popup.margin(6f);
        buildHeader();
        popup.row();
        popup.pane(this::buildContent).width(300f).maxHeight(380f);
        popup.pack();
    }

    /** 构建返回按钮、面包屑路径和关闭按钮组成的标题栏。 */
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

    /**
     * 从当前节点向根节点回溯，生成“MDT > 分类 > 子分类”形式的路径文本。
     */
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

    /**
     * 构建当前分类主体内容。
     *
     * <p>先显示仍有可用内容的子分类，再显示当前分类直接包含的方块。
     * 如果两者都为空，则显示空分类提示。</p>
     */
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

    /** 将当前节点中包含可用内容的子分类生成为纵向导航按钮。 */
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

    /**
     * 将当前分类中的可用方块按每行四个排列。
     *
     * <p>按钮勾选状态与原版放置系统同步；悬停按钮时刷新建造花费详情。</p>
     */
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

    /** 设置当前悬停方块，并根据该方块刷新详情面板。 */
    private void setHoveredBlock(Block block) {
        if (block == null || hoveredBlock == block) return;
        hoveredBlock = block;
        rebuildHoverInfo(block);
        hoverInfo.visible = opened && popup.visible;
        hoverInfo.toFront();
        updateHoverInfoPosition();
    }

    /** 清空悬停状态，同时隐藏并清空详情面板。 */
    private void clearHoveredBlock() {
        hoveredBlock = null;
        if (hoverInfo != null) {
            hoverInfo.visible = false;
            hoverInfo.clearChildren();
        }
    }

    /**
     * 根据方块重建悬浮信息。
     *
     * <p>顶部显示方块图标和名称，下方逐项列出受当前建造成本倍率影响后的资源需求。</p>
     */
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

    /**
     * 格式化单项建造材料的库存与需求量。
     *
     * <p>库存不足一半使用红色；不足需求但达到一半使用强调色；
     * 数量充足使用白色。无限资源规则下只显示需求量。</p>
     */
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

    /** 将悬浮详情放在主弹窗上方，并限制在 HUD 可见边界内。 */
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

    /**
     * 将方块交给 Mindustry 原版放置系统。
     *
     * <p>再次点击已选方块会取消选择。选择新方块时关闭 MDT 菜单，
     * 再在下一帧同步原版建造分类与输入状态。</p>
     */
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

    /**
     * 根据原版当前选中方块同步 MDT 菜单所在分类。
     *
     * <p>通过注册表反向索引查找方块的主要分类；菜单打开时会立即刷新内容。</p>
     */
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

    /**
     * 判断方块在当前游戏状态下是否应显示。
     *
     * <p>同时检查内容可见性、科技解锁、玩家可建造标记、环境限制和当前规则环境。</p>
     */
    private boolean available(Block block) {
        if (block == null) return false;
        return block.isVisible()
                && block.unlockedNowHost()
                && block.placeablePlayer
                && block.environmentBuildable()
                && block.supportsEnv(Vars.state.rules.env);
    }

    /** @return 当前节点是否至少包含一个可显示方块。 */
    private boolean hasAvailableBlocks(BuildMenuNode node) {
        for (Block block : node.blocks) if (available(block)) return true;
        return false;
    }

    /** @return 当前节点是否至少包含一个有可显示内容的子分类。 */
    private boolean hasAvailableChildren(BuildMenuNode node) {
        for (BuildMenuNode child : node.children) if (hasAvailableContent(child)) return true;
        return false;
    }

    /**
     * 递归判断节点本身或任意后代是否存在可显示内容。
     *
     * <p>用于隐藏完全空的分类，避免玩家进入没有任何可建造内容的层级。</p>
     */
    private boolean hasAvailableContent(BuildMenuNode node) {
        if (hasAvailableBlocks(node)) return true;
        for (BuildMenuNode child : node.children) if (hasAvailableContent(child)) return true;
        return false;
    }

    /** 根据入口按钮位置计算主弹窗坐标，并将结果限制在 HUD 边界内。 */
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