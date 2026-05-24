package io.github.naimjeg.damagenexus.command.test;

/** Every existing command preset that creates one test mob. */
public enum TestMobPreset {
    BASELINE("baseline", "[DN-Test] Baseline / No Armor"),
    ZOMBIE("zombie", "[DN-Test] Zombie"),
    COW("cow", "[DN-Test] Cow"),
    SPIDER("spider", "[DN-Test] Spider"),
    IRON("iron", "[DN-Test] Iron Armor"),
    DIAMOND("diamond", "[DN-Test] Diamond Armor"),
    NETHERITE_PROT(
            "netherite_prot",
            "[DN-Test] Netherite Prot IV"
    ),
    LOW_HP("low_hp", "[DN-Test] Overkill Cap / 5 HP"),
    INVUL("invul", "[DN-Test] Invul Delta / Fast Hit");

    private final String commandName;
    private final String displayName;

    TestMobPreset(String commandName, String displayName) {
        this.commandName = commandName;
        this.displayName = displayName;
    }

    public String commandName() {
        return commandName;
    }

    public String displayName() {
        return displayName;
    }
}
