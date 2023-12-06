/*
 * Made by IToncek
 *
 * Copyright (c) 2023.
 */

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

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;


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
                            sender.sendMessage(ChatColor.DARK_RED + "JDA unable to connect");
                        } catch (SQLException e) {
                            Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
                            sender.sendMessage(ChatColor.DARK_RED + "MySQL unable to connect");
                        } catch (Exception e) {
                            Bukkit.getLogger().log(Level.INFO, e.getMessage());
                            sender.sendMessage(ChatColor.DARK_RED + "Bot Already running");
                        } finally {
                            sender.sendMessage(ChatColor.GREEN + "DBC Started Succesfully");
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
                            sender.sendMessage(ChatColor.DARK_RED + "MySQL DB Access Error!");
                        } catch (InterruptedException e) {
                            sender.sendMessage(ChatColor.DARK_RED + "JDA shutdown interrupted!");
                        } finally {
                            sender.sendMessage(ChatColor.GREEN + "DBC Closed Succesfully");
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
                            sender.sendMessage(ChatColor.DARK_RED + "MySQL DB Access Error!");
                        } catch (InterruptedException e) {
                            sender.sendMessage(ChatColor.DARK_RED + "JDA processing interrupted!");
                        } finally {
                            sender.sendMessage(ChatColor.GREEN + "Channels Created Succesfully");
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
