package database.characters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;

public class CharacterDao {
    public static final CharacterDao instance = new CharacterDao();
    private static final Logger log = LoggerFactory.getLogger(CharacterDao.class);

    public int getAccountIdByName(String name) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            return handle.createQuery("SELECT accountid FROM characters WHERE name = ?")
                    .bind(0, name)
                    .mapTo(int.class)
                    .findOne()
                    .orElse(-1);
        } catch (JdbiException e) {
            log.warn("Failed to find accountid by character name: {}", name, e);
            return -1;
        }
    }
}
