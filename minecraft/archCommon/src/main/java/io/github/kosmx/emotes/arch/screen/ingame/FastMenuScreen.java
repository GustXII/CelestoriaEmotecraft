package io.github.kosmx.emotes.arch.screen.ingame;

import dev.kosmx.playerAnim.core.util.MathHelper;
import io.github.kosmx.emotes.arch.screen.EmoteConfigScreen;
import io.github.kosmx.emotes.arch.screen.widget.ModernChooseWheel;
import io.github.kosmx.emotes.arch.screen.widget.IChooseWheel;
import io.github.kosmx.emotes.arch.screen.widget.AbstractFastChooseWidget;
import io.github.kosmx.emotes.executor.EmoteInstance;
import io.github.kosmx.emotes.inline.TmpGetters;
import io.github.kosmx.emotes.main.EmoteHolder;
import io.github.kosmx.emotes.main.config.ClientConfig;
import io.github.kosmx.emotes.main.network.ClientPacketManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class FastMenuScreen extends EmoteConfigScreen {

    // ── layout ──────────────────────────────────────────────────────
    private static final int PANEL_W        = 220;
    private static final int ITEM_H         = 24;
    private static final int HEADER_H       = 24;
    private static final int FOOTER_H       = 22;
    private static final int PADDING        = 6;
    private static final int MARGIN_RIGHT   = 12;
    private static final int MARGIN_BOTTOM  = 34; // above "All Emotes" button
    private static final int ITEMS_PER_PAGE = 8;
    private static final int MAX_PAGE       = 99; // same as ModernChooseWheel

    // ── colours (ARGB) ──────────────────────────────────────────────
    private static final int C_BG          = 0xCC111122;
    private static final int C_HEADER_BG   = 0xFF2A2A4A;
    private static final int C_HEADER_TXT  = 0xFFFFCC44;
    private static final int C_ITEM_TXT    = 0xFFDDDDDD;
    private static final int C_ITEM_HOVER  = 0x55303060;
    private static final int C_DIVIDER     = 0x33AAAACC;
    private static final int C_FOOTER_BG   = 0xFF1A1A30;
    private static final int C_PAGE_TXT    = 0xFFCCCCCC;
    private static final int C_DOT         = 0xFF6C63FF;
    private static final int C_EMPTY       = 0xFF444455;

    // ── warn messages ────────────────────────────────────────────────
    private static final Component WARN_NONE  = Component.translatable("emotecraft.no_server");
    private static final Component WARN_PROXY = Component.translatable("emotecraft.only_proxy");

    // ── runtime state ────────────────────────────────────────────────
    private int hoveredRow = -1;
    private int panelX, panelY, panelH;

    public FastMenuScreen(Screen screen) {
        super(Component.translatable("emotecraft.fastmenu"), screen);
    }

    // ── init ─────────────────────────────────────────────────────────
    @Override
    public void init() {
        addRenderableWidget(
                Button.builder(
                        Component.translatable("emotecraft.emotelist"),
                        btn -> getMinecraft().setScreen(new FullMenuScreenHelper(this))
                ).pos(width - 120, height - 30).size(96, 20).build()
        );
    }

    // ── render ───────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        renderBackground(gfx);

        // คำนวณ geometry ทุก frame รองรับ resize
        panelH = HEADER_H + ITEMS_PER_PAGE * ITEM_H + FOOTER_H + PADDING * 2;
        panelX = width  - PANEL_W  - MARGIN_RIGHT;
        panelY = height - panelH   - MARGIN_BOTTOM;

        renderSidebarPanel(gfx, mouseX, mouseY);
        renderWarning(gfx);
        super.render(gfx, mouseX, mouseY, delta);
    }

    private void renderSidebarPanel(GuiGraphics gfx, int mouseX, int mouseY) {
        int x = panelX, y = panelY, w = PANEL_W;
        int page = ModernChooseWheel.fastMenuPage;

        // background
        gfx.fill(x, y, x + w, y + panelH, C_BG);

        // ── header ──────────────────────────────────────────────────
        gfx.fill(x, y, x + w, y + HEADER_H, C_HEADER_BG);
        gfx.fill(x + 6, y + 5, x + 15, y + 15, C_HEADER_TXT); // icon square
        gfx.drawString(font, Component.literal("CELESTORIA TEAM"),
                x + 19, y + 7, C_HEADER_TXT, false);

        // ── emote rows ──────────────────────────────────────────────
        int listY = y + HEADER_H + PADDING;
        hoveredRow = -1;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int ry = listY + i * ITEM_H;

            // divider
            if (i > 0) gfx.fill(x, ry, x + w, ry + 1, C_DIVIDER);

            // hover detection
            boolean hover = mouseX >= x && mouseX <= x + w
                    && mouseY >= ry && mouseY <= ry + ITEM_H;
            if (hover) {
                hoveredRow = i;
                gfx.fill(x, ry, x + w, ry + ITEM_H, C_ITEM_HOVER);
            }

            UUID uuid = ((ClientConfig) EmoteInstance.config).fastMenuEmotes[page][i];
            if (uuid != null) {
                EmoteHolder holder = EmoteHolder.list.get(uuid);
                gfx.fill(x + 8, ry + 8, x + 15, ry + 15, C_DOT);
                Component label = holder != null ? holder.name
                        : Component.literal("???");
                gfx.drawString(font, label, x + 20, ry + 7, C_ITEM_TXT, false);
            } else {
                gfx.fill(x + 8, ry + 8, x + 15, ry + 15, C_EMPTY);
                gfx.drawString(font, Component.literal("—"),
                        x + 20, ry + 7, C_EMPTY, false);
            }
        }

        // ── footer / page nav ────────────────────────────────────────
        int fy = y + HEADER_H + PADDING + ITEMS_PER_PAGE * ITEM_H;
        gfx.fill(x, fy, x + w, fy + FOOTER_H, C_FOOTER_BG);

        String pageStr = "< Page " + (page + 1) + "/" + (MAX_PAGE + 1) + " >";
        int pw = font.width(pageStr);
        gfx.drawString(font, Component.literal(pageStr),
                x + (w - pw) / 2, fy + 6, C_PAGE_TXT, false);
    }

    private void renderWarning(GuiGraphics gfx) {
        if (((ClientConfig) EmoteInstance.config).hideWarningMessage.get()) return;
        int ver = ClientPacketManager.isRemoteAvailable() ? 2
                : ClientPacketManager.isAvailableProxy()  ? 1 : 0;
        if (ver == 2) return;
        Component txt = ver == 0 ? WARN_NONE : WARN_PROXY;
        gfx.drawCenteredString(Minecraft.getInstance().font, txt,
                width / 2, height / 24 - 1,
                MathHelper.colorHelper(255, 255, 255, 255));
    }

    // ── input ────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int page = ModernChooseWheel.fastMenuPage;

            // คลิก footer → เปลี่ยนหน้า
            int fy = panelY + HEADER_H + PADDING + ITEMS_PER_PAGE * ITEM_H;
            if (mouseX >= panelX && mouseX <= panelX + PANEL_W
                    && mouseY >= fy && mouseY <= fy + FOOTER_H) {
                if (mouseX < panelX + PANEL_W / 2.0) {
                    ModernChooseWheel.fastMenuPage = page > 0 ? page - 1 : MAX_PAGE;
                } else {
                    ModernChooseWheel.fastMenuPage = page < MAX_PAGE ? page + 1 : 0;
                }
                return true;
            }

            // คลิก row → เล่น emote
            if (hoveredRow >= 0) {
                UUID uuid = ((ClientConfig) EmoteInstance.config).fastMenuEmotes[page][hoveredRow];
                if (uuid != null) {
                    EmoteHolder holder = EmoteHolder.list.get(uuid);
                    if (holder != null) {
                        holder.playEmote(TmpGetters.getClientMethods().getMainPlayer());
                        Minecraft.getInstance().setScreen(null);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int page = ModernChooseWheel.fastMenuPage;
        if (amount < 0) {
            ModernChooseWheel.fastMenuPage = page < MAX_PAGE ? page + 1 : 0;
        } else {
            ModernChooseWheel.fastMenuPage = page > 0 ? page - 1 : MAX_PAGE;
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── inner widget (เก็บไว้เพื่อ compatibility กับ code อื่น) ──────
    protected FastMenuWidget newFastMenuWidget(int x, int y, int size) {
        return new FastMenuWidget(x, y, size);
    }

    protected static class FastMenuWidget extends AbstractFastChooseWidget {
        public FastMenuWidget(int x, int y, int size) { super(x, y, size); }

        @Override protected boolean doHoverPart(IChooseWheel.IChooseElement part) { return part.hasEmote(); }
        @Override protected boolean isValidClickButton(int button) { return button == 0; }
        @Override protected boolean doesShowInvalid() { return false; }

        @Override
        protected boolean onClick(IChooseWheel.IChooseElement element, int button) {
            if (element.getEmote() != null) {
                boolean bl = element.getEmote().playEmote(
                        TmpGetters.getClientMethods().getMainPlayer());
                Minecraft.getInstance().setScreen(null);
                return bl;
            }
            return false;
        }

        private boolean focused = false;
        @Override public void setFocused(boolean bl) { focused = bl; }
        @Override public boolean isFocused() { return focused; }
    }
}