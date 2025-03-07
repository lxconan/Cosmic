package database.characters;

import net.server.PlayerCoolDownValueHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class CooldownsDao {
    public static final CooldownsDao instance = new CooldownsDao();
    private static final Logger log = LoggerFactory.getLogger(CooldownsDao.class);

    private CooldownsDao () { }

    public void deleteCooldownForCharacter(int characterId) {
        log.trace("Deleting cooldowns for character id: {}", characterId);

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM cooldowns WHERE charid = ?")) {
            ps.setInt(1, characterId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warn("Failed to delete cooldowns for character id: {}", characterId, e);
        }
    }

    public void insertPlayerCooldowns(int characterId, List<PlayerCoolDownValueHolder> cooldowns) {
        log.trace("Inserting cooldowns for character id: {}", characterId);

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO cooldowns (charid, skillid, starttime, length) VALUES (?, ?, ?, ?)")) {
            for (PlayerCoolDownValueHolder cooldown : cooldowns) {
                ps.setInt(1, characterId);
                ps.setInt(2, cooldown.skillId);
                ps.setLong(3, cooldown.startTime);
                ps.setLong(4, cooldown.length);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            log.warn("Failed to insert cooldowns for character id: {}", characterId, e);
        }
    }
}
