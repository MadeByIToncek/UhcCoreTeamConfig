/*
 * Made by IToncek
 *
 * Copyright (c) 2023.
 */

package space.itoncek.uctc;

import com.gmail.val59000mc.events.UhcEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import static space.itoncek.uctc.UhcCoreTeamConfig.*;
public class InstanceObtainer implements Listener {
    @EventHandler
    public void onUhcEvent(UhcEvent event) {
        //Bukkit.getLogger().info("Obtained GameManager! Current state:" + event.getGameManager().getGameState());
        gmmgr = event.getGameManager();
    }
}
