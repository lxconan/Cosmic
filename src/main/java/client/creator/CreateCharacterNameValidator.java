package client.creator;

import database.characters.CharacterDao;

import java.util.regex.Pattern;

public class CreateCharacterNameValidator {
    public static final CreateCharacterNameValidator instance = new CreateCharacterNameValidator();
    private static final String[] BLOCKED_NAMES = {"admin", "owner", "moderator", "intern", "donor", "administrator",
            "fredrick", "help", "helper", "alert", "notice", "maplestory", "fuck", "wizet", "fucking", "negro", "fuk",
            "fuc", "penis", "pussy", "asshole", "gay", "nigger", "homo", "suck", "cum", "shit", "shitty", "condom",
            "security", "official", "rape", "nigga", "sex", "tit", "boner", "orgy", "clit", "asshole", "fatass", "bitch",
            "support", "gamemaster", "cock", "gaay", "gm", "operate", "master", "sysop", "party", "GameMaster",
            "community", "message", "event", "test", "meso", "Scania", "yata", "AsiaSoft", "henesys"
    };

    private final CharacterDao characterDao = CharacterDao.instance;

    private CreateCharacterNameValidator() { }

    public boolean canCreateChar(String name) {
        String lowercaseName = name.toLowerCase();
        for (String nameTest : BLOCKED_NAMES) {
            if (lowercaseName.contains(nameTest)) {
                return false;
            }
        }
        return characterDao.getIdByName(name) < 0 && Pattern.compile("[a-zA-Z0-9]{3,12}").matcher(name).matches();
    }
}
