package mdtnh.modui.itemui;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.input.KeyBind;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Log;

import mindustry.Vars;
import mindustry.content.Items;
import mindustry.core.UI;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustry.type.Item;
import mindustry.ui.CoreItemsDisplay;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.nio.charset.StandardCharsets;

import static mindustry.Vars.content;
import static mindustry.Vars.player;

/**
 * MDT 核心物品快捷显示。
 *
 * 功能：
 *
 * 1. 接管原版 HudFragment.coreItems 的位置；
 * 2. 原位置只显示用户选择的快捷物品；
 * 3. 点击菜单按钮打开二级菜单；
 * 4. 二级菜单显示原版与模组物品；
 * 5. 用户可以自由添加/删除快捷显示物品；
 * 6. 选择结果保存在 Core.settings 中；
 * 7. 核心库存数量实时刷新；
 * 8. 支持按数量升/降序；
 * 9. 支持名称、内部名、全拼与拼音首字母搜索。
 */
public class MdtCoreItemsQuickBar {

    /** Core.settings 中保存快捷物品的 key。 */
    private static final String settingsKey =
            "mdtnh-core-item-quickrod";

    /**
     * 原生 Arc KeyBind。
     * 默认 I 键打开详情页，可在“设置 -> 控制 -> MDTNH”中修改。
     */
    public static final KeyBind openDetailsKey =
            KeyBind.add("mdtnh_item_details", KeyCode.i, "mdtnh");

    /**
     * 在 MainMod 构造函数最开始调用，用于尽早完成快捷键注册。
     */
    public static void registerKeybind() {
        // 调用此方法时类已经完成静态初始化。
    }

    /**
     * 快捷栏最大高度。
     *
     * 横向空间不够时使用 ScrollPane 横向滚动，
     * 不会把 HUD 撑得特别宽。
     */
    private static final float itemSize = 36f;


    /** 保存详情页排序方式。 */
    private static final String sortSettingsKey =
            "mdtnh-core-item-sort-mode";

    /** 搜索文本；关闭再打开详情页时保留。 */
    private String searchText = "";

    /** 当前排序方式。 */
    private SortMode sortMode = SortMode.defaultOrder;

    /** 详情页物品网格，搜索时只重建这里，不重建整个 Dialog。 */
    private Table dialogItemTable;

    /** 搜索结果数量提示。 */
    private Label resultCountLabel;

    /** 搜索框引用，供“清除”按钮使用。 */
    private TextField searchField;

    /** 每个物品的搜索索引缓存，避免每次按键都重新做拼音转换。 */
    private final ObjectMap<Item, String> searchIndexCache = new ObjectMap<>();

    /**
     * 拼音输出格式：
     * - 小写；
     * - 不带声调；
     * - ü 输出为 v，方便键盘输入。
     */
    private static final HanyuPinyinOutputFormat pinyinFormat =
            new HanyuPinyinOutputFormat();

