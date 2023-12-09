/*######################
 # Copyright (c) 2023. #
 #                     #
 # Made by IToncek     #
 ######################*/

package space.itoncek.uctc.cfg;

import org.bukkit.Bukkit;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.logging.Level;

public class CFGMGR {
    public static boolean configNotPresent(File datadir) {
        return !new File(datadir + "/config.json").exists();
    }

    private static void dumpDefaultCFG(File datadir) {
        File cfg = new File(datadir + "/config.json");
        try (FileWriter fw = new FileWriter(cfg)) {
            fw.write(new JSONObject().put("dburl", "").put("dbcToken", new JSONArray().put("token1").put("token2")).put("dbcGuildID", 0L).put("mainVoice", 0L).put("dbcCategoryID", 0L).toString(4));
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private static void mkdirs(File datadir) {
        datadir.mkdirs();
    }

    public static JSONObject getConfig(File datadir) {
        if (configNotPresent(datadir)) {
            mkdirs(datadir);
            dumpDefaultCFG(datadir);
        }
        File cfg = new File(datadir + "/config.json");
        JSONObject raw = new JSONObject();

        try (Scanner sc = new Scanner(cfg)) {
            StringJoiner js = new StringJoiner("\n");
            while (sc.hasNextLine()) {
                js.add(sc.nextLine());
            }
            raw = new JSONObject(js.toString());
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
        return raw;
    }
}