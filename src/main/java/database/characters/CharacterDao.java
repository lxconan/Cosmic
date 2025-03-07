package database.characters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CharacterDao {
    public static final CharacterDao instance = new CharacterDao();
    private static final Logger log = LoggerFactory.getLogger(CharacterDao.class);

    private CharacterDao() { }

    public int getAccountIdByName(String name) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            return handle.createQuery("SELECT accountid FROM characters WHERE name = ?")
                    .bind(0, name)
                    .mapTo(int.class)
                    .findOne()
                    .orElse(-1);
        } catch (JdbiException e) {
            log.warn("Failed to find account id by character name: {}", name, e);
            return -1;
        }
    }

    public int getIdByName(String name) {
        final int id;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id FROM characters WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return -1;
                }
                id = rs.getInt("id");
            }
            return id;
        } catch (Exception e) {
            log.warn("Failed to find character id by name: {}", name, e);
        }
        return -1;
    }

    public String getNameById(int id) {
        final String name;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT name FROM characters WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                name = rs.getString("name");
            }
            return name;
        } catch (Exception e) {
            log.warn("Failed to find character name by id: {}", id, e);
        }
        return null;
    }

    public void recordFameGivenLog(int fromCharacterId, int toCharacterId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO famelog (characterid, characterid_to) VALUES (?, ?)")) {
            ps.setInt(1, fromCharacterId);
            ps.setInt(2, toCharacterId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warn("Failed to record fame given log from {} to {}", fromCharacterId, toCharacterId, e);
        }
    }
}