    static {
        pinyinFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        pinyinFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        pinyinFormat.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    private enum SortMode {
        defaultOrder,
        amountDesc,
        amountAsc
    }

    /**
     * 当前加入顶部快捷栏的物品名称集合。
     *
     * <p>保存 {@link Item#name} 而不是 Item 实例，便于持久化并在重启后恢复。</p>
     */
    private final ObjectSet<String> selected = new ObjectSet<>();

    /** 原版 HudFragment 的 CoreItemsDisplay。 */
    private CoreItemsDisplay host;

    /** 我们实际插进去的 UI。 */
    private Table root;

    /** 快捷物品所在 Table。 */
    private Table quickItems;

    /** 顶部横向滚动区域。 */
    private ScrollPane quickPane;

    /** 选择物品的二级菜单。 */
    private BaseDialog dialog;

    /** 是否已经安装。 */
    private boolean installed;

    public void install() {
        if (installed) return;

        if (Vars.ui == null || Vars.ui.hudfrag == null) {
            Log.err("MDT quick item rod: HUD is not ready.");
            return;
        }

        host = Vars.ui.hudfrag.coreItems;

        if (host == null) {
            Log.err("MDT quick item rod: coreItems is null.");
            return;
        }

        loadSettings();
        buildDialog();
        buildRoot();

        /*
         * 原版 HudFragment 已经把 coreItems 放在核心物品 HUD 的正确位置。
         * 直接替换其内部内容即可继续沿用原版布局、位置和显隐逻辑。
         */
        host.clear();
        host.add(root);

        /*
         * HUD 重建时原版可能重新生成 coreItems 内容。
         * 每帧仅检查子元素引用；发现内容被替换时重新挂载快捷栏。
         */
        host.update(() -> {
            if (root == null) return;

            if (host.getChildren().size != 1 ||
                    host.getChildren().first() != root) {

                host.clearChildren();
                host.add(root);
            }
        });

        installed = true;

        /*
         * 原生快捷键监听。
         * 只在游戏内、没有文本输入框、没有其它对话框时打开详情页。
         */
        Events.run(Trigger.update, () -> {
            if (!installed ||
                    dialog == null ||
                    Core.input == null ||
                    Core.scene == null ||
                    Vars.state == null ||
                    Vars.state.isMenu()) {

                return;
            }

            if (Core.scene.hasField() || Core.scene.hasDialog()) {
                return;
            }

            if (Core.input.keyTap(openDetailsKey)) {
                showDialog();
            }
        });

        Log.info(
                "MDT core item quick rod installed. Selected items: @",
                selected.size
        );
    }

    /**
     * 创建顶部快捷栏。
     */
    private void buildRoot() {
        root = new Table();

        root.top();
        root.left();

        /*
         * 使用 black6 接近原版 HUD 风格。
         */
        root.background(Styles.black6);

        /*
         * 左侧：打开二级菜单。
         */
        root.button(
                        Icon.list,
                        Styles.clearNonei,
                        this::showDialog
                ).size(42f)
                .tooltip("物品快捷显示");

        /*
         * 中间：用户选择的快捷物品。
         */
        quickItems = new Table();
        quickItems.left();

        quickPane = new ScrollPane(
                quickItems,
                Styles.smallPane
        );

        quickPane.setScrollingDisabled(false, true);
        quickPane.setFadeScrollBars(true);
        quickPane.setOverscroll(false, false);

        /*
         * Arc Scene 会把滚轮事件优先发送给 scrollFocus。
         * ScrollPane 获得 scrollFocus 后，如果鼠标离开时焦点没有释放，
         * 相机缩放就收不到滚轮事件。
         *
         * 每帧检查鼠标是否已经离开顶部快捷栏；
         * 离开后主动释放属于本 ScrollPane 的 scrollFocus。
         */
        quickPane.update(this::releaseQuickPaneScrollFocus);

        root.add(quickPane)
                .maxWidth(600f)
                .height(46f);

        rebuildQuickItems();
    }

    /**
     * 鼠标离开顶部快捷栏后释放滚轮焦点，
     * 避免阻止游戏相机缩放。
     */
    private void releaseQuickPaneScrollFocus() {
        if (quickPane == null || Core.scene == null) {
            return;
        }

        Element focus = Core.scene.getScrollFocus();

        boolean focusBelongsToQuickPane =
                focus == quickPane ||
                        (focus != null && focus.isDescendantOf(quickPane));

        if (!focusBelongsToQuickPane) {
            return;
        }

        Element hover = Core.scene.getHoverElement();

        boolean mouseInsideQuickPane =
                hover == quickPane ||
                        (hover != null && hover.isDescendantOf(quickPane));

        if (!mouseInsideQuickPane) {
            Core.scene.setScrollFocus(null);
        }
    }

    /**
     * 重建顶部快捷物品列表。
     *
     * 只有用户改变选择时才 rebuild，
     * 物品数量本身使用 Label provider 动态获取，
     * 所以不用每帧 rebuild。
     */
    private void rebuildQuickItems() {
        quickItems.clear();

        Seq<Item> items = getSelectedItems();

        if (items.isEmpty()) {
            quickItems.button(
                    "选择物品",
                    Icon.add,
                    Styles.flatt,
                    this::showDialog
            ).height(itemSize);

            return;
        }

        for (Item item : items) {
            addQuickItem(quickItems, item);
        }
    }

    /**
     * 添加一个顶部快捷物品。
     */
    private void addQuickItem(Table table, Item item) {

        Table entry = new Table();

        /*
         * 图标。
         */
        Image image = new Image(item.uiIcon);

        entry.add(image)
                .size(24f)
                .padLeft(5f)
                .padRight(3f);

        /*
         * 数量。
         *
         * player.team().items() 是当前队伍核心库存，
         * 用 provider 每帧更新文本即可。
         */
        Label amount = new Label(() ->
                getFormattedAmount(item)
        );

        amount.setColor(Color.white);

        entry.add(amount)
                .padRight(6f);

        // 点击快捷物品打开统一的物品显示设置页。
        entry.clicked(this::showDialog);

        table.add(entry)
                .height(itemSize)
                .tooltip(item.localizedName);
    }

    /**
     * 创建二级物品菜单。
     */
    private void buildDialog() {
        dialog = new BaseDialog("物品显示设置");

        /*
         * 关闭按钮。
         */
        dialog.addCloseButton();

        /*
         * 重置为默认值。
         */
        dialog.buttons.button(
                "恢复默认",
                Icon.refresh,
                () -> {
                    resetDefaults();
                    rebuildDialogItems();
                    rebuildQuickItems();
                }
        );

        /*
         * 直接打开 Mindustry 原版“控制”页面，
         * 用户可在 MDTNH 分类中修改详情页快捷键。
         */
        dialog.buttons.button(
                "快捷键设置",
                Icon.settings,
                () -> {
                    dialog.hide();

                    Core.app.post(() -> {
                        if (Vars.ui != null && Vars.ui.controls != null) {
                            Vars.ui.controls.show();
                        }
                    });
                }
        );

        rebuildDialogItems();
    }

    /**
     * 重建二级菜单。
     */
    private void rebuildDialogItems() {
        if (dialog == null) return;

        dialog.cont.clear();

        dialog.cont.label(() ->
                "[lightgray]选择需要显示在顶部快捷栏中的物品。[]\n" +
                        "[lightgray]打开详情快捷键：[accent]" +
                        getShortcutName() +
                        "[]"
        ).pad(10f);

        dialog.cont.row();

        /*
         * 搜索栏。
         *
         * 支持：
         * - 本地化名称，例如“巨浪合金”；
         * - content 内部名，例如“surge-alloy”；
         * - 全拼，例如“julanghejin”；
         * - 拼音首字母，例如“jlhj”。
         *
         * changed() 时只 rebuild 下面的物品网格，
         * 因此输入焦点不会因为整个 Dialog 被重建而丢失。
         */
        dialog.cont.table(search -> {
                    search.left();

                    search.add("搜索：")
                            .padRight(6f);

                    searchField = new TextField(searchText);
                    searchField.setMessageText("名称 / 拼音 / 首字母");

                    searchField.changed(() -> {
                        searchText = searchField.getText();
                        rebuildItemTable();
                    });

                    search.add(searchField)
                            .width(330f)
                            .height(42f);

                    search.button(
                                    "清除",
                                    Styles.flatt,
                                    () -> {
                                        searchText = "";
                                        searchField.setText("");
                                        rebuildItemTable();

                                        if (Core.scene != null) {
                                            Core.scene.setKeyboardFocus(searchField);
                                        }
                                    }
                            ).height(42f)
                            .padLeft(6f);

                }).growX()
                .padLeft(10f)
                .padRight(10f)
                .padBottom(6f);

        dialog.cont.row();

        /*
         * 选择与排序控制。
         */
        dialog.cont.table(actions -> {

            actions.button(
                    "全部选择",
                    Icon.ok,
                    Styles.flatt,
                    () -> {
                        for (Item item : getAvailableItems()) {
                            selected.add(item.name);
                        }

                        saveSettings();
                        rebuildQuickItems();
                        rebuildItemTable();
                    }
            ).growX().row();

            actions.button(
                    "全部清除",
                    Icon.cancel,
                    Styles.flatt,
                    () -> {
                        selected.clear();

                        saveSettings();
                        rebuildQuickItems();
                        rebuildItemTable();
                    }
            ).growX().row();

            actions.button(
                    getSortButtonText(),
                    Icon.settings,
                    Styles.flatt,
                    () -> {
                        cycleSortMode();

                        /*
                         * 排序按钮文字也要变化，因此这里重建整个详情区。
                         * searchText 会被保留。
                         */
                        rebuildDialogItems();
                    }
            ).growX().row();

        }).growX().padBottom(5f);

        dialog.cont.row();

        resultCountLabel = new Label("");
        resultCountLabel.setColor(Color.lightGray);

        dialog.cont.add(resultCountLabel)
                .left()
                .padLeft(12f)
                .padBottom(4f);

        dialog.cont.row();

        /*
         * 实际物品列表放 ScrollPane。
         */
        dialogItemTable = new Table();
        dialogItemTable.top();
        dialogItemTable.left();

        ScrollPane pane =
                new ScrollPane(dialogItemTable, Styles.smallPane);

        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);

        dialog.cont.add(pane)
                .width(520f)
                .maxHeight(500f)
                .growY();

        rebuildItemTable();
    }

