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
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import space.itoncek.uctc.cfg.CFGMGR;
import space.itoncek.uctc.cfg.Translation;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;
import java.util.logging.Level;

import static space.itoncek.uctc.UhcCoreTeamConfig.*;


public class AutoAssigner implements Listener, AutoCloseable {
    private final Connection conn;
    private final TreeSet<Request> assign = new TreeSet<>();
    private final BukkitTask runtime;

    public AutoAssigner(String url) {
        try {
            conn = DriverManager.getConnection(url);
            createTables(conn);
            runtime = startRuntime(conn);
        } catch (SQLException e) {
            handle(e);
            throw new RuntimeException(e);
        }
    }

    private BukkitTask startRuntime(Connection conn) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (!assign.isEmpty()) {
                    Request r = assign.first();
                    try {
                        if (conn.isClosed()) {
                            this.cancel();
                        }
                        UhcPlayer p = gmmgr.getPlayerManager().getUhcPlayer(r.name);
                        if (r.team > 0) {
                            Statement stmt2 = conn.createStatement();
                            ResultSet fellows = stmt2.executeQuery("SELECT * FROM Players WHERE team = '%d'".formatted(r.team));
                            int total = 0, failed = 0;
                            while (fellows.next()) {
                                try {
                                    if (!Objects.equals(fellows.getString("name"), p.getName()) && gmmgr.getPlayerManager().getUhcPlayer(fellows.getString("name")).isOnline()) {
                                        UhcTeam team = gmmgr.getPlayerManager().getUhcPlayer(fellows.getString("name")).getTeam();
                                        p.setTeam(team);
                                        team.getMembers().add(p);
                                        team.setTeamName("Team #" + r.team);
                                        failed = -99;
                                        total = 99;
                                        break;
                                    }
                                } catch (UhcPlayerDoesNotExistException e) {
                                    failed++;
                                }
                                ;
                                total++;
                            }

                            if (total == failed) {
                                p.sendMessage("Unable to find your teammate, please invite him to this server!");
                                p.getTeam().setTeamName("[INCOMPLETE] Team #" + r.team);
                            }

                            fellows.close();
                            stmt2.close();
                            for (UhcPlayer uhcPlayer : gmmgr.getPlayerManager().getPlayersList()) {
                                gmmgr.getScoreboardManager().updatePlayerOnTab(uhcPlayer);
                            }
                        } else {
                            setPlayerSpectating(r.p, p);
                        }
                    } catch (UhcPlayerDoesNotExistException | SQLException e) {
                        Bukkit.getLogger().log(Level.SEVERE, e.getMessage(), e);
                    }
                    assign.remove(r);
                }
            }
        }.runTaskTimer(pl,
                CFGMGR.getConfig(pl.getDataFolder()).getJSONObject("jdbc").getInt("databasePollLimit"),
                CFGMGR.getConfig(pl.getDataFolder()).getJSONObject("jdbc").getInt("databasePollLimit"));
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
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM Players WHERE name = '%s'".formatted(event.getPlayer().getName()));
            int teamid = rs.getInt("team");
            if (rs.next()) {
                assign.add(new Request(event.getPlayer().getName(), teamid, event.getPlayer(), LocalDateTime.now()));
            } else {
                event.getPlayer().sendTitle(lng.getTranslation(Translation.NOT_WHITELISTED_TITLE),
                        lng.getTranslation(Translation.NOT_WHITELISTED_SUBTITLE).formatted(event.getPlayer().getName()),
                        20,
                        200,
                        20);
                event.getPlayer().kick(Component.text(lng.getTranslation(Translation.NOT_WHITELISTED_KICK_MESSAGE)));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public void close() throws Exception {
        conn.close();
        runtime.cancel();
    }

    private void setPlayerSpectating(Player player, UhcPlayer uhcPlayer) {
        uhcPlayer.setState(PlayerState.DEAD);

        // Clear lobby items
        player.getInventory().clear();

        if (!uhcPlayer.getTeam().isSolo()) {
            try {
                UhcTeam oldTeam = uhcPlayer.getTeam();
                oldTeam.leave(uhcPlayer);
                oldTeam.setTeamName("Spectator " + Integer.toHexString(new Random().nextInt(0, 16777216)));
                gmmgr.getScoreboardManager().updatePlayerOnTab(uhcPlayer);
                gmmgr.getScoreboardManager().updateTeamOnTab(oldTeam);
            } catch (UhcTeamException e) {
                Bukkit.getLogger().log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    private record Request(String name, int team, Player p, LocalDateTime joinedAt) implements Comparable<Request> {
        @Override
        public int compareTo(@NotNull AutoAssigner.Request o) {
            return (int) (this.joinedAt.toEpochSecond(ZoneOffset.UTC) - o.joinedAt.toEpochSecond(ZoneOffset.UTC));
        }
    }
}