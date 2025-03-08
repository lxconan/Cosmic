package database.characters;

import client.Disease;
import server.life.MobSkill;
import server.life.MobSkillId;
import tools.DatabaseConnection;
import tools.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class PlayerDiseasesDao {
    public static final PlayerDiseasesDao instance = new PlayerDiseasesDao();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PlayerDiseasesDao.class);

    private PlayerDiseasesDao() {
    }

    public void deleteDiseasesForCharacter(int characterId) {
        log.trace("Deleting diseases for character id: {}", characterId);

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM playerdiseases WHERE charid = ?")) {
            ps.setInt(1, characterId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warn("Failed to delete diseases for character id: {}", characterId, e);
        }
    }

    public void insertPlayerDiseases(int characterId, Map<Disease, Pair<Long, MobSkill>> diseases) {
        log.trace("Inserting diseases for character id: {}", characterId);

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO playerdiseases (charid, disease, mobskillid, mobskilllv, length) VALUES (?, ?, ?, ?, ?)")) {
            for (Map.Entry<Disease, Pair<Long, MobSkill>> e : diseases.entrySet()) {
                ps.setInt(1, characterId);
                ps.setInt(2, e.getKey().ordinal());
                MobSkill ms = e.getValue().getRight();
                MobSkillId msId = ms.getId();
                ps.setInt(3, msId.type().getId());
                ps.setInt(4, msId.level());
                ps.setInt(5, e.getValue().getLeft().intValue());
                ps.addBatch();
            }

            ps.executeBatch();
        } catch (SQLException e) {
            log.warn("Failed to insert diseases for character id: {}", characterId, e);
        }
    }
}