    /**
     * 只重建物品网格。
     * 搜索输入时不重建整个 Dialog，因此 TextField 不会丢焦点。
     */
    private void rebuildItemTable() {
        if (dialogItemTable == null) return;

        dialogItemTable.clear();

        Seq<Item> visible = getFilteredAndSortedItems();

        if (resultCountLabel != null) {
            resultCountLabel.setText(
                    "显示 " + visible.size +
                            " / " + getAvailableItems().size +
                            " 个物品"
            );
        }

        buildItemSelection(dialogItemTable, visible);
    }

    /**
     * 二级菜单中的物品网格。
     */
    private void buildItemSelection(Table table, Seq<Item> items) {
        table.top();
        table.left();

        if (items.isEmpty()) {
            table.add(
                            "[lightgray]没有找到匹配的物品。\n" +
                                    "可尝试名称、内部名、全拼或拼音首字母。[]"
                    )
                    .width(480f)
                    .pad(30f);

            return;
        }

        int columns = 4;
        int index = 0;

        for (Item item : items) {

            boolean checked =
                    selected.contains(item.name);

            table.button(
                            button -> {

                                button.left();

                                button.image(item.uiIcon)
                                        .size(28f)
                                        .padRight(6f);

                                /*
                                 * 中间两行：
                                 * 第一行物品名称；
                                 * 第二行实时核心库存数量。
                                 */
                                button.table(info -> {
                                    info.left();

                                    info.add(item.localizedName)
                                            .left();

                                    info.row();

                                    info.label(() ->
                                            "[lightgray]" +
                                                    getFormattedAmount(item) +
                                                    "[]"
                                    ).left();
                                }).left().growX();

                                /*
                                 * 右边显示是否加入快捷栏。
                                 */
                                button.label(() ->
                                        selected.contains(item.name)
                                                ? "[accent]✓[]"
                                                : "[gray]○[]"
                                ).padLeft(4f);

                            },
                            Styles.flatTogglet,
                            () -> toggle(item)
                    )
                    .checked(checked)
                    .width(122f)
                    .height(56f)
                    .pad(2f);

            index++;

            if (index % columns == 0) {
                table.row();
            }
        }
    }

