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
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class FastMenuScreen extends EmoteConfigScreen {

    // ── layout ──────────────────────────────────────────────────────
    private static final int PANEL_W        = 230;
    private static final int ITEM_H         = 26;   // สูงขึ้นนิดเพื่อให้รูปพอดี
    private static final int ICON_SIZE      = 18;   // ขนาด icon ใน row
    private static final int HEADER_H       = 24;
    private static final int FOOTER_H       = 22;
    private static final int PADDING        = 6;
    private static final int MARGIN_RIGHT   = 12;
    private static final int MARGIN_BOTTOM  = 34;
    private static final int ITEMS_PER_PAGE = 8;
    private static final int MAX_PAGE       = 99;

    // ── colours (ARGB) ──────────────────────────────────────────────
    private static final int C_BG          = 0xCC111122;
    private static final int C_HEADER_BG   = 0xFF2A2A4A;
    private static final int C_HEADER_TXT  = 0xFFFFCC44;
    private static final int C_ITEM_TXT    = 0xFFDDDDDD;
    private static final int C_ITEM_HOVER  = 0x55303060;
    private static final int C_DIVIDER     = 0x33AAAACC;
    private static final int C_FOOTER_BG   = 0xFF1A1A30;
    private static final int C_PAGE_TXT    = 0xFFCCCCCC;
    private static final int C_DOT         = 0xFF6C63FF;  // fallback dot ถ้าไม่มี icon
    private static final int C_EMPTY       = 0xFF444455;
    private static final int C_DISABLE_LBL = 0xFFAAAAAA;
    private static final int C_DISABLE_OFF = 0xFFFF4444;

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
        gfx.fill(x + 6, y + 5, x + 15, y + 15, C_HEADER_TXT);
        gfx.drawString(font, Component.literal("CELESTORIA TEAM"),
                x + 19, y + 7, C_HEADER_TXT, false);

        // ── emote rows ──────────────────────────────────────────────
        int listY = y + HEADER_H + PADDING;
        hoveredRow = -1;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int ry = listY + i * ITEM_H;

            if (i > 0) gfx.fill(x, ry, x + w, ry + 1, C_DIVIDER);

            boolean hover = mouseX >= x && mouseX <= x + w
                    && mouseY >= ry && mouseY <= ry + ITEM_H;
            if (hover) {
                hoveredRow = i;
                gfx.fill(x, ry, x + w, ry + ITEM_H, C_ITEM_HOVER);
            }

            UUID uuid = ((ClientConfig) EmoteInstance.config).fastMenuEmotes[page][i];

            if (uuid != null) {
                EmoteHolder holder = EmoteHolder.list.get(uuid);

                // icon position: กลาง vertical ของ row
                int iconX = x + 5;
                int iconY = ry + (ITEM_H - ICON_SIZE) / 2;
                int textX  = iconX + ICON_SIZE + 4;

                if (holder != null) {
                    // ── ลอง render icon รูปภาพก่อน ──────────────────
                    ResourceLocation iconId = null;
                    if (((ClientConfig) EmoteInstance.config).showIcons.get()) {
                        iconId = holder.getIconIdentifier();
                    }

                    if (iconId != null) {
                        // มี icon รูปภาพ → blit เหมือนที่ LegacyChooseWidget ทำ
                        gfx.blit(iconId,
                                iconX, iconY,
                                ICON_SIZE, ICON_SIZE,
                                0f, 0f,
                                256, 256,
                                256, 256);
                    } else {
                        // ไม่มี icon → แสดง dot สีม่วงแทน
                        int dotPad = (ICON_SIZE - 8) / 2;
                        gfx.fill(iconX + dotPad, iconY + dotPad,
                                iconX + dotPad + 8, iconY + dotPad + 8,
                                C_DOT);
                    }

                    // ── ชื่อ emote ───────────────────────────────────
                    // holder.name เป็น Component field ตรงๆ
                    gfx.drawString(font, holder.name,
                            textX, ry + (ITEM_H - 8) / 2, C_ITEM_TXT, false);

                } else {
                    // uuid มีแต่หา holder ไม่เจอ
                    int dotPad = (ICON_SIZE - 8) / 2;
                    gfx.fill(iconX + dotPad, iconY + dotPad,
                            iconX + dotPad + 8, iconY + dotPad + 8,
                            C_EMPTY);
                    gfx.drawString(font, Component.literal("???"),
                            textX, ry + (ITEM_H - 8) / 2, C_EMPTY, false);
                }

            } else {
                // slot ว่าง
                int iconX = x + 5;
                int iconY = ry + (ITEM_H - ICON_SIZE) / 2;
                int dotPad = (ICON_SIZE - 8) / 2;
                gfx.fill(iconX + dotPad, iconY + dotPad,
                        iconX + dotPad + 8, iconY + dotPad + 8,
                        C_EMPTY);
                gfx.drawString(font, Component.literal("—"),
                        iconX + ICON_SIZE + 4, ry + (ITEM_H - 8) / 2,
                        C_EMPTY, false);
            }
        }

        // ── footer / page nav ────────────────────────────────────────
        int fy = y + HEADER_H + PADDING + ITEMS_PER_PAGE * ITEM_H;
        gfx.fill(x, fy, x + w, fy + FOOTER_H, C_FOOTER_BG);

        String pageStr = "< Page " + (page + 1) + "/" + (MAX_PAGE + 1) + " >";
        int pw = font.width(pageStr);
        gfx.drawString(font, Component.literal(pageStr),
                x + (w - pw) / 2, fy + 6, C_PAGE_TXT, false);

        // ── Disable Emote badge ──────────────────────────────────────
        int bx = x;
        int by = y + panelH + 3;
        int lblW = font.width("Disable Emote: ");
        gfx.fill(bx, by, bx + w, by + 14, 0xFF1A1A30);
        gfx.drawString(font, Component.literal("Disable Emote: "),
                bx + 6, by + 3, C_DISABLE_LBL, false);
        gfx.drawString(font, Component.literal("OFF"),
                bx + 6 + lblW, by + 3, C_DISABLE_OFF, false);
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

            // footer → เปลี่ยนหน้า
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

            // row → เล่น emote
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

    // ── inner widget (compatibility) ─────────────────────────────────
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