/*######################
 # Copyright (c) 2023. #
 #                     #
 # Made by IToncek     #
 ######################*/

package space.itoncek.uctc;

import com.gmail.val59000mc.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import space.itoncek.uctc.cfg.CFGMGR;
import space.itoncek.uctc.cfg.Lang;

import java.sql.SQLException;
import java.util.logging.Level;

public final class UhcCoreTeamConfig extends JavaPlugin {
    public static GameManager gmmgr = null;
    public static UhcCoreTeamConfig pl;
    public static DiscordBotController dbc;
    public static Lang lng;
    public AutoAssigner assigner;

    @Override
    public void onEnable() {
        // Plugin startup logic
        pl = this;
        getServer().getPluginManager().registerEvents(new InstanceObtainer(), this);
        dbc = new DiscordBotController(CFGMGR.getConfig(getDataFolder()));
        assigner = new AutoAssigner(CFGMGR.getConfig(getDataFolder()).getString("dburl"));
        getCommand("discord").setExecutor(new DiscordCommand());
        getCommand("discord").setTabCompleter(new DiscordCommand());
        getServer().getPluginManager().registerEvents(assigner, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        try {
            assigner.close();
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
        try {
            dbc.close();
        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