    /**
     * 用户点击二级菜单物品。
     */
    private void toggle(Item item) {
        if (selected.contains(item.name)) {
            selected.remove(item.name);
        } else {
            selected.add(item.name);
        }

        saveSettings();

        /*
         * 顶部立刻生效。
         */
        rebuildQuickItems();

        /*
         * Button 自己会切换 checked 状态，右侧 ✓ / ○ 又是动态 Label，
         * 因此这里不重建整个 Dialog，避免搜索框与滚动位置跳动。
         */
    }

    /**
     * 打开二级菜单。
     */
    public void showDialog() {
        if (dialog == null) {
            buildDialog();
        }

        rebuildDialogItems();

        dialog.show();
    }

    /**
     * 获取当前队伍核心中某个物品的数量。
     */
    private int getAmount(Item item) {
        if (item == null ||
                Vars.state == null ||
                Vars.state.isMenu() ||
                player == null) {

            return 0;
        }

        return player.team().items().get(item);
    }

    /**
     * 使用 Mindustry 原版数量格式，例如 1.2k。
     */
    private String getFormattedAmount(Item item) {
        return UI.formatAmount(
                getAmount(item)
        );
    }

    /**
     * 当前详情页快捷键的人类可读名称。
     */
    private String getShortcutName() {
        if (openDetailsKey.value == null ||
                openDetailsKey.value.key == null) {

            return "未设置";
        }

        return openDetailsKey.value.key.getName();
    }

    /**
     * 根据搜索词筛选，并按照当前模式排序。
     */
    private Seq<Item> getFilteredAndSortedItems() {
        Seq<Item> result = new Seq<>();
        String query = normalizeSearch(searchText);

        for (Item item : getAvailableItems()) {
            if (query.isEmpty() || matchesSearch(item, query)) {
                result.add(item);
            }
        }

        if (sortMode == SortMode.amountDesc) {
            result.sort((a, b) -> {
                int amountCompare =
                        Integer.compare(getAmount(b), getAmount(a));

                if (amountCompare != 0) {
                    return amountCompare;
                }

                return Integer.compare(a.id, b.id);
            });

        } else if (sortMode == SortMode.amountAsc) {
            result.sort((a, b) -> {
                int amountCompare =
                        Integer.compare(getAmount(a), getAmount(b));

                if (amountCompare != 0) {
                    return amountCompare;
                }

                return Integer.compare(a.id, b.id);
            });

        } else {
            result.sort(item -> item.id);
        }

        return result;
    }

    /**
     * 名称搜索：
     * 1. 本地化名称；
     * 2. content 内部名称；
     * 3. 中文名称的全拼；
     * 4. 中文名称的拼音首字母。
     */
    private boolean matchesSearch(Item item, String normalizedQuery) {
        if (item == null) return false;

        String index = searchIndexCache.get(item);

        if (index == null) {
            index = buildSearchIndex(item);
            searchIndexCache.put(item, index);
        }

        return index.contains(normalizedQuery);
    }

    /**
     * 为一个 Item 建立一次搜索索引。
     */
    private String buildSearchIndex(Item item) {
        String localized =
                item.localizedName == null ? "" : item.localizedName;

        String internal =
                item.name == null ? "" : item.name;

        StringBuilder index = new StringBuilder();

        index.append(normalizeSearch(localized))
                .append(' ');

        index.append(normalizeSearch(internal))
                .append(' ');

        /*
         * 拼音索引同时包含全拼和首字母。
         */
        String[] pinyin = toPinyin(localized);

        index.append(normalizeSearch(pinyin[0]))
                .append(' ');

        index.append(normalizeSearch(pinyin[1]));

        return index.toString();
    }

    /**
     * 返回：
     * [0] 全拼
     * [1] 首字母
     *
     * 示例：
     * 巨浪合金 -> julanghejin / jlhj
     */
    private String[] toPinyin(String text) {
        if (text == null || text.isEmpty()) {
            return new String[]{"", ""};
        }

        StringBuilder full = new StringBuilder();
        StringBuilder initials = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            try {
                String[] values =
                        PinyinHelper.toHanyuPinyinStringArray(
                                c,
                                pinyinFormat
                        );

                if (values != null &&
                        values.length > 0 &&
                        values[0] != null &&
                        !values[0].isEmpty()) {

                    /*
                     * pinyin4j 对多音字可能返回多个读音。
                     * 这里使用第一个常用读音建立连续全拼索引。
                     */
                    String value =
                            values[0].toLowerCase();

                    full.append(value);
                    initials.append(value.charAt(0));

                    continue;
                }

            } catch (BadHanyuPinyinOutputFormatCombination ignored) {
                /*
                 * 当前固定格式正常情况下不会触发。
                 * 出错时退回原字符搜索。
                 */
            }

            if (Character.isLetterOrDigit(c)) {
                char lower = Character.toLowerCase(c);

                full.append(lower);
                initials.append(lower);
            }
        }

