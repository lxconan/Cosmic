package client;

import tools.Pair;
import tools.Randomizer;

public final class CharacterCalculator {
    public static Pair<Integer, Integer> calculateJobChangeHpMpDelta(Job newJob) {
        int addhp = 0, addmp = 0;
        int job_ = newJob.getId() % 1000; // lame temp "fix"
        if (job_ == 100) {                      // 1st warrior
            addhp += Randomizer.rand(200, 250);
        } else if (job_ == 200) {               // 1st mage
            addmp += Randomizer.rand(100, 150);
        } else if (job_ % 100 == 0) {           // 1st others
            addhp += Randomizer.rand(100, 150);
            addmp += Randomizer.rand(25, 50);
        } else if (job_ > 0 && job_ < 200) {    // 2nd~4th warrior
            addhp += Randomizer.rand(300, 350);
        } else if (job_ < 300) {                // 2nd~4th mage
            addmp += Randomizer.rand(450, 500);
        } else if (job_ > 0) {                  // 2nd~4th others
            addhp += Randomizer.rand(300, 350);
            addmp += Randomizer.rand(150, 200);
        }

        return new Pair<>(addhp, addmp);
    }
}
