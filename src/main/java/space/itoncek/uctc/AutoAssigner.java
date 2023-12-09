/*######################
 # Copyright (c) 2023. #
 #                     #
 # Made by IToncek     #
 ######################*/

package space.itoncek.uctc;

import com.gmail.val59000mc.exceptions.UhcPlayerDoesNotExistException;
import com.gmail.val59000mc.exceptions.UhcTeamException;
import com.gmail.val59000mc.players.PlayerState;
import com.gmail.val59000mc.players.UhcPlayer;
import com.gmail.val59000mc.players.UhcTeam;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import space.itoncek.uctc.cfg.Translation;

import java.sql.*;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;

import static space.itoncek.uctc.UhcCoreTeamConfig.*;


public class AutoAssigner implements Listener, AutoCloseable {
    private final Connection conn;

    public AutoAssigner(String url) {
        try {
            conn = DriverManager.getConnection(url);
            createTables(conn);
        } catch (SQLException e) {
            handle(e);
            throw new RuntimeException(e);
        }
    }

    /*
    CREATE TABLE IF NOT EXISTS `DiscordChannelStorage` (
            `team` int(11) NOT NULL,
            `channelSnowflake` bigint(20) unsigned NOT NULL,
            `roleSnowflake` bigint(20) unsigned NOT NULL,
        PRIMARY KEY (`team`)
    );

    CREATE TABLE IF NOT EXISTS `Players` (
            `name` varchar(32) NOT NULL,
            `team` int(10) NOT NULL DEFAULT 0,
            `snowflake` bigint(20) NOT NULL,
        PRIMARY KEY (`name`)
    );

     */
    private void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `Players` (
                        `name` varchar(32) NOT NULL,
                        `team` int(10) NOT NULL DEFAULT 0,
                        `snowflake` bigint(20) NOT NULL,
                    PRIMARY KEY (`name`)
                );""");
        stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `DiscordChannelStorage` (
                        `team` int(11) NOT NULL,
                        `channelSnowflake` bigint(20) unsigned NOT NULL,
                        `roleSnowflake` bigint(20) unsigned NOT NULL,
                    PRIMARY KEY (`team`)
                );""");
    }

    private void handle(Exception e) {
        Bukkit.getLogger().log(Level.SEVERE, e.getMessage(), e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM Players WHERE name = '%s'".formatted(event.getPlayer().getName()));

                    if (rs.next()) {
                        UhcPlayer p = gmmgr.getPlayerManager().getUhcPlayer(event.getPlayer().getName());
                        int teamID = rs.getInt("team");
                        if (teamID > 0) {
                            Statement stmt2 = conn.createStatement();
                            ResultSet fellows = stmt2.executeQuery("SELECT * FROM Players WHERE team = '%d'".formatted(rs.getInt("team")));
                            while (fellows.next()) {
                                if (!Objects.equals(fellows.getString("name"), p.getName()) && gmmgr.getPlayerManager().getUhcPlayer(fellows.getString("name")).isOnline()) {
                                    UhcTeam team = gmmgr.getPlayerManager().getUhcPlayer(fellows.getString("name")).getTeam();
                                    team.join(p);
                                    team.setTeamId(teamID);
                                }
                            }
                            fellows.close();
                            stmt2.close();
                            for (UhcPlayer uhcPlayer : gmmgr.getPlayerManager().getPlayersList()) {
                                gmmgr.getScoreboardManager().updatePlayerOnTab(uhcPlayer);
                            }
                        } else {
                            setPlayerSpectating(event.getPlayer(), p);
                        }
                    } else {
                        event.getPlayer().sendTitle(lng.getTranslation(Translation.NOT_WHITELISTED_TITLE),
                                lng.getTranslation(Translation.NOT_WHITELISTED_SUBTITLE).formatted(event.getPlayer().getName()),
                                20,
                                200,
                                20);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                event.getPlayer().kick(Component.text(lng.getTranslation(Translation.NOT_WHITELISTED_KICK_MESSAGE)));
                            }
                        }.runTaskLater(pl, 2L);
                    }
                    rs.close();
                    stmt.close();
                } catch (UhcPlayerDoesNotExistException | SQLException | UhcTeamException e) {
                    Bukkit.getLogger().log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }.runTaskLater(pl, new Random().nextLong(1, 20));
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }

    private void setPlayerSpectating(Player player, UhcPlayer uhcPlayer) {
        uhcPlayer.setState(PlayerState.DEAD);

        // Clear lobby items
        player.getInventory().clear();

        if (!uhcPlayer.getTeam().isSolo()) {
            try {
                UhcTeam oldTeam = uhcPlayer.getTeam();
                oldTeam.leave(uhcPlayer);
                oldTeam.setTeamId(new Random().nextInt(99, 9999));
                gmmgr.getScoreboardManager().updatePlayerOnTab(uhcPlayer);
                gmmgr.getScoreboardManager().updateTeamOnTab(oldTeam);
            } catch (UhcTeamException e) {
                Bukkit.getLogger().log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }
}