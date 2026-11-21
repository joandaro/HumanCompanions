package com.github.justinwon777.humancompanions.client;

import com.github.justinwon777.humancompanions.HumanCompanions;
import com.github.justinwon777.humancompanions.compat.CompanionCurioSlotData;
import com.github.justinwon777.humancompanions.compat.CompanionCurios;
import com.github.justinwon777.humancompanions.container.CompanionContainer;
import com.github.justinwon777.humancompanions.core.PacketHandler;
import com.github.justinwon777.humancompanions.entity.AbstractHumanCompanionEntity;
import com.github.justinwon777.humancompanions.entity.Arbalist;
import com.github.justinwon777.humancompanions.entity.Archer;
import com.github.justinwon777.humancompanions.entity.Knight;
import com.github.justinwon777.humancompanions.networking.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@OnlyIn(Dist.CLIENT)
public class CompanionScreen extends AbstractContainerScreen<CompanionContainer> implements MenuAccess<CompanionContainer> {
    private static final ResourceLocation CURIO_BACKGROUND = new ResourceLocation(
            "curios", "textures/gui/inventory_revamp.png");
    private static final ResourceLocation EMPTY_CURIO_ICON = new ResourceLocation(
            "curios", "slot/empty_curio_slot");
    private static final ResourceLocation CONTAINER_BACKGROUND = new ResourceLocation(HumanCompanions.MOD_ID,
            "textures/inventory.png");
    private static final ResourceLocation ALERT_BUTTON = new ResourceLocation(HumanCompanions.MOD_ID, "textures/alertbutton.png");
    private static final ResourceLocation HUNTING_BUTTON = new ResourceLocation(HumanCompanions.MOD_ID, "textures" +
            "/huntingbutton.png");
    private static final ResourceLocation PATROL_BUTTON = new ResourceLocation(HumanCompanions.MOD_ID, "textures" +
            "/patrolbutton.png");
    private static final ResourceLocation CLEAR_BUTTON = new ResourceLocation(HumanCompanions.MOD_ID, "textures" +
            "/clearbutton.png");
    private static final ResourceLocation STATIONERY_BUTTON = new ResourceLocation(HumanCompanions.MOD_ID, "textures" +
            "/stationerybutton.png");
    private static final ResourceLocation RELEASE_BUTTON = new ResourceLocation(HumanCompanions.MOD_ID, "textures" +
            "/releasebutton.png");
    private final int containerRows;
    private final AbstractHumanCompanionEntity companion;
    private CompanionButton alertButton;
    private CompanionButton huntingButton;
    private CompanionButton patrolButton;
    private CompanionButton clearButton;
    private CompanionButton stationeryButton;
    private CompanionButton releaseButton;
    private Button previousCurioPage;
    private Button nextCurioPage;
    private final Map<String, ResourceLocation> curioIcons = new HashMap<>();
    DecimalFormat df = new DecimalFormat("#.#");
    int sidebarx;
    int rowHeight;
    int colwidth;
    int row1;
    int row2;
    int row3;
    int col1;
    int col2;

