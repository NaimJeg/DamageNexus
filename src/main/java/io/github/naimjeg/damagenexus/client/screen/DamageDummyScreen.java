package io.github.naimjeg.damagenexus.client.screen;

import io.github.naimjeg.damagenexus.entity.DamageDummyAttributeEdit;
import io.github.naimjeg.damagenexus.entity.DamageDummyAttributeSnapshot;
import io.github.naimjeg.damagenexus.entity.DamageDummyAttributeView;
import io.github.naimjeg.damagenexus.menu.DamageDummyMenu;
import io.github.naimjeg.damagenexus.network.payload.DamageDummyApplyAttributesPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Compact, scrollable editor for the server-owned damage-dummy attributes. */
public class DamageDummyScreen extends AbstractContainerScreen<DamageDummyMenu> {

    private static final int PANEL_WIDTH = 332;
    private static final int PANEL_HEIGHT = 234;
    private static final int TITLE_Y = 10;
    private static final int HEADER_Y = 29;
    private static final int ROWS_TOP = 43;
    private static final int ROW_HEIGHT = 24;
    private static final int VISIBLE_ROWS = 6;
    private static final int NAME_X = 12;
    private static final int NAME_WIDTH = 137;
    private static final int INPUT_X = 151;
    private static final int INPUT_WIDTH = 84;
    private static final int EFFECTIVE_X = 242;
    private static final int STATUS_Y = 189;
    private static final int BUTTON_Y = 207;

    private static final int PANEL_BORDER_COLOR = 0xFF3A3A3A;
    private static final int PANEL_FILL_COLOR = 0xF0101010;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int LABEL_COLOR = 0xFFE0E0E0;
    private static final int MUTED_COLOR = 0xFF9E9E9E;
    private static final int ERROR_COLOR = 0xFFFF5555;
    private static final int SCROLL_TRACK_COLOR = 0xFF242424;
    private static final int SCROLL_THUMB_COLOR = 0xFF777777;

    private final Map<Identifier, String> inputValues = new LinkedHashMap<>();
    private final Set<Identifier> dirtyAttributes = new HashSet<>();
    private final Map<Identifier, EditBox> visibleInputs = new HashMap<>();
    private int scrollOffset;
    private long observedSnapshotVersion = -1L;
    private boolean waitingForServer;
    private Button applyButton;

    public DamageDummyScreen(
            DamageDummyMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.applyButton = this.addRenderableWidget(
                Button.builder(
                        Component.translatable(
                                "gui.damagenexus.damage_dummy.apply"
                        ),
                        button -> this.applyChanges()
                ).bounds(
                        this.leftPos + (this.imageWidth - 112) / 2,
                        this.topPos + BUTTON_Y,
                        112,
                        20
                ).build()
        );
        this.refreshFromSnapshot();
    }

    /** Called only after the client payload handler validated this open menu. */
    public void refreshFromSnapshot() {
        DamageDummyAttributeSnapshot snapshot = this.menu.snapshot();
        int previousOffset = this.scrollOffset;
        this.inputValues.clear();
        for (DamageDummyAttributeView entry : snapshot.attributes()) {
            this.inputValues.put(entry.id(), formatDouble(entry.baseValue()));
        }
        this.dirtyAttributes.clear();
        this.waitingForServer = false;
        this.observedSnapshotVersion = this.menu.snapshotVersion();
        this.scrollOffset = Math.min(previousOffset, this.maxScrollOffset());
        this.rebuildVisibleInputs();
        this.updateApplyState();
    }

    @Override
    protected void containerTick() {
        if (this.observedSnapshotVersion != this.menu.snapshotVersion()) {
            this.refreshFromSnapshot();
        }
    }

    private void rebuildVisibleInputs() {
        for (EditBox box : this.visibleInputs.values()) {
            this.removeWidget(box);
        }
        this.visibleInputs.clear();

        DamageDummyAttributeSnapshot snapshot = this.menu.snapshot();
        if (!snapshot.available()) {
            return;
        }
        int end = Math.min(
                snapshot.attributes().size(),
                this.scrollOffset + VISIBLE_ROWS
        );
        for (int index = this.scrollOffset; index < end; index++) {
            DamageDummyAttributeView entry = snapshot.attributes().get(index);
            int visibleIndex = index - this.scrollOffset;
            EditBox box = new EditBox(
                    this.font,
                    this.leftPos + INPUT_X,
                    this.topPos + ROWS_TOP + visibleIndex * ROW_HEIGHT,
                    INPUT_WIDTH,
                    18,
                    Component.translatable(
                            "gui.damagenexus.damage_dummy.base_value"
                    ).append(" ").append(Component.literal(entry.id().toString()))
            );
            box.setMaxLength(64);
            box.setValue(this.inputValues.getOrDefault(entry.id(), ""));
            box.setResponder(value -> this.onInputChanged(entry, box, value));
            box.setTooltip(Tooltip.create(Component.literal(
                    entry.id().toString()
            )));
            box.active = !this.waitingForServer;
            this.updateInputColor(entry, box);
            this.visibleInputs.put(entry.id(), this.addRenderableWidget(box));
        }
    }

