/*######################
 # Copyright (c) 2023. #
 #                     #
 # Made by IToncek     #
 ######################*/

package space.itoncek.uctc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.itoncek.uctc.cfg.Translation;
import space.itoncek.uctc.meta.AlreadyRunningException;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

import static space.itoncek.uctc.UhcCoreTeamConfig.lng;


public class DiscordCommand implements CommandExecutor, TabCompleter {
    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) return true;

        switch (args[0]) {
            case "startBot" -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            UhcCoreTeamConfig.dbc.connect();
                        } catch (InterruptedException e) {
                            Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
                            sender.sendMessage(lng.getTranslation(Translation.JDA_UNABLE_TO_CONNECT));
                        } catch (SQLException e) {
                            Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
                            sender.sendMessage(lng.getTranslation(Translation.MYSQL_UNABLE_TO_CONNECT));
                        } catch (AlreadyRunningException e) {
                            Bukkit.getLogger().log(Level.INFO, e.getMessage());
                            sender.sendMessage(lng.getTranslation(Translation.DBC_ALREADY_RUNNING));
                        } finally {
                            sender.sendMessage(lng.getTranslation(Translation.DBC_START_SUCCESS));
                            sender.sendMessage(" ");
                            sender.sendMessage(UhcCoreTeamConfig.dbc.getBotStatus());
                            sender.sendMessage(" ");
                        }
                    }
                }.runTaskAsynchronously(UhcCoreTeamConfig.pl);
            }
            case "stopBot" -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            UhcCoreTeamConfig.dbc.close();
                        } catch (SQLException e) {
                            Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
                            sender.sendMessage(lng.getTranslation(Translation.MYSQL_ACCESSS_ERROR));
                        } catch (InterruptedException e) {
                            sender.sendMessage(lng.getTranslation(Translation.JDA_SHUTDOWN_INTERRUPTED));
                        } finally {
                            sender.sendMessage(lng.getTranslation(Translation.DBC_STOP_SUCCESS));
                        }
                    }
                }.runTaskAsynchronously(UhcCoreTeamConfig.pl);
            }
            case "createChannels" -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            UhcCoreTeamConfig.dbc.createChannels();
                        } catch (SQLException e) {
                            sender.sendMessage(lng.getTranslation(Translation.MYSQL_ACCESSS_ERROR));
                        } catch (InterruptedException e) {
                            sender.sendMessage(lng.getTranslation(Translation.JDA_PROCESSING_INTERRUPTED));
                        } finally {
                            sender.sendMessage(lng.getTranslation(Translation.DBC_CHANNEL_CREATED_SUCCESS));
                        }
                    }
                }.runTaskAsynchronously(UhcCoreTeamConfig.pl);
            }
            case "destroyChannels" -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            UhcCoreTeamConfig.dbc.deleteChannels();
                        } catch (SQLException e) {
                            sender.sendMessage(ChatColor.DARK_RED + "MySQL DB Access Error!");
                        } catch (InterruptedException e) {
                            sender.sendMessage(ChatColor.DARK_RED + "JDA processing interrupted!");
                        } finally {
                            sender.sendMessage(ChatColor.GREEN + "Channels Deleted Succesfully");
                        }
                    }
                }.runTaskAsynchronously(UhcCoreTeamConfig.pl);
            }
            case "cleanupChannels" -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            UhcCoreTeamConfig.dbc.cleanUp();
                        } catch (InterruptedException e) {
                            sender.sendMessage(ChatColor.DARK_RED + "JDA processing interrupted!");
                        } finally {
                            sender.sendMessage(ChatColor.GREEN + "Channels Cleaned up Succesfully");
                        }
                    }
                }.runTaskAsynchronously(UhcCoreTeamConfig.pl);
            }
            case "moveAll" -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            UhcCoreTeamConfig.dbc.moveAll((s -> sender.sendMessage(ChatColor.DARK_RED + s)));
                        } catch (SQLException e) {
                            sender.sendMessage(ChatColor.DARK_RED + "MySQL DB Access Error!");
                        } catch (InterruptedException e) {
                            sender.sendMessage(ChatColor.DARK_RED + "JDA processing interrupted!");
                        } finally {
                            sender.sendMessage(ChatColor.GREEN + "Members moved succesfully");
                        }
                    }
                }.runTaskAsynchronously(UhcCoreTeamConfig.pl);
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) return List.of();
        return List.of("startBot", "stopBot", "createChannels", "destroyChannels", "cleanupChannels");
    }
}
