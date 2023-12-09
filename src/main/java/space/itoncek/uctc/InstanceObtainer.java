/*######################
 # Copyright (c) 2023. #
 #                     #
 # Made by IToncek     #
 ######################*/

package space.itoncek.uctc;

import com.gmail.val59000mc.events.UhcEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import static space.itoncek.uctc.UhcCoreTeamConfig.gmmgr;

public class InstanceObtainer implements Listener {
    @EventHandler
    public void onUhcEvent(UhcEvent event) {
        //Bukkit.getLogger().info("Obtained GameManager! Current state:" + event.getGameManager().getGameState());
        gmmgr = event.getGameManager();
    }
}