    public CompanionScreen(CompanionContainer container, Inventory pPlayerInventory,
                           AbstractHumanCompanionEntity companion) {
        super(container, pPlayerInventory, companion.getName());
        this.companion = companion;
        this.containerRows = container.getRowCount();
        this.imageHeight = 114 + this.containerRows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
        this.imageWidth = 226;
        df.setRoundingMode(RoundingMode.CEILING);
        sidebarx = 174;
        rowHeight = 15;
        colwidth = 19;
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, CONTAINER_BACKGROUND);
        int i = this.leftPos;
        int j = this.topPos;
        pGuiGraphics.blit(CONTAINER_BACKGROUND, i, j, 0, 0, this.imageWidth, this.containerRows * 18 + 17);
        pGuiGraphics.blit(CONTAINER_BACKGROUND, i, j + this.containerRows * 18 + 17, 0, 126, this.imageWidth, 96);
        if (menu.hasCurios()) {
            renderCurioPanel(pGuiGraphics);
        }
    }

    private int getCurioFrameX() {
        return leftPos - CompanionCurios.PANEL_GAP - menu.getCurioPanelWidth();
    }

    private int getCurioFrameY() {
        return topPos;
    }

    private int getCurioFrameHeight() {
        return menu.getCurioPanelHeight();
    }

    private void renderCurioPanel(GuiGraphics graphics) {
        RenderSystem.enableBlend();
        drawCurioFrame(graphics, getCurioFrameX(), getCurioFrameY(),
                menu.getCurioPanelWidth(), getCurioFrameHeight());

        for (Slot slot : menu.slots) {
            CompanionCurioSlotData address = menu.getCurioSlotData(slot.index);
            if (address == null || !slot.isActive()) {
                continue;
            }

            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.blit(CURIO_BACKGROUND, x - 1, y - 1, 7, 7, 18, 18);

            if (!slot.hasItem() && minecraft != null) {
                ResourceLocation icon = curioIcons.computeIfAbsent(address.identifier(),
                        id -> CuriosArtwork.getSlotIcon(id, companion.level()));
                graphics.blit(x, y, 0, 16, 16,
                        minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(icon));
            }
        }
        RenderSystem.disableBlend();
    }

    private void drawCurioFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        // Curios' blank backplate occupies (91, 0), size 32x166, with 7px edges.
        int insideWidth = width - 14;
        int insideHeight = height - 14;

        graphics.blit(CURIO_BACKGROUND, x, y, 91, 0, 7, 7);
        graphics.blit(CURIO_BACKGROUND, x + width - 7, y, 116, 0, 7, 7);
        graphics.blit(CURIO_BACKGROUND, x, y + height - 7, 91, 159, 7, 7);
        graphics.blit(CURIO_BACKGROUND, x + width - 7, y + height - 7, 116, 159, 7, 7);

        graphics.blit(CURIO_BACKGROUND, x, y + 7, 91, 7, 7, insideHeight);
        graphics.blit(CURIO_BACKGROUND, x + width - 7, y + 7, 116, 7, 7, insideHeight);

        for (int offset = 0; offset < insideWidth; offset += 18) {
            int pieceWidth = Math.min(18, insideWidth - offset);
            graphics.blit(CURIO_BACKGROUND, x + 7 + offset, y, 98, 0, pieceWidth, 7);
            graphics.blit(CURIO_BACKGROUND, x + 7 + offset, y + 7,
                    98, 7, pieceWidth, insideHeight);
            graphics.blit(CURIO_BACKGROUND, x + 7 + offset, y + height - 7,
                    98, 159, pieceWidth, 7);
        }
    }

    @Override
    protected void init() {
        super.init();
        curioIcons.clear();
        previousCurioPage = null;
        nextCurioPage = null;
        if (menu.hasCurios()) {
            // Keep the main window at its normal position. Shift only if both panels
            // fit and the accessory panel would otherwise be clipped on the left.
            int reservedWidth = menu.getMaximumCurioPanelWidth() + CompanionCurios.PANEL_GAP;
            if (width >= imageWidth + reservedWidth) {
                leftPos = Math.max(leftPos, reservedWidth);
            }
            if (menu.getCurioPageCount() > 1) {
                int arrowY = getCurioFrameY() + 2;
                int arrowX = leftPos - CompanionCurios.PANEL_GAP - 29;
                previousCurioPage = addRenderableWidget(new CurioPageButton(arrowX, arrowY, false, button -> {
                    menu.setCurioPage(menu.getCurioPage() - 1);
                    updateCurioPageButtons();
                }));
                nextCurioPage = addRenderableWidget(new CurioPageButton(arrowX + 11, arrowY, true, button -> {
                    menu.setCurioPage(menu.getCurioPage() + 1);
                    updateCurioPageButtons();
                }));
                updateCurioPageButtons();
            }
        }
        row1 = topPos + 66;
        row2 = row1 + rowHeight;
        row3 = row2 + rowHeight;
        col1 = leftPos + sidebarx + 3;
        col2 = col1 + colwidth;
        this.alertButton = addRenderableWidget(new CompanionButton("alert", col1, row1, 16,
                12, 0
                , 0, 13,
                ALERT_BUTTON,
                btn -> {
                    PacketHandler.INSTANCE.sendToServer(new SetAlertPacket(companion.getId()));
                }));
        this.huntingButton = addRenderableWidget(new CompanionButton("hunting", col2, row1,
                16,
                12, 0, 0,13,
                HUNTING_BUTTON,
                btn -> {
                    PacketHandler.INSTANCE.sendToServer(new SetHuntingPacket(companion.getId()));
                }));
        this.patrolButton = addRenderableWidget(new CompanionButton("patrolling", col1, row2,
                16,
                12,
                0, 0
                ,13,
                PATROL_BUTTON,
                btn -> {
                    PacketHandler.INSTANCE.sendToServer(new SetPatrolingPacket(companion.getId()));
                }));
        if (companion instanceof Archer || companion instanceof Arbalist) {
            this.stationeryButton = addRenderableWidget(new CompanionButton("stationery", col2,
                    row2,
                    16,
                    12,
                    0, 0
                    , 13,
                    STATIONERY_BUTTON,
                    btn -> {
                        PacketHandler.INSTANCE.sendToServer(new SetStationeryPacket(companion.getId()));
                    }));
        }
        this.clearButton = addRenderableWidget(new CompanionButton("clear", leftPos + sidebarx + 5, row3, 31,
                12, 0, 0
                ,13,
                CLEAR_BUTTON,
                btn -> {
                    PacketHandler.INSTANCE.sendToServer(new ClearTargetPacket(companion.getId()));
                }));
        this.releaseButton = addRenderableWidget(new CompanionButton("release", leftPos + sidebarx + 3, topPos + 148,
                34,
                12, 0, 0
                ,13,
                RELEASE_BUTTON,
                btn -> {
                    PacketHandler.INSTANCE.sendToServer(new ReleasePacket(companion.getId()));
                    this.onClose();
                }));
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        super.renderLabels(pGuiGraphics, pMouseX, pMouseY);
        int classHeight = this.titleLabelY + 14;
        int classLeft = sidebarx + 4;
        MutableComponent classTitle = Component.literal("Class");
        MutableComponent healthTitle = Component.literal("Health");
        MutableComponent health =
                Component.literal(df.format(companion.getHealth()) + "/" + (int) companion.getMaxHealth());

        pGuiGraphics.drawString(this.font, classTitle.withStyle(ChatFormatting.UNDERLINE), sidebarx + 4, this.titleLabelY + 3,
                4210752, false);
        if (companion instanceof Arbalist) {
            pGuiGraphics.drawString(this.font, "Arbalist", classLeft, classHeight, 4210752, false);
        } else if (companion instanceof Archer) {
            pGuiGraphics.drawString(this.font, "Archer", classLeft, classHeight, 4210752, false);
        } else if (companion instanceof Knight) {
            pGuiGraphics.drawString(this.font, "Knight", classLeft, classHeight, 4210752, false);
        } else {
            pGuiGraphics.drawString(this.font, "Axe", classLeft, classHeight, 4210752, false);
        }

        pGuiGraphics.drawString(this.font, healthTitle.withStyle(ChatFormatting.UNDERLINE), sidebarx + 4, this.titleLabelY + 26,
                4210752, false);
        pGuiGraphics.drawString(this.font, health, sidebarx + 4, this.titleLabelY + 37, 4210752, false);

        pGuiGraphics.drawString(this.font, "Level " + companion.getExpLvl(), sidebarx, this.titleLabelY + 49,
                4210752, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics pGuiGraphics, int x, int y) {
        super.renderTooltip(pGuiGraphics, x, y);
        if (hoveredSlot != null && !hoveredSlot.hasItem()) {
            CompanionCurioSlotData address = menu.getCurioSlotData(hoveredSlot.index);
            if (address != null) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.translatable("curios.identifier." + address.identifier()));
                lines.add(Component.translatable("gui.humancompanions.curios.approved_only")
                        .withStyle(ChatFormatting.GRAY));
                if (!menu.getCarried().isEmpty() && !CompanionCurios.isApproved(menu.getCarried())) {
                    lines.add(Component.translatable("gui.humancompanions.curios.not_approved")
                            .withStyle(ChatFormatting.RED));
                }
                pGuiGraphics.renderTooltip(font, lines, Optional.empty(), x, y);
            }
        }
        if (this.alertButton.isHovered()) {
            List<Component> tooltips = new ArrayList<>();
            if (this.companion.isAlert()) {
                tooltips.add(Component.literal("Alert mode: On"));
            } else {
                tooltips.add(Component.literal("Alert mode: Off"));
            }
            tooltips.add(Component.literal("Attacks nearby hostile mobs").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));

            pGuiGraphics.renderTooltip(this.font, tooltips, Optional.empty(), x, y);
        }
        if (this.huntingButton.isHovered()) {
            List<Component> tooltips = new ArrayList<>();
            if (this.companion.isHunting()) {
                tooltips.add(Component.literal("Hunting mode: On"));
            } else {
                tooltips.add(Component.literal("Hunting mode: Off"));
            }
            tooltips.add(Component.literal("Attacks nearby mobs for food").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));

            pGuiGraphics.renderTooltip(this.font, tooltips, Optional.empty(), x, y);
        }

        if (this.patrolButton.isHovered()) {
            List<Component> tooltips = new ArrayList<>();
            if (this.companion.isFollowing()) {
                tooltips.add(Component.literal("Follow"));
                tooltips.add(Component.literal("Follows you").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
            } else if (this.companion.isPatrolling()) {
                tooltips.add(Component.literal("Patrol"));
                tooltips.add(Component.literal("Patrols a 4 block radius").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
            } else {
                tooltips.add(Component.literal("Guard"));
                tooltips.add(Component.literal("Stands at its position ready for action").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
            }

            pGuiGraphics.renderTooltip(this.font, tooltips, Optional.empty(), x, y);
        }

        if (this.clearButton.isHovered()) {
            List<Component> tooltips = new ArrayList<>();
            tooltips.add(Component.literal("Clear target"));
            tooltips.add(Component.literal("Useful if it gets stuck attacking").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));

            pGuiGraphics.renderTooltip(this.font, tooltips, Optional.empty(), x, y);
        }

        if (this.releaseButton.isHovered()) {
            List<Component> tooltips = new ArrayList<>();
            tooltips.add(Component.literal("Release Companion"));
            tooltips.add(Component.literal("Releases companion from your command. It can be tamed again.").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));

            pGuiGraphics.renderTooltip(this.font, tooltips, Optional.empty(), x, y);
        }

        if (companion instanceof Archer || companion instanceof Arbalist) {
            if (this.stationeryButton.isHovered()) {
                List<Component> tooltips = new ArrayList<>();
                if (this.companion.isStationery()) {
                    tooltips.add(Component.literal("Stationery: On"));
                } else {
                    tooltips.add(Component.literal("Stationery: Off"));
                }
                tooltips.add(Component.literal("Companion will not move while attacking in guard mode").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));

                pGuiGraphics.renderTooltip(this.font, tooltips, Optional.empty(), x, y);
            }
        }
    }

    private void updateCurioPageButtons() {
        if (previousCurioPage != null) {
            previousCurioPage.active = menu.getCurioPage() > 0;
            nextCurioPage.active = menu.getCurioPage() + 1 < menu.getCurioPageCount();
        }
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        boolean insideMain = mouseX >= leftPos && mouseX < leftPos + imageWidth
                && mouseY >= topPos && mouseY < topPos + imageHeight;
        boolean insideCurios = menu.hasCurios()
                && mouseX >= getCurioFrameX() && mouseX < getCurioFrameX() + menu.getCurioPanelWidth()
                && mouseY >= getCurioFrameY() && mouseY < getCurioFrameY() + getCurioFrameHeight();
        return !insideMain && !insideCurios;
    }

    // This class is loaded only when the accessory panel asks for an icon.
    // Its Curios API references never need to resolve when Curios is absent.
    private static final class CuriosArtwork {
        private static ResourceLocation getSlotIcon(String identifier, Level level) {
            return top.theillusivec4.curios.api.CuriosApi.getSlot(identifier, level)
                    .map(slot -> slot.getIcon())
                    .orElse(EMPTY_CURIO_ICON);
        }
    }

    private final class CurioPageButton extends Button {
        private final boolean next;

        private CurioPageButton(int x, int y, boolean next, Button.OnPress onPress) {
            super(x, y, 11, 12, Component.literal(next ? ">" : "<"), onPress, DEFAULT_NARRATION);
            this.next = next;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int u = next ? 43 : 32;
            int v = 25;
            if (!active) {
                v += 12;
            } else if (isHoveredOrFocused()) {
                u += 22;
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.blit(CURIO_BACKGROUND, getX(), getY(), u, v, width, height);
            if (isHovered()) {
                graphics.renderTooltip(font, Component.translatable("gui.curios.page",
                        menu.getCurioPage() + 1, menu.getCurioPageCount()), mouseX, mouseY);
            }
        }
    }

    class CompanionButton extends ImageButton {

        private String name;

        public CompanionButton(String name, int p_94269_, int p_94270_, int p_94271_, int p_94272_, int p_94273_,
                               int p_94274_,
                               int p_94275_, ResourceLocation p_94276_,
                               Button.OnPress p_94277_) {
            super(p_94269_, p_94270_, p_94271_, p_94272_, p_94273_, p_94274_, p_94275_, p_94276_, p_94277_);
            this.name = name;
        }

        @Override
        public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            if (this.name.equals("alert")) {
                if (CompanionScreen.this.companion.isAlert()) {
                    this.xTexStart = 0;
                } else {
                    this.xTexStart = 17;
                }
            } else if (this.name.equals("hunting")) {
                if (CompanionScreen.this.companion.isHunting()) {
                    this.xTexStart = 0;
                } else {
                    this.xTexStart = 17;
                }
            } else if (this.name.equals("patrolling")) {
                if (CompanionScreen.this.companion.isFollowing()) {
                    this.xTexStart = 0;
                } else if (CompanionScreen.this.companion.isPatrolling()){
                    this.xTexStart = 17;
                } else {
                    this.xTexStart = 34;
                }
            } else if (this.name.equals("stationery")) {
                if (CompanionScreen.this.companion.isStationery()) {
                    this.xTexStart = 0;
                } else {
                    this.xTexStart = 17;
                }
            }
            RenderSystem.enableBlend();
            super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            RenderSystem.disableBlend();
        }
    }
}
