package database.note;

import model.NoteEntity;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class NoteRowMapper implements RowMapper<NoteEntity> {

    @Override
    public NoteEntity map(ResultSet rs, StatementContext ctx) throws SQLException {
        int id = rs.getInt("id");
        String message = rs.getString("message");
        String from = rs.getString("from");
        String to = rs.getString("to");
        long timestamp = rs.getLong("timestamp");
        int fame = rs.getInt("fame");
        return new NoteEntity(id, message, from, to, timestamp, fame);
    }
}
