/*
 * Made by IToncek
 *
 * Copyright (c) 2023.
 */

package space.itoncek.uctc;

import com.gmail.val59000mc.exceptions.UhcPlayerDoesNotExistException;
import com.gmail.val59000mc.exceptions.UhcTeamException;
import com.gmail.val59000mc.players.PlayerState;
import com.gmail.val59000mc.players.UhcPlayer;
import com.gmail.val59000mc.players.UhcTeam;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;


public class AutoAssigner implements Listener, AutoCloseable {
    private final Connection conn;

    public AutoAssigner(String url) {
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            handle(e);
            throw new RuntimeException("AutoAssignerError");
        }
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
                        UhcPlayer p = UhcCoreTeamConfig.gmmgr.getPlayerManager().getUhcPlayer(event.getPlayer().getName());
                        int teamID = rs.getInt("team");
                        if (teamID > 0) {
                            Statement stmt2 = conn.createStatement();
                            ResultSet fellows = stmt2.executeQuery("SELECT * FROM Players WHERE team = '%d'".formatted(rs.getInt("team")));
                            while (fellows.next()) {
                                if (!Objects.equals(fellows.getString("name"), p.getName()) && UhcCoreTeamConfig.gmmgr.getPlayerManager().getUhcPlayer(fellows.getString("name")).isOnline()) {
                                    UhcTeam team = UhcCoreTeamConfig.gmmgr.getPlayerManager().getUhcPlayer(fellows.getString("name")).getTeam();
                                    team.join(p);
                                    team.setTeamId(teamID);
                                }
                            }
                            fellows.close();
                            stmt2.close();
                            for (UhcPlayer uhcPlayer : UhcCoreTeamConfig.gmmgr.getPlayerManager().getPlayersList()) {
                                UhcCoreTeamConfig.gmmgr.getScoreboardManager().updatePlayerOnTab(uhcPlayer);
                            }
                        } else {
                            setPlayerSpectating(event.getPlayer(), p);
                        }
                    } else {
                        event.getPlayer().sendTitle(ChatColor.DARK_RED + "You are not whitelisted!",
                                ChatColor.DARK_RED + "Message admins your nick \"" + ChatColor.WHITE + event.getPlayer().getName() + ChatColor.DARK_RED + "\"!",
                                20,
                                200,
                                20);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                event.getPlayer().kick(Component.text(ChatColor.RED + "You are not whitelisted!    Message @itoncek your nick \"" + ChatColor.WHITE + event.getPlayer().getName() + ChatColor.RED + "\" on discord!"));
                            }
                        }.runTaskLater(UhcCoreTeamConfig.pl, 2L);
                    }
                    rs.close();
                    stmt.close();
                } catch (UhcPlayerDoesNotExistException | SQLException | UhcTeamException e) {
                    Bukkit.getLogger().log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }.runTaskLater(UhcCoreTeamConfig.pl, new Random().nextLong(1,20));
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
                UhcCoreTeamConfig.gmmgr.getScoreboardManager().updatePlayerOnTab(uhcPlayer);
                UhcCoreTeamConfig.gmmgr.getScoreboardManager().updateTeamOnTab(oldTeam);
            } catch (UhcTeamException e) {
                Bukkit.getLogger().log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }
}
