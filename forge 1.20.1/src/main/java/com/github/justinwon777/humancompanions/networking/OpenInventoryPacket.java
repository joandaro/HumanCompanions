package com.github.justinwon777.humancompanions.networking;

import com.github.justinwon777.humancompanions.compat.CompanionCurioSlotData;
import com.github.justinwon777.humancompanions.core.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

public class OpenInventoryPacket {
    private final int id;
    private final int size;
    private final int entityId;
    private final List<CompanionCurioSlotData> curioSlots;

    public OpenInventoryPacket(int id, int size, int entityId) {
        this(id, size, entityId, List.of());
    }

    public OpenInventoryPacket(
            int id,
            int size,
            int entityId,
            List<CompanionCurioSlotData> curioSlots) {
        this.id = id;
        this.size = size;
        this.entityId = entityId;

        if (curioSlots.size() > CompanionCurioSlotData.MAX_SLOTS
                || new HashSet<>(curioSlots).size() != curioSlots.size()) {
            throw new IllegalArgumentException(
                    "Invalid companion Curios layout");
        }

        this.curioSlots = List.copyOf(curioSlots);
    }

    public static OpenInventoryPacket decode(FriendlyByteBuf buf) {
        int id = buf.readUnsignedByte();
        int size = buf.readVarInt();
        int entityId = buf.readInt();
        int count = buf.readVarInt();

        if (count < 0 || count > CompanionCurioSlotData.MAX_SLOTS) {
            throw new IllegalArgumentException(
                    "Invalid companion Curios slot count");
        }

        List<CompanionCurioSlotData> slots = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            slots.add(new CompanionCurioSlotData(
                    buf.readUtf(
                            CompanionCurioSlotData.MAX_IDENTIFIER_LENGTH),
                    buf.readVarInt()));
        }

        return new OpenInventoryPacket(id, size, entityId, slots);
    }

    public static void encode(
            OpenInventoryPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.id);
        buf.writeVarInt(msg.size);
        buf.writeInt(msg.entityId);
        buf.writeVarInt(msg.curioSlots.size());

        for (CompanionCurioSlotData slot : msg.curioSlots) {
            buf.writeUtf(
                    slot.identifier(),
                    CompanionCurioSlotData.MAX_IDENTIFIER_LENGTH);
            buf.writeVarInt(slot.index());
        }
    }

    public int getId() {
        return id;
    }

    public int getEntityId() {
        return entityId;
    }

    public List<CompanionCurioSlotData> getCurioSlots() {
        return curioSlots;
    }

    public static void handle(
            OpenInventoryPacket msg,
            Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(
                () -> PacketHandler.openInventory(msg));
        context.get().setPacketHandled(true);
    }
}