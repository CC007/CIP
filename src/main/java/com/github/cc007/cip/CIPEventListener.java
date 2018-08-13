/*
 * The MIT License
 *
 * Copyright 2016 Rik Schaaf aka CC007 (http://coolcat007.nl/).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.cc007.cip;

import com.earth2me.essentials.PlayerExtension;
import com.earth2me.essentials.User;
import com.earth2me.essentials.UserMap;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

/**
 *
 * @author Rik Schaaf aka CC007 (http://coolcat007.nl/)
 */
public class CIPEventListener implements Listener {

    CIP plugin;

    public CIPEventListener(CIP plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(final PlayerLoginEvent event) {
        Thread t = new Thread() {

            @Override
            public void run() {
                Set<User> users = getAllUsersFromIp(event.getAddress());
                Set<Player> notifyAltsStaff = getOnlineStaff(CIPNotifyType.ALTS);
                String altsNotification = "";
                for (User user : users) {
                    if(!event.getPlayer().getName().equals(user.getName())){
                        altsNotification += "\n - "+ user.getName();
                    }
                }
                if(!"".equals(altsNotification)){
                    for (Player notifyAltsStaff1 : notifyAltsStaff) {
                        notifyAltsStaff1.sendMessage("Possible alt accounts:" + altsNotification);
                    }
                }
                boolean banned = false;
                User bannedUser = null;
                for (User user : users) {
                    if (Bukkit.getBanList(BanList.Type.NAME).getBanEntry(user.getName()) != null) {
                        banned = true;
                        bannedUser = user;
                        break;
                    }
                }
                if (banned) {
                    Set<Player> notifyBannedStaff = getOnlineStaff(CIPNotifyType.BANNED);
                    plugin.getLogger().log(Level.WARNING, "The player named {0} uses an IP address for which a user was previously banned!", event.getPlayer().getDisplayName());
                    for (Player staffMember : notifyBannedStaff) {
                        staffMember.sendMessage(ChatColor.GOLD + "The player named " + event.getPlayer().getDisplayName() + " uses an IP address for which a user was previously banned!");
                        staffMember.sendMessage(ChatColor.GOLD + "Name of banned player: " + bannedUser.getName());
                    }
                    switch (plugin.getConfig().getString("cip.bannedaction")) {
                        case "kick":
                            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                @Override
                                public void run() {
                                    event.getPlayer().kickPlayer("You are using an IP address for which a user was banned");
                                }
                            });
                            plugin.getLogger().log(Level.INFO, "The player has been kicked.");
                            for (Player staffMember : notifyBannedStaff) {
                                staffMember.sendMessage(ChatColor.GOLD + "The player has been kicked.");
                            }
                            break;
                        case "ban":
                            Calendar cal = new GregorianCalendar();
                            cal.add(Calendar.DAY_OF_MONTH, 7);
                            plugin.getServer().getBanList(BanList.Type.NAME).addBan(event.getPlayer().getName(), "Using an IP address for which a user was banned", cal.getTime(), null);
                            plugin.getLogger().log(Level.INFO, "The player has been banned.");
                            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                @Override
                                public void run() {
                                    event.getPlayer().kickPlayer("You are using an IP address for which a user was banned");
                                }
                            });
                            for (Player staffMember : notifyBannedStaff) {
                                staffMember.sendMessage(ChatColor.GOLD + "The player has been banned.");
                            }
                            break;
                        case "warn":
                            // just warining is enough
                            break;
                    }
                }
            }

        };
        t.start();
    }

    private Set<User> getAllUsersFromIp(InetAddress address) {
        Set<User> users = new HashSet<>();
        UserMap uMap = plugin.getEssentials().getUserMap();
        for (UUID userID : uMap.getAllUniqueUsers()) {
            User user = uMap.getUser(userID);
            if (user != null
                    && user.getBase() != null
                    && user.getName() != null
                    && plugin.getEssentials().getOfflineUser(user.getName()) != null
                    && plugin.getEssentials().getOfflineUser(user.getName()).getLastLoginAddress() != null
                    && plugin.getEssentials().getOfflineUser(user.getName()).getLastLoginAddress().equals(address.getHostAddress())) {
                users.add(user);
            } else if (user != null
                    && user.getBase() != null) {
            }
        }
        return users;
    }

    private Set<Player> getOnlineStaff(CIPNotifyType type) {
        Set<Player> staff = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            switch (type) {
                case ALTS:
                    if (player.hasPermission("cip.notifyalts")) {
                        staff.add(player);
                    }
                    break;
                case BANNED:
                    if (player.hasPermission("cip.notifybanned")) {
                        staff.add(player);
                    }
                    break;
            }
        }
        return staff;
    }
}
