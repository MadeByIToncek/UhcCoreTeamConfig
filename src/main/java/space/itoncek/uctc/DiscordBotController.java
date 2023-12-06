/*
 * Made by IToncek
 *
 * Copyright (c) 2023.
 */

package space.itoncek.uctc;

import space.itoncek.uctc.meta.AlreadyRunningException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.bukkit.Bukkit;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.logging.Level;

import static org.bukkit.ChatColor.GREEN;
import static org.bukkit.ChatColor.WHITE;

public class DiscordBotController {
    private final long categorySnowFlake;
    private final List<String> token;
    private final long serverSnowFlake;
    private final String jdbc;
    private final List<JDA> jda;
    private JDA bot;
    private String bottoken;
    private Connection conn;
    private boolean initialized = false;
    private final long mainVoice;


    public DiscordBotController(JSONObject dbcConf) {
        token = new ArrayList<>();
        boolean first = true;
        for (Object dbcToken : dbcConf.getJSONArray("dbcToken")) {
            String token = (String) dbcToken;
            if (first) {
                first = false;
                bottoken = token;
            } else {
                this.token.add(token);
            }
        }
        this.serverSnowFlake = dbcConf.getLong("dbcGuildID");
        this.categorySnowFlake = dbcConf.getLong("dbcCategoryID");
        this.mainVoice = dbcConf.getLong("mainVoice");
        this.jdbc = dbcConf.getString("dburl");
        jda = new ArrayList<>();
    }

    public DiscordBotController(String token, List<String> supportBots, long serverSnowFlake, long categorySnowFlake, long mainVoice, String jdbc) {
        this.bottoken = token;
        this.token = supportBots;
        this.serverSnowFlake = serverSnowFlake;
        this.mainVoice = mainVoice;
        this.categorySnowFlake = categorySnowFlake;
        this.jdbc = jdbc;
        jda = new ArrayList<>();
    }

