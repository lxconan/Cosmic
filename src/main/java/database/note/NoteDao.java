package database.note;

import database.DaoException;
import model.NoteEntity;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;
import tools.DatabaseConnection;

import java.util.List;
import java.util.Optional;

public class NoteDao {

    public void save(NoteEntity note) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            handle.createUpdate("""
                            INSERT INTO notes (`message`, `from`, `to`, `timestamp`, `fame`, `deleted`)
                            VALUES (?, ?, ?, ?, ?, ?)""")
                    .bind(0, note.message())
                    .bind(1, note.from())
                    .bind(2, note.to())
                    .bind(3, note.timestamp())
                    .bind(4, note.fame())
                    .bind(5, 0)
                    .execute();
        } catch (JdbiException e) {
            throw new DaoException("Failed to save note: %s".formatted(note.toString()), e);
        }
    }

    public List<NoteEntity> findAllByTo(String to) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            return handle.createQuery("""
                            SELECT * 
                            FROM notes
                            WHERE `deleted` = 0
                            AND `to` = ?""")
                    .bind(0, to)
                    .mapTo(NoteEntity.class)
                    .list();
        } catch (JdbiException e) {
            throw new DaoException("Failed to find notes sent to: %s".formatted(to), e);
        }
    }

    public Optional<NoteEntity> delete(int id) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            Optional<NoteEntity> note = findById(handle, id);
            if (note.isEmpty()) {
                return Optional.empty();
            }
            deleteById(handle, id);

            return note;
        } catch (JdbiException e) {
            throw new DaoException("Failed to delete note with id: %d".formatted(id), e);
        }
    }

    private Optional<NoteEntity> findById(Handle handle, int id) {
        final Optional<NoteEntity> note;
        try {
            note = handle.createQuery("""
                            SELECT *
                            FROM notes
                            WHERE `deleted` = 0
                            AND `id` = ?""")
                    .bind(0, id)
                    .mapTo(NoteEntity.class)
                    .findOne();
        } catch (JdbiException e) {
            throw new DaoException("Failed find note with id %s".formatted(id), e);
        }
        return note;
    }

    private void deleteById(Handle handle, int id) {
        try {
            handle.createUpdate("""
                        UPDATE notes
                        SET `deleted` = 1
                        WHERE `id` = ?""")
                    .bind(0, id)
                    .execute();
        } catch (JdbiException e) {
            throw new DaoException("Failed to delete note with id %d".formatted(id), e);
        }
    }
}
