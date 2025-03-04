package net.packet.out;

import model.NoteEntity;
import net.opcodes.SendOpcode;
import net.packet.ByteBufOutPacket;

import java.util.List;
import java.util.Objects;

import static tools.PacketCreator.getTime;

public final class ShowNotesPacket extends ByteBufOutPacket {

    public ShowNotesPacket(List<NoteEntity> notes) {
        super(SendOpcode.MEMO_RESULT);
        Objects.requireNonNull(notes);

        writeByte(3);
        writeByte(notes.size());
        notes.forEach(this::writeNote);
    }

    private void writeNote(NoteEntity note) {
        writeInt(note.id());
        writeString(note.from() + " "); //Stupid nexon forgot space lol
        writeString(note.message());
        writeLong(getTime(note.timestamp()));
        writeByte(note.fame());
    }
}