    private void onInputChanged(
            DamageDummyAttributeView entry,
            EditBox box,
            String value
    ) {
        this.inputValues.put(entry.id(), value);
        Double parsed = parseFinite(value);
        if (parsed != null
                && Double.compare(parsed, entry.baseValue()) == 0) {
            this.dirtyAttributes.remove(entry.id());
        } else {
            this.dirtyAttributes.add(entry.id());
        }
        this.updateInputColor(entry, box);
        this.updateApplyState();
    }

    private void updateInputColor(
            DamageDummyAttributeView entry,
            EditBox box
    ) {
        String value = this.inputValues.get(entry.id());
        box.setTextColor(parseFinite(value) == null
                ? ERROR_COLOR
                : LABEL_COLOR);
    }

    private void updateApplyState() {
        if (this.applyButton == null) {
            return;
        }
        this.applyButton.active = this.menu.snapshot().available()
                && !this.waitingForServer
                && !this.dirtyAttributes.isEmpty()
                && !this.hasInvalidInput();
    }

    private boolean hasInvalidInput() {
        for (Identifier id : this.dirtyAttributes) {
            if (parseFinite(this.inputValues.get(id)) == null) {
                return true;
            }
        }
        return false;
    }

    private void applyChanges() {
        if (this.applyButton == null || !this.applyButton.active) {
            return;
        }
        List<DamageDummyAttributeEdit> edits = new ArrayList<>();
        for (DamageDummyAttributeView entry
                : this.menu.snapshot().attributes()) {
            if (!this.dirtyAttributes.contains(entry.id())) {
                continue;
            }
            Double value = parseFinite(this.inputValues.get(entry.id()));
            if (value == null) {
                this.updateApplyState();
                return;
            }
            edits.add(new DamageDummyAttributeEdit(entry.id(), value));
        }
        if (edits.isEmpty()) {
            this.updateApplyState();
            return;
        }

        this.waitingForServer = true;
        this.updateApplyState();
        for (EditBox box : this.visibleInputs.values()) {
            box.active = false;
        }
        ClientPacketDistributor.sendToServer(
                new DamageDummyApplyAttributesPayload(
                        this.menu.containerId,
                        this.menu.anchorPos(),
                        edits
                )
        );
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if ((event.key() == 257 || event.key() == 335)
                && this.applyButton != null
                && this.applyButton.active) {
            this.applyChanges();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(
            double x,
            double y,
            double scrollX,
            double scrollY
    ) {
        boolean overRows = x >= this.leftPos + 8
                && x < this.leftPos + this.imageWidth - 8
                && y >= this.topPos + ROWS_TOP
                && y < this.topPos + ROWS_TOP
                + VISIBLE_ROWS * ROW_HEIGHT;
        if (overRows && this.maxScrollOffset() > 0 && scrollY != 0.0D) {
            int next = Math.max(
                    0,
                    Math.min(
                            this.maxScrollOffset(),
                            this.scrollOffset + (scrollY < 0.0D ? 1 : -1)
                    )
            );
            if (next != this.scrollOffset) {
                this.scrollOffset = next;
                this.rebuildVisibleInputs();
            }
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    private int maxScrollOffset() {
        return Math.max(
                0,
                this.menu.snapshot().attributes().size() - VISIBLE_ROWS
        );
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(
                x - 1,
                y - 1,
                x + this.imageWidth + 1,
                y + this.imageHeight + 1,
                PANEL_BORDER_COLOR
        );
        graphics.fill(
                x,
                y,
                x + this.imageWidth,
                y + this.imageHeight,
                PANEL_FILL_COLOR
        );
    }

    @Override
    protected void extractLabels(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY
    ) {
        drawCentered(
                graphics,
                Component.translatable(
                        "gui.damagenexus.damage_dummy.title"
                ),
                TITLE_Y,
                TITLE_COLOR
        );
        DamageDummyAttributeSnapshot snapshot = this.menu.snapshot();
        if (!snapshot.available()) {
            drawCentered(
                    graphics,
                    Component.translatable(
                            "gui.damagenexus.damage_dummy.unavailable"
                    ),
                    103,
                    MUTED_COLOR
            );
            return;
        }
        if (snapshot.attributes().isEmpty()) {
            drawCentered(
                    graphics,
                    Component.translatable(
                            "gui.damagenexus.damage_dummy.no_attributes"
                    ),
                    103,
                    MUTED_COLOR
            );
            return;
        }

        graphics.text(
                this.font,
                Component.translatable(
                        "gui.damagenexus.damage_dummy.base_value"
                ),
                INPUT_X,
                HEADER_Y,
                MUTED_COLOR,
                false
        );
        graphics.text(
                this.font,
                Component.translatable(
                        "gui.damagenexus.damage_dummy.effective_value"
                ),
                EFFECTIVE_X,
                HEADER_Y,
                MUTED_COLOR,
                false
        );

        int end = Math.min(
                snapshot.attributes().size(),
                this.scrollOffset + VISIBLE_ROWS
        );
        for (int index = this.scrollOffset; index < end; index++) {
            DamageDummyAttributeView entry = snapshot.attributes().get(index);
            int rowY = ROWS_TOP
                    + (index - this.scrollOffset) * ROW_HEIGHT + 5;
            String name = I18n.exists(entry.translationKey())
                    ? Component.translatable(entry.translationKey()).getString()
                    : entry.id().toString();
            graphics.text(
                    this.font,
                    this.font.plainSubstrByWidth(name, NAME_WIDTH),
                    NAME_X,
                    rowY,
                    LABEL_COLOR,
                    false
            );
            graphics.text(
                    this.font,
                    formatDouble(entry.effectiveValue()),
                    EFFECTIVE_X,
                    rowY,
                    MUTED_COLOR,
                    false
            );
            if (mouseX >= this.leftPos + NAME_X
                    && mouseX < this.leftPos + NAME_X + NAME_WIDTH
                    && mouseY >= this.topPos + rowY - 3
                    && mouseY < this.topPos + rowY + 12) {
                graphics.setTooltipForNextFrame(
                        this.font,
                        Component.literal(entry.id().toString()),
                        mouseX,
                        mouseY
                );
            }
        }

        if (this.maxScrollOffset() > 0) {
            int trackX = this.imageWidth - 8;
            int trackY = ROWS_TOP;
            int trackHeight = VISIBLE_ROWS * ROW_HEIGHT;
            int thumbHeight = Math.max(
                    18,
                    trackHeight * VISIBLE_ROWS
                            / snapshot.attributes().size()
            );
            int thumbY = trackY
                    + (trackHeight - thumbHeight) * this.scrollOffset
                            / this.maxScrollOffset();
            graphics.fill(
                    trackX,
                    trackY,
                    trackX + 3,
                    trackY + trackHeight,
                    SCROLL_TRACK_COLOR
            );
            graphics.fill(
                    trackX,
                    thumbY,
                    trackX + 3,
                    thumbY + thumbHeight,
                    SCROLL_THUMB_COLOR
            );
        }

        Component status = this.statusText();
        if (status != null) {
            drawCentered(
                    graphics,
                    status,
                    STATUS_Y,
                    this.hasInvalidInput() ? ERROR_COLOR : MUTED_COLOR
            );
        }
    }

    private Component statusText() {
        if (this.waitingForServer) {
            return Component.translatable(
                    "gui.damagenexus.damage_dummy.waiting"
            );
        }
        if (this.hasInvalidInput()) {
            return Component.translatable(
                    "gui.damagenexus.damage_dummy.invalid_input"
            );
        }
        return null;
    }

    private void drawCentered(
            GuiGraphicsExtractor graphics,
            Component text,
            int y,
            int color
    ) {
        int x = (this.imageWidth - this.font.width(text)) / 2;
        graphics.text(this.font, text, x, y, color, true);
    }

    private static Double parseFinite(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            double value = Double.parseDouble(text.trim());
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static String formatDouble(double value) {
        if (value == 0.0D) {
            return "0";
        }
        String text = Double.toString(value);
        if (text.endsWith(".0")) {
            return text.substring(0, text.length() - 2);
        }
        int exponent = text.indexOf('E');
        return exponent > 1 && text.substring(0, exponent).endsWith(".0")
                ? text.substring(0, exponent - 2) + text.substring(exponent)
                : text;
    }
}
