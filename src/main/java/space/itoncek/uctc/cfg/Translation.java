package space.itoncek.uctc.cfg;

import static net.kyori.adventure.text.format.NamedTextColor.*;

public enum Translation {
    NOT_WHITELISTED_TITLE(DARK_RED + "You are not whitelisted!"),
    NOT_WHITELISTED_SUBTITLE(DARK_RED + "Message admins your nick \"" + WHITE + "%s" + DARK_RED + "\" on discord!"),
    NOT_WHITELISTED_KICK_MESSAGE(RED + "You are not whitelisted!\nMessage admins your nick \"" + WHITE + "%s" + RED + "\" on discord!"),
    JDA_UNABLE_TO_CONNECT(DARK_RED + "JDA unable to connect"),
    JDA_SHUTDOWN_INTERRUPTED(DARK_RED + "JDA shutdown interrupted!"),
    JDA_PROCESSING_INTERRUPTED(DARK_RED + "JDA processing interrupted!"),
    MYSQL_UNABLE_TO_CONNECT(DARK_RED + "MySQL unable to connect"),
    MYSQL_ACCESSS_ERROR(DARK_RED + "MySQL DB Access Error!"),
    DBC_ALREADY_RUNNING(DARK_RED + "DBC Already running"),
    DBC_START_SUCCESS(GREEN + "DBC Started Succesfully"),
    DBC_STOP_SUCCESS(GREEN + "DBC Closed Succesfully"),
    DBC_CHANNEL_CREATED_SUCCESS(GREEN + "Channels Created Succesfully"),
    ;
    private final String def;

    Translation(String def) {
        this.def = def;
    }

    public String getDefault() {
        return def;
    }
}