        return new String[]{
                full.toString(),
                initials.toString()
        };
    }

    /**
     * 搜索时忽略大小写、空格、横线和下划线。
     *
     * 因此：
     * iron-ingot / iron_ingot / iron ingot
     * 都可以使用 ironingot 搜索。
     */
    private String normalizeSearch(String value) {
        if (value == null) return "";

        return value
                .toLowerCase()
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
                .replace("·", "")
                .trim();
    }

    private String getSortButtonText() {
        if (sortMode == SortMode.amountDesc) {
            return "数量 ↓";
        }

        if (sortMode == SortMode.amountAsc) {
            return "数量 ↑";
        }

        return "默认顺序";
    }

    /**
     * 默认 -> 数量降序 -> 数量升序 -> 默认
     */
    private void cycleSortMode() {
        if (sortMode == SortMode.defaultOrder) {
            sortMode = SortMode.amountDesc;

        } else if (sortMode == SortMode.amountDesc) {
            sortMode = SortMode.amountAsc;

        } else {
            sortMode = SortMode.defaultOrder;
        }

        Core.settings.put(
                sortSettingsKey,
                sortMode.ordinal()
        );

        Core.settings.saveValues();
    }

    /**
     * 获取当前内容注册表中的全部物品。
     *
     * <p>不按模组来源过滤，因此原版物品和所有已加载模组物品
     * 都可以进入详情页并加入顶部快捷栏。</p>
     */
    private Seq<Item> getAvailableItems() {
        Seq<Item> result = new Seq<>();

        for (Item item : content.items()) {
            result.add(item);
        }

        /*
         * 按 content ID 排序，
         * 与 Mindustry 原版注册顺序基本保持一致。
         */
        result.sort(item -> item.id);

        return result;
    }

    /**
     * 根据玩家保存的配置取得快捷物品。
     */
    private Seq<Item> getSelectedItems() {
        Seq<Item> result = new Seq<>();

        for (Item item : content.items()) {
            if (selected.contains(item.name)) {
                result.add(item);
            }
        }

        /*
         * 保持 content 顺序，
         * 避免 HashSet 导致显示顺序随机变化。
         */
        result.sort(item -> item.id);

        return result;
    }

    /**
     * 从 Core.settings 读取。
     */
    private void loadSettings() {
        selected.clear();

        int savedSort =
                Core.settings.getInt(
                        sortSettingsKey,
                        SortMode.defaultOrder.ordinal()
                );

        if (savedSort >= 0 &&
                savedSort < SortMode.values().length) {

            sortMode = SortMode.values()[savedSort];

        } else {
            sortMode = SortMode.defaultOrder;
        }

        Object rawValue = Core.settings.get(settingsKey, null);
        String value;

        if (rawValue instanceof byte[]) {
            value = new String((byte[])rawValue, StandardCharsets.UTF_8);
        } else if (rawValue instanceof String) {
            value = (String)rawValue;
        } else {
            value = "";
        }

        if (value.isEmpty()) {
            resetDefaults();
            return;
        }

        String[] names = value.split(",");

        for (String name : names) {
            if (name == null || name.isEmpty()) {
                continue;
            }

            /*
             * 确保旧配置对应的物品仍然存在。
             */
            Item item = content.item(name);

            if (item != null) {
                selected.add(item.name);
            }
        }

        /*
         * 配置损坏或版本升级导致一个都找不到，
         * 回退到默认配置。
         */
        if (selected.isEmpty()) {
            resetDefaults();
        }
    }

    /**
     * 保存选择。
     *
     */
    private void saveSettings() {
        StringBuilder builder =
                new StringBuilder();

        Seq<Item> items = getSelectedItems();

        for (int i = 0; i < items.size; i++) {
            if (i > 0) {
                builder.append(",");
            }

            builder.append(items.get(i).name);
        }

        byte[] data = builder
                .toString()
                .getBytes(StandardCharsets.UTF_8);

        Core.settings.put(
                settingsKey,
                data
        );

        Core.settings.saveValues();
    }

    /**
     * 恢复预设的常用原版物品快捷项，并立即写入设置。
     */
    private void resetDefaults() {
        selected.clear();

        addDefault(Items.copper);
        addDefault(Items.lead);
        addDefault(Items.graphite);
        addDefault(Items.silicon);
        addDefault(Items.titanium);
        addDefault(Items.thorium);

        saveSettings();
    }

    private void addDefault(Item item) {
        if (item != null) {
            selected.add(item.name);
        }
    }
}