    /**
     * Connects to Discord API and MySQL
     *
     * @throws InterruptedException JDA failed to initialize
     * @throws SQLException         DB Access Error!
     * @implNote use in BukkitRunnable!
     */
    public void connect() throws InterruptedException, SQLException, AlreadyRunningException {
        if (!initialized) {
            conn = DriverManager.getConnection(jdbc);
            bot = JDABuilder
                    .createDefault(bottoken)
                    .setActivity(Activity.watching("CSYT OPEN!"))
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGES)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .build().awaitReady();
            for (String subToken : token) {
                jda.add(JDABuilder
                        .createDefault(subToken)
                        .setActivity(Activity.watching("CSYT OPEN!"))
                        .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGES)
                        .setMemberCachePolicy(MemberCachePolicy.ALL)
                        .build().awaitReady());
            }
            initialized = true;
        } else throw new AlreadyRunningException();
    }

    /**
     * Creates required channels to do its magic
     *
     * @throws SQLException DB Access Error!
     * @implNote use in BukkitRunnable!
     */
    public void createChannels() throws SQLException, InterruptedException {
        if (initialized) {
            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery("SELECT * FROM Players;");
            HashMap<Integer, List<IncompletePlayer>> requests = new HashMap<>();
            while (rs.next()) {
                if (!requests.containsKey(rs.getInt("team"))) requests.put(rs.getInt("team"), new ArrayList<>());
                requests.get(rs.getInt("team")).add(new IncompletePlayer(rs.getString("name"), rs.getLong("snowflake")));
            }
            requests.remove(-1);
            stmt.close();
            var ref = new Object() {
                final CountDownLatch latch = new CountDownLatch(requests.size());
            };
            List<JDA> superbots = new ArrayList<>(jda);
            superbots.add(bot);
            Random rnd = new SecureRandom();
            for (Map.Entry<Integer, List<IncompletePlayer>> entry : requests.entrySet()) {
                Thread t = new Thread(() -> {
                    JDA bot = superbots.get(rnd.nextInt(superbots.size()));
                    Integer integer = entry.getKey();
                    List<IncompletePlayer> incompletePlayers = entry.getValue();
                    Guild guild = bot.getGuildById(serverSnowFlake);

                    assert guild != null;
                    Role role = guild.createRole().setName("Team #" + integer).complete();
                    for (IncompletePlayer incompletePlayer : incompletePlayers) {
                        Member member = guild.getMemberById(incompletePlayer.snowflake);
                        if (member != null) {
                            guild.addRoleToMember(member, role).queue();
                        } else {
                            System.out.println("User not given role! " + guild.getMemberById(incompletePlayer.snowflake).getUser().getName() + " in team " + integer);
                        }
                    }
                    VoiceChannel voiceChannel = guild.getCategoryById(categorySnowFlake).createVoiceChannel("Team #" + integer).complete();
                    try {
                        Statement statement = conn.createStatement();
                        statement.executeUpdate("INSERT INTO DiscordChannelStorage (team, channelSnowflake, roleSnowflake) VALUES (%d, %d, %d);".formatted(integer, voiceChannel.getIdLong(), role.getIdLong()));
                        statement.close();
                    } catch (SQLException e) {
//                        e.printStackTrace();
                        Bukkit.getLogger().log(Level.SEVERE, e.getMessage(), e);
                    }
                    ref.latch.countDown();
                });
                t.start();
            }

            ref.latch.await();
            bot.getGuildById(serverSnowFlake).modifyVoiceChannelPositions(bot.getCategoryById(categorySnowFlake))
                    .sortOrder((o1, o2) -> {
                        int obj1 = Integer.parseInt(o1.getName().replace("Team #", ""));
                        int obj2 = Integer.parseInt(o2.getName().replace("Team #", ""));
                        return obj1 - obj2;
                    })
                    .complete();
        }
    }

    public void moveAll(Consumer<String> errorStream) throws SQLException, InterruptedException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM Players;");

        List<MoveAction> todo = new ArrayList<>();

        Guild g = bot.getGuildById(serverSnowFlake);
        while (rs.next()) {
            long userID = rs.getLong("Snowflake");
            if (rs.getLong("team") == -1) continue;

            if (!g.getMemberById(userID).getVoiceState().inAudioChannel()) {
                errorStream.accept("Player " + rs.getString("name") + " is not in voice channel, ignoring;");
                continue;
            }

            if (g.getMemberById(userID).getVoiceState().getChannel().asStageChannel().getIdLong() == mainVoice) {
                System.out.println("Moving " + rs.getString("name") + " to appropriate channel");
                ResultSet set = stmt.executeQuery("SELECT * FROM DiscordChannelStorage WHERE team = %d;");
                set.next();
                long vcid = set.getLong("channelSnowflake");
                todo.add(new MoveAction(userID, vcid));
            }
        }

        CountDownLatch latch = new CountDownLatch(todo.size());
        List<JDA> superbots = new ArrayList<>(jda);
        superbots.add(bot);
        Random rnd = new SecureRandom();

        for (MoveAction l : todo) {
            Thread t = new Thread(() -> {
                Guild gu = superbots.get(rnd.nextInt(superbots.size())).getGuildById(serverSnowFlake);
                VoiceChannel vc = gu.getVoiceChannelById(l.targetChannel);
                gu.moveVoiceMember(gu.getMemberById(l.userID), vc).complete();
            });
            t.start();
        }
        latch.await();

        stmt.close();
    }

    /**
     * Destroys previously created channels
     *
     * @throws SQLException DB Access Error!
     * @implNote use in BukkitRunnable!
     */
    public void deleteChannels() throws SQLException, InterruptedException {
        if (initialized) {
            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery("SELECT * FROM DiscordChannelStorage;");

            List<String> batchUpdate = new ArrayList<>();
            List<Long> deleteChannels = new ArrayList<>();
            List<Long> deleteRoles = new ArrayList<>();

            while (rs.next()) {
                deleteChannels.add(rs.getLong("channelSnowflake"));
                deleteRoles.add(rs.getLong("roleSnowflake"));
                batchUpdate.add("DELETE FROM `s4_csytopen`.`DiscordChannelStorage` WHERE  `team`=%d;".formatted(rs.getInt("team")));
            }
            var ref = new Object() {
                CountDownLatch latch = new CountDownLatch(deleteChannels.size());
            };
            List<JDA> superbots = new ArrayList<>(jda);
            superbots.add(bot);
            Random rnd = new SecureRandom();
            for (Long l : deleteChannels) {
                Thread t = new Thread(() -> {
                    superbots.get(rnd.nextInt(superbots.size())).getGuildById(serverSnowFlake).getVoiceChannelById(l).delete().complete();
                    ref.latch.countDown();
                });
                t.start();
            }
            ref.latch.await();

            ref.latch = new CountDownLatch(deleteRoles.size());
            for (Long l : deleteRoles) {
                Thread t = new Thread(() -> {
                    superbots.get(rnd.nextInt(superbots.size())).getGuildById(serverSnowFlake).getRoleById(l).delete().complete();
                    ref.latch.countDown();
                });
                t.start();
            }
            ref.latch.await();

            for (String sql : batchUpdate) stmt.executeUpdate(sql);
            stmt.close();
        }
    }

    /**
     * Destroys channels and roles starting with 'Team #'
     *
     * @implNote use in BukkitRunnable!
     */
    public void cleanUp() throws InterruptedException {
        Guild guild = bot.getGuildById(serverSnowFlake);
        List<Long> vcToDelete = new ArrayList<>();
        List<Long> roleToDelete = new ArrayList<>();
        assert guild != null;
        for (VoiceChannel voiceChannel : guild.getVoiceChannels())
            if (voiceChannel.getName().startsWith("Team #")) vcToDelete.add(voiceChannel.getIdLong());
        for (Role role : guild.getRoles()) if (role.getName().startsWith("Team #")) roleToDelete.add(role.getIdLong());

        var ref = new Object() {
            CountDownLatch latch = new CountDownLatch(vcToDelete.size());
        };
        List<JDA> superbots = new ArrayList<>(jda);
        superbots.add(bot);
        Random rnd = new SecureRandom();
        for (Long l : vcToDelete) {
            Thread t = new Thread(() -> {
                superbots.get(rnd.nextInt(superbots.size())).getGuildById(serverSnowFlake).getVoiceChannelById(l).delete().complete();
                ref.latch.countDown();
            });
            t.start();
        }
        ref.latch.await();

        ref.latch = new CountDownLatch(roleToDelete.size());
        for (Long l : roleToDelete) {
            Thread t = new Thread(() -> {
                superbots.get(rnd.nextInt(superbots.size())).getGuildById(serverSnowFlake).getRoleById(l).delete().complete();
                ref.latch.countDown();
            });
            t.start();
        }
        ref.latch.await();
    }

    public void close() throws SQLException, InterruptedException {
        if (initialized) {
            conn.close();
            for (JDA bot : jda) {
                bot.shutdown();
            }
            bot.shutdown();
        }
        conn = null;
        jda.clear();
        initialized = false;
    }

    public String getBotStatus() {
        if (initialized) {
            StringJoiner js = new StringJoiner("\n");
            js.add(WHITE + "______________________________");
            int i = 0;
            for (JDA bot : jda) {
                js.add(GREEN + "Bot %d name: ".formatted(i) + WHITE + bot.getSelfUser().getName());
                i++;
            }
            js.add(WHITE + "______________________________");
            return js.toString();
        } else return "";
    }


    private record IncompletePlayer(String name, long snowflake) {
    }

    private record MoveAction(long userID, long targetChannel) {
    }
}
