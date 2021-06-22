package xyz.n7mn.dev.survivalplugin.listener;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import xyz.n7mn.dev.survivalplugin.data.LockCommandUser;
import xyz.n7mn.dev.survivalplugin.event.DiscordonMessageReceivedEvent;
import xyz.n7mn.dev.survivalplugin.function.Lati2Hira;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class EventListener implements Listener {

    private final Plugin plugin;
    private final JDA jda;
    private List<LockCommandUser> lockUserList;

    public EventListener(Plugin plugin, JDA jda, List<LockCommandUser> lockUserList){
        this.plugin = plugin;
        this.jda = jda;

        this.lockUserList = lockUserList;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void PlayerJoinEvent (PlayerJoinEvent e){
        e.joinMessage(Component.text(""));

        new Thread(()->{
            try {
                boolean found = false;
                Enumeration<Driver> drivers = DriverManager.getDrivers();

                while (drivers.hasMoreElements()){
                    Driver driver = drivers.nextElement();
                    if (driver.equals(new com.mysql.cj.jdbc.Driver())){
                        found = true;
                        break;
                    }
                }

                if (!found){
                    DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
                }

                Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));

                PreparedStatement statement = con.prepareStatement("SELECT * FROM SurvivalUser WHERE UUID = ?");
                statement.setString(1, e.getPlayer().getUniqueId().toString());

                ResultSet set = statement.executeQuery();
                if (set.next()){
                    for (Player player : plugin.getServer().getOnlinePlayers()){
                        if (!player.isOp()){
                            player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] "+ChatColor.RESET+e.getPlayer().getName()+"さんが入室しました！");
                        }
                    }
                    long count = set.getLong("Count");
                    set.close();
                    statement.close();
                    count++;

                    PreparedStatement statement1 = con.prepareStatement("UPDATE SurvivalUser SET LastJoinDate = NOW(), Count = ? WHERE UUID = ?");
                    statement1.setLong(1, count);
                    statement1.setString(2, e.getPlayer().getUniqueId().toString());
                    statement1.execute();

                    statement1.close();
                    con.close();
                    return;
                }

                statement.close();
                for (Player player : plugin.getServer().getOnlinePlayers()){
                    player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] "+ChatColor.RESET+e.getPlayer().getName()+"さんが新規に入室しました！ゆっくりしていってね！");
                }

                PreparedStatement statement1 = con.prepareStatement("INSERT INTO `SurvivalUser`(`UUID`, `FirstJoinDate`, `LastJoinDate`, `RoleID`, `Count`) VALUES (?,NOW(),NOW(),'',1)");
                statement1.setString(1, e.getPlayer().getUniqueId().toString());
                statement1.execute();
                statement1.close();

                con.close();
            } catch (Exception ex){
                ex.printStackTrace();
            }

        }).start();

        new Thread(()->{
            if (jda != null && jda.getStatus() == JDA.Status.CONNECTED){
                // plugin.getLogger().info("test");
                TextChannel channel = jda.getTextChannelById(plugin.getConfig().getString("NotificationChannel"));

                channel.getHistoryAfter(1, 100).queue(messageHistory -> {
                    List<Message> list = messageHistory.getRetrievedHistory();
                    TextComponent component = Component.text(ChatColor.YELLOW + "[ななみ生活鯖] "+ChatColor.UNDERLINE+list.size()+"件のおしらせ"+ChatColor.RESET+"があります。");

                    TextComponent component1 = Component.text("[確認する]");
                    component1 = component1.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/noti"));

                    e.getPlayer().sendMessage(component);
                    e.getPlayer().sendMessage(component1);
                });
            }
        }).start();

        new Thread(()->{
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(e.getPlayer().getName()+"さんが入室しました。");
            builder.setColor(java.awt.Color.GREEN);
            builder.setDescription("現在" + plugin.getServer().getOnlinePlayers().size()+"人です。");

            jda.getTextChannelById(plugin.getConfig().getString("ChatChannel")).sendMessage(builder.build()).queue();
        }).start();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void PlayerQuitEvent(PlayerQuitEvent e){
        e.quitMessage(Component.text(""));

        new Thread(()->{
            for (Player player : plugin.getServer().getOnlinePlayers()){
                if (!player.isOp()){
                    player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] "+ChatColor.RESET+e.getPlayer().getName()+"さんが退出しました！");
                }
            }
        }).start();

        new Thread(()->{
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(e.getPlayer().getName()+"さんが退出しました。");
            builder.setColor(java.awt.Color.RED);
            builder.setDescription("現在" + (plugin.getServer().getOnlinePlayers().size() - 1) + "人です。");

            jda.getTextChannelById(plugin.getConfig().getString("ChatChannel")).sendMessage(builder.build()).queue();
        }).start();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void AsyncChatEvent (AsyncChatEvent e){
        // plugin.getLogger().info("test");
        Component message = e.message();
        TextComponent m = (TextComponent) message;

        String s = m.content();
        if (s.length() != s.getBytes(StandardCharsets.UTF_8).length){

            String finalS = s;
            new Thread(()->{
                EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle(e.getPlayer().getName());
                builder.setColor(java.awt.Color.ORANGE);
                builder.setDescription(finalS);

                jda.getTextChannelById(plugin.getConfig().getString("ChatChannel")).sendMessage(builder.build()).queue();
            }).start();
            return;
        }
        s = Lati2Hira.parse(s);

        StringBuffer sb = new StringBuffer();
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("http://www.google.com/transliterate?langpair=ja-Hira|ja&text="+ URLEncoder.encode(s, "UTF-8"))
                    .build();

            Response response = client.newCall(request).execute();
            String MojiCode = "UTF-8";
            if (System.getProperty("os.name").toLowerCase().startsWith("windows")){
                MojiCode = "windows-31j";
            }
            String RequestText = new String(response.body().string().getBytes(),MojiCode);

            for ( JsonElement jsonElements : new Gson().fromJson(RequestText, JsonArray.class)){
                sb.append(jsonElements.getAsJsonArray().get(1).getAsJsonArray().get(0).getAsString());
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }


        String msg = sb.toString() + " ("+m.content()+")";
        e.message(Component.text(msg));

        new Thread(()->{
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(e.getPlayer().getName());
            builder.setColor(java.awt.Color.ORANGE);
            builder.setDescription(msg);

            jda.getTextChannelById(plugin.getConfig().getString("ChatChannel")).sendMessage(builder.build()).queue();
        }).start();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void InventoryOpenEvent(InventoryOpenEvent e){

        if (e.getInventory().getType() == InventoryType.CHEST){
            boolean isFound = false;
            boolean isAdd = false;

            LockCommandUser u = null;
            int i = 0;
            for (LockCommandUser user : lockUserList){
                if (e.getPlayer().getUniqueId().equals(user.getUserUUID())){
                    isAdd = user.isAdd();
                    isFound = true;
                    u = user;
                    break;
                }
                i++;
            }

            if (u != null){
                lockUserList.remove(i);
            }

            Location location = e.getInventory().getLocation();
            Chest chest = (Chest) location.getBlock().getState();

            if (isFound){
                e.getView().close();
                e.getPlayer().closeInventory();
                e.setCancelled(true);

                UUID chestID;
                try {
                    Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));

                    PreparedStatement statement = con.prepareStatement("SELECT * FROM LockUUIDList WHERE Active = 1 AND world = ? AND x = ? AND y = ? AND z = ?");
                    statement.setString(1, chest.getLocation().getWorld().getUID().toString());
                    statement.setInt(2, chest.getLocation().getBlockX());
                    statement.setInt(3, chest.getLocation().getBlockY());
                    statement.setInt(4, chest.getLocation().getBlockZ());
                    ResultSet set = statement.executeQuery();
                    if (set.next()){
                        chestID = UUID.fromString(set.getString("UUID"));
                    } else {
                        chestID = UUID.randomUUID();

                        PreparedStatement statement1 = con.prepareStatement("INSERT INTO `LockUUIDList`(`UUID`, `world`, `x`, `y`, `z`, `Active`) VALUES (?,?,?,?,?,?)");
                        statement1.setString(1, chestID.toString());
                        statement1.setString(2, chest.getLocation().getWorld().getUID().toString());
                        statement1.setInt(3, chest.getLocation().getBlockX());
                        statement1.setInt(4, chest.getLocation().getBlockY());
                        statement1.setInt(5, chest.getLocation().getBlockZ());
                        statement1.setBoolean(6, true);
                        statement1.execute();
                        statement1.close();
                    }
                    set.close();
                    statement.close();

                    con.close();
                } catch (SQLException ex){
                    ex.printStackTrace();
                    e.getPlayer().sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "エラーが発生しました。もう一度実行してもエラーになる場合は運営に教えてください。");
                    return;
                }

                // ロック追加 or 解除処理
                try {
                    Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));

                    PreparedStatement statement = con.prepareStatement("SELECT * FROM LockList WHERE BlockID = ? AND Active = 1");
                    statement.setString(1, chestID.toString());
                    ResultSet set = statement.executeQuery();


                    if (isAdd){
                        // 追加
                        UUID addUser = u.getAddUser();
                        UUID userUUID = u.getUserUUID();

                        boolean check = false;
                        boolean isParent = false;
                        boolean isAddCheck = true;
                        while (set.next()){
                            check = true;
                            if (userUUID.toString().equals(set.getString("MinecraftUserID"))){
                                isParent = set.getBoolean("IsParent");
                                break;
                            }

                            if (addUser != null && addUser.toString().equals(set.getString("MinecraftUserID"))){
                                isAddCheck = false;
                            }
                        }

                        set.close();
                        statement.close();

                        // System.out.println("check " + check);
                        // System.out.println("isParent" + isParent);
                        if (check && !isParent && addUser != null){
                            e.getPlayer().sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "保護を追加した人しか保護追加できません。");
                            con.close();
                            return;
                        }

                        if (check && addUser == null){
                            e.getPlayer().sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "すでに登録されています。");
                            con.close();
                            return;
                        }

                        if (check && !isAddCheck){
                            e.getPlayer().sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "すでに登録されています。");
                            con.close();
                            return;
                        }

                        if (!check && addUser != null){
                            e.getPlayer().sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "保護されていないので追加登録できません。");
                            con.close();
                            return;
                        }

                        new Thread(()->{
                            try {
                                PreparedStatement statement1 = con.prepareStatement("INSERT INTO `LockList`(`UUID`, `BlockID`, `BlockType`, `MinecraftUserID`, `IsParent`, `Active`) VALUES (?,?,?,?,?,?)");
                                statement1.setString(1, UUID.randomUUID().toString());
                                statement1.setString(2, chestID.toString());
                                statement1.setString(3, chest.getType().name());
                                if (addUser == null){
                                    statement1.setString(4, userUUID.toString());
                                    statement1.setBoolean(5, true);
                                } else {
                                    statement1.setString(4, addUser.toString());
                                    statement1.setBoolean(5, false);
                                }
                                statement1.setBoolean(6, true);
                                statement1.execute();
                                statement1.close();
                                con.close();
                            } catch (SQLException ex){
                                ex.printStackTrace();
                            }

                        }).start();

                        if (addUser != null){
                            e.getPlayer().sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "保護チェストに追加登録が完了しました。");
                        } else {
                            e.getPlayer().sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "チェストを保護しました。");
                        }

                        return;
                    }
                    // 削除
                    UUID delUser = u.getAddUser();
                    UUID userUUID = u.getUserUUID();

                    boolean check = false;
                    boolean isParent = false;
                    boolean isDelCheck = true;
                    while (set.next()){
                        check = true;
                        if (delUser != null && delUser.toString().equals(set.getString("MinecraftUserID"))){
                            isDelCheck = false;
                        }

                        if (userUUID.toString().equals(set.getString("MinecraftUserID"))){
                            isParent = set.getBoolean("IsParent");
                            break;
                        }

                    }

                    set.close();
                    statement.close();

                    if (check && !isParent && delUser != null){
                        e.getPlayer().sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "保護を追加した人しか保護削除できません。");
                        con.close();
                        return;
                    }

                    if (check && !isParent){
                        //plugin.getLogger().info("a");
                        e.getPlayer().sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "すでに解除されています。");
                        con.close();
                        return;
                    }

                    if (check && !isDelCheck){
                        //plugin.getLogger().info("b");
                        e.getPlayer().sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "すでに解除されています。");
                        con.close();
                        return;
                    }

                    if (!check){
                        e.getPlayer().sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "すでに解除されています。");
                        con.close();
                        return;
                    }

                    new Thread(()->{
                        try {
                            PreparedStatement statement1 = con.prepareStatement("UPDATE `LockList` SET `Active` = ? WHERE BlockID = ? AND MinecraftUserID = ?");
                            statement1.setBoolean(1, false);
                            statement1.setString(2, chestID.toString());
                            if (delUser == null){
                                statement1.setString(3, userUUID.toString());
                            } else {
                                statement1.setString(3, delUser.toString());
                            }
                            statement1.execute();
                            statement1.close();
                            con.close();
                        } catch (SQLException ex){
                            ex.printStackTrace();
                        }

                    }).start();

                    if (delUser != null){
                        e.getPlayer().sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "保護チェストに登録解除が完了しました。");
                    } else {
                        e.getPlayer().sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "チェストを保護解除しました。");
                    }
                } catch (SQLException ex){
                    ex.printStackTrace();
                }
                return;
            }
            // ロックチェック
            UUID UserUUID = e.getPlayer().getUniqueId();
            UUID chestID;
            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));

                PreparedStatement statement = con.prepareStatement("SELECT * FROM LockUUIDList WHERE Active = 1 AND world = ? AND x = ? AND y = ? AND z = ?");
                statement.setString(1, chest.getLocation().getWorld().getUID().toString());
                statement.setInt(2, chest.getLocation().getBlockX());
                statement.setInt(3, chest.getLocation().getBlockY());
                statement.setInt(4, chest.getLocation().getBlockZ());
                ResultSet set = statement.executeQuery();
                if (set.next()){
                    chestID = UUID.fromString(set.getString("UUID"));
                } else {
                    chestID = UUID.randomUUID();

                    PreparedStatement statement1 = con.prepareStatement("INSERT INTO `DeathList`(`UUID`, `MinecraftUUID`, `world`, `x`, `y`, `z`, `Active`) VALUES (?,?,?,?,?,?,?)");
                    statement1.setString(1, chestID.toString());
                    statement1.setString(2, e.getPlayer().getUniqueId().toString());
                    statement1.setString(3, chest.getLocation().getWorld().getUID().toString());
                    statement1.setInt(4, chest.getLocation().getBlockX());
                    statement1.setInt(5, chest.getLocation().getBlockY());
                    statement1.setInt(6, chest.getLocation().getBlockZ());
                    statement1.setBoolean(7, true);
                    statement1.execute();
                    statement1.close();
                }
                set.close();
                statement.close();

                con.close();
            } catch (SQLException ex){
                ex.printStackTrace();
                return;
            }

            try {

                Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));

                PreparedStatement statement = con.prepareStatement("SELECT * FROM LockList WHERE BlockID = ? AND Active = 1");
                statement.setString(1, chestID.toString());
                ResultSet set = statement.executeQuery();

                boolean isCheck = false;
                boolean isFoundData = false;
                while (set.next()){
                    isFoundData = true;
                    if (UserUUID.toString().equals(set.getString("MinecraftUserID"))){
                        isCheck = true;
                        break;
                    }
                }

                if (!isCheck && isFoundData){
                    e.getView().close();
                    e.getPlayer().closeInventory();
                    e.setCancelled(true);
                    e.getPlayer().sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "このチェストは保護されています。");
                }

                set.close();
                statement.close();
                con.close();

            } catch (SQLException ex){
                ex.printStackTrace();
            }
            return;
        }

        if (e.getInventory().getType() == InventoryType.SHULKER_BOX){
            boolean isFound = false;
            boolean isAdd = false;

            LockCommandUser u = null;
            int i = 0;
            for (LockCommandUser user : lockUserList){
                if (e.getPlayer().getUniqueId().equals(user.getUserUUID())){
                    isAdd = user.isAdd();
                    isFound = true;
                    u = user;
                    break;
                }
                i++;
            }

            if (u != null){
                lockUserList.remove(i);
            }

            Location location = e.getInventory().getLocation();
            ShulkerBox box = (ShulkerBox) location.getBlock().getState();

            UUID uuid;
            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));

                PreparedStatement statement = con.prepareStatement("SELECT * FROM LockUUIDList WHERE Active = 1 AND world = ? AND x = ? AND y = ? AND z = ?");
                statement.setString(1, box.getLocation().getWorld().getUID().toString());
                statement.setInt(2, box.getLocation().getBlockX());
                statement.setInt(3, box.getLocation().getBlockY());
                statement.setInt(4, box.getLocation().getBlockZ());
                ResultSet set = statement.executeQuery();
                if (set.next()){
                    uuid = UUID.fromString(set.getString("UUID"));
                } else {
                    uuid = UUID.randomUUID();

                    PreparedStatement statement1 = con.prepareStatement("INSERT INTO `LockUUIDList`(`UUID`, `world`, `x`, `y`, `z`, `Active`) VALUES (?,?,?,?,?,?)");
                    statement1.setString(1, uuid.toString());
                    statement1.setString(2, box.getLocation().getWorld().getUID().toString());
                    statement1.setInt(3, box.getLocation().getBlockX());
                    statement1.setInt(4, box.getLocation().getBlockY());
                    statement1.setInt(5, box.getLocation().getBlockZ());
                    statement1.setBoolean(6, true);
                    statement1.execute();
                    statement1.close();
                    plugin.getLogger().info("test");
                }
                set.close();
                statement.close();
                con.close();
            } catch (SQLException ex){
                ex.printStackTrace();
                return;
            }

            plugin.getLogger().info("UUID : " + uuid.toString());

            if (isFound) {
                e.getView().close();
                e.getPlayer().closeInventory();
                e.setCancelled(true);
                HumanEntity player = e.getPlayer();

                if (isAdd){
                    // 追加
                    try {
                        Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));
                        PreparedStatement statement = con.prepareStatement("SELECT * FROM LockList WHERE BlockID = ? AND Active = 1");
                        statement.setString(1, uuid.toString());
                        ResultSet set = statement.executeQuery();
                        boolean result = false;
                        boolean resultFound = false;
                        while (set.next()){
                            resultFound = true;
                            if (player.getUniqueId().toString().equals(set.getString("MinecraftUserID")) && set.getBoolean("IsParent")){
                                result = true;
                            }
                        }
                        set.close();
                        statement.close();

                        if (!result && resultFound){
                            player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] "+ChatColor.RESET+"他の人が保護しているシュルカーボックスです。");
                            con.close();
                            return;
                        }

                        if (resultFound){
                            player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] "+ChatColor.RESET+"すでに保護されているシュルカーボックスです");
                            con.close();
                            return;
                        }

                        player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] "+ChatColor.RESET+"シュルカーボックスを保護しました。");
                        PreparedStatement statement1 = con.prepareStatement("INSERT INTO `LockList`(`UUID`, `BlockID`, `BlockType`, `MinecraftUserID`, `IsParent`, `Active`) VALUES (?,?,?,?,?,?)");
                        statement1.setString(1, UUID.randomUUID().toString());
                        statement1.setString(2, uuid.toString());
                        statement1.setString(3, box.getBlock().getType().name());
                        statement1.setString(4, player.getUniqueId().toString());
                        statement1.setBoolean(5, true);
                        statement1.setBoolean(6, true);
                        statement1.execute();
                        statement1.close();
                        con.close();

                    } catch (SQLException ex){
                        ex.printStackTrace();
                    }
                    return;
                }

                // 削除
                try {
                    Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));
                    PreparedStatement statement = con.prepareStatement("SELECT * FROM LockList WHERE BlockID = ? AND Active = 1");
                    statement.setString(1, uuid.toString());
                    ResultSet set = statement.executeQuery();
                    boolean result = false;
                    while (set.next()){
                        if (player.getUniqueId().toString().equals(set.getString("MinecraftUserID")) && set.getBoolean("IsParent")){
                            result = true;
                        }
                    }
                    set.close();
                    statement.close();

                    if (!result){
                        player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] "+ChatColor.RESET+"他の人が保護しているシュルカーボックスです。");
                        con.close();
                        return;
                    }

                    player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] "+ChatColor.RESET+"シュルカーボックスを保護解除しました。");
                    PreparedStatement statement1 = con.prepareStatement("UPDATE `LockList` SET `Active` = 0 WHERE BlockID = ?");
                    statement1.setString(1, uuid.toString());
                    statement1.execute();
                    statement1.close();
                    con.close();

                } catch (SQLException ex){
                    ex.printStackTrace();
                }
                return;
            }

            // 保護チェック
            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));
                PreparedStatement statement = con.prepareStatement("SELECT * FROM LockList WHERE BlockID = ? AND Active = 1");
                statement.setString(1, uuid.toString());
                ResultSet set = statement.executeQuery();

                boolean result = false;
                while (set.next()){
                    result = true;
                    if (e.getPlayer().getUniqueId().toString().equals(set.getString("MinecraftUserID"))){
                        set.close();
                        statement.close();
                        con.close();
                        return;
                    }
                }
                set.close();
                statement.close();
                con.close();

                if (!result){
                    return;
                }
            } catch (SQLException ex){
                ex.printStackTrace();
            }

            e.getPlayer().sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] "+ChatColor.RESET+"他の人が保護しているシュルカーボックスです。");
            e.getView().close();
            e.getPlayer().closeInventory();
            e.setCancelled(true);

        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void DiscordOnMessageReceivedEvent (DiscordonMessageReceivedEvent e){
        MessageReceivedEvent event = e.getMessageReceivedEvent();

        if (event.isWebhookMessage() || event.getAuthor().isBot()){
            return;
        }

        if (event.getMessage().getTextChannel().getId().equals(plugin.getConfig().getString("NotificationChannel"))){
            for (Player player : plugin.getServer().getOnlinePlayers()){
                player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] "+ChatColor.RESET+"新しいお知らせがあります。");
                TextComponent component = Component.text("[確認する]");
                component = component.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/noti"));
                player.sendMessage(component);
            }

            return;
        }

        if (event.getMessage().getTextChannel().getId().equals(plugin.getConfig().getString("ChatChannel"))){
            String discordName = event.getAuthor().getName();
            if (event.getMessage().getMember().getNickname() != null){
                discordName = event.getMessage().getMember().getNickname();
            }

            String text = ChatColor.AQUA + "[Discord] "+ discordName + " " + ChatColor.RESET + e.getMessageReceivedEvent().getMessage().getContentDisplay();
            plugin.getLogger().info(text);
            for (Player player : plugin.getServer().getOnlinePlayers()){
                player.sendMessage(text);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void PlayerDeathEvent(PlayerDeathEvent e){

        int exp = e.getDroppedExp();
        if (e.getEntity().getLocation().getWorld().getName().equals("world_the_end")){
            e.setKeepInventory(true);
            e.setKeepLevel(true);
            e.getDrops().clear();
            e.setDroppedExp(0);
            return;
        } else {
            e.setKeepInventory(false);
            e.setKeepLevel(false);
        }

        Player player = e.getEntity();
        PlayerInventory inventory = e.getEntity().getInventory();
        YamlConfiguration config = new YamlConfiguration();
        UUID DeathUUID = UUID.randomUUID();

        config.set("x", player.getLocation().getBlockX());
        config.set("y", player.getLocation().getBlockY());
        config.set("z", player.getLocation().getBlockZ());

        config.set("exp", exp);
        config.set("OldBlockType", player.getLocation().getBlock().getType().name());

        for (int i = 0; i < inventory.getSize(); i++){
            ItemStack item = inventory.getItem(i);
            if (item != null){
                config.set("item"+i, item);
                e.getEntity().getInventory().clear(i);
            }
        }


        try {
            File file = new File("./" + plugin.getDataFolder().getPath().replaceAll("\\\\", "/") + "/de/");
            if (!file.exists()){
                file.mkdir();
            }

            config.save(new File("./"+plugin.getDataFolder().getPath().replaceAll("\\\\", "/")+"/de/"+DeathUUID.toString()+".yml"));
            plugin.getLogger().info("[死体生成] "+DeathUUID.toString()+".yml");
        } catch (IOException ex){
            ex.printStackTrace();
        }

        player.getLocation().getWorld().getBlockAt(player.getLocation()).setType(Material.BIRCH_SIGN);

        Block block = player.getLocation().getBlock();
        Sign sign = (Sign) block.getState();
        sign.line(0, Component.text("[死体]"));
        sign.line(1, Component.text(player.getName()));
        sign.update();

        player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "ワールド名"+player.getLocation().getWorld().getName()+"の" + "X:" + player.getLocation().getBlockX() + " Y:"+ player.getLocation().getBlockY() + " Z:" + player.getLocation().getBlockZ() + "に死体を生成しました。 (左クリックで回収できます。)");

        player.spigot().respawn();

        new Thread(()->{
            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));
                con.setAutoCommit(true);

                PreparedStatement statement = con.prepareStatement("INSERT INTO `DeathList`(`UUID`, `MinecraftUUID`, `world`, `x`, `y`, `z`, `Active`) VALUES (?,?,?,?,?,?,?)");
                statement.setString(1, DeathUUID.toString());
                statement.setString(2, e.getEntity().getUniqueId().toString());
                statement.setString(3, block.getLocation().getWorld().getUID().toString());
                statement.setInt(4, block.getLocation().getBlockX());
                statement.setInt(5, block.getLocation().getBlockY());
                statement.setInt(6, block.getLocation().getBlockZ());
                statement.setBoolean(7, true);

                statement.execute();
                statement.close();
                con.close();
            } catch (SQLException ex){
                ex.printStackTrace();
            }
        }).start();

        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void PlayerInteractEvent (PlayerInteractEvent e){
        Block block = e.getClickedBlock();

        if (block != null && block.getState() instanceof Sign){

            UUID targetUUID = null;
            UUID targetUser = null;
            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));
                con.setAutoCommit(true);

                PreparedStatement statement = con.prepareStatement("SELECT * FROM DeathList WHERE Active = 1 AND world = ? AND x = ? AND y = ? AND z = ?");
                statement.setString(1, block.getLocation().getWorld().getUID().toString());
                statement.setInt(2, block.getLocation().getBlockX());
                statement.setInt(3, block.getLocation().getBlockY());
                statement.setInt(4, block.getLocation().getBlockZ());

                ResultSet set = statement.executeQuery();
                if (set.next()){
                    targetUUID = UUID.fromString(set.getString("UUID"));
                    targetUser = UUID.fromString(set.getString("MinecraftUUID"));
                }

                set.close();
                statement.close();
                con.close();
            } catch (SQLException ex){
                ex.printStackTrace();
            }

            if (targetUUID == null){
                return;
            }

            if (targetUser == null){
                Sign sign = (Sign) block.getState();
                String str = sign.line(1).insertion();

                if (str != null && !e.getPlayer().getName().startsWith(str)){
                    return;
                }
            } else {
                if (!e.getPlayer().getUniqueId().equals(targetUser)){
                    return;
                }
            }

            File file = new File("./" + plugin.getDataFolder().getPath().replaceAll("\\\\", "/") + "/de/" + targetUUID.toString() + ".yml");

            if (!file.exists()){
                plugin.getLogger().info("[死体生成] 存在しないデータ : " + targetUUID.toString() + ".yml");
            }

            if (file.exists()){
                YamlConfiguration config = new YamlConfiguration();
                try {
                    config.load(file);
                    Material type = null;
                    if (config.isSet("OldBlockType")){
                        type = Material.getMaterial((String) config.get("OldBlockType"));
                    }

                    if (type != null){
                        block.getLocation().getWorld().getBlockAt(block.getLocation()).setType(type);
                    } else {
                        block.getLocation().getWorld().getBlockAt(block.getLocation()).setType(Material.AIR);
                    }

                    if (config.isSet("exp")){
                        e.getPlayer().giveExp(config.getInt("exp"));
                    }

                    int size = e.getPlayer().getInventory().getSize();
                    for (int i = 0; i < size; i++){
                        if (config.isSet("item"+i)){
                            ItemStack stack = config.getItemStack("item" + i);
                            block.getLocation().getWorld().dropItem(block.getLocation(), stack);
                        }
                    }
                } catch (IOException | InvalidConfigurationException ex){
                    ex.printStackTrace();
                }
                file.deleteOnExit();
            }

            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));
                con.setAutoCommit(true);

                PreparedStatement statement = con.prepareStatement("UPDATE `DeathList` SET `Active`= 0 WHERE UUID = ?");
                statement.setString(1, targetUUID.toString());
                statement.execute();
                statement.close();
                con.close();
            } catch (SQLException ex){
                ex.printStackTrace();
            }

        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void EntityExplodeEvent (EntityExplodeEvent e){
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void BlockBreakEvent(BlockBreakEvent e){
        Block block = e.getBlock();
        if (block.getState() instanceof Chest || block.getState() instanceof ShulkerBox){

            String uuid = "";
            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));

                PreparedStatement statement = con.prepareStatement("SELECT * FROM `LockUUIDList` WHERE world = ? AND x = ? AND y = ? AND z = ? AND Active = 1");
                statement.setString(1, block.getLocation().getWorld().getUID().toString());
                statement.setInt(2, block.getLocation().getBlockX());
                statement.setInt(3, block.getLocation().getBlockY());
                statement.setInt(4, block.getLocation().getBlockZ());
                ResultSet set = statement.executeQuery();
                if (set.next()){
                    uuid = set.getString("UUID");
                }
                set.close();
                statement.close();
                con.close();
            } catch (SQLException ex){
                ex.printStackTrace();
            }

            plugin.getLogger().info("test");
            if (uuid.equals("")){
                return;
            }
            plugin.getLogger().info(uuid);
            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));

                PreparedStatement statement = con.prepareStatement("SELECT * FROM LockList WHERE BlockID = ? AND Active = 1");
                statement.setString(1, uuid);
                ResultSet set = statement.executeQuery();
                if (set.next()){
                    e.getPlayer().sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "保護がかかっています。");
                    e.setCancelled(true);
                }

                set.close();
                statement.close();
                con.close();
            } catch (SQLException ex){
                ex.printStackTrace();
            }

            return;
        }

        if (block.getState() instanceof Sign){
            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));

                PreparedStatement statement = con.prepareStatement("SELECT * FROM DeathList WHERE Active = 1 AND x = ? AND y = ? AND z = ?");
                statement.setInt(1, block.getLocation().getBlockX());
                statement.setInt(2, block.getLocation().getBlockY());
                statement.setInt(3, block.getLocation().getBlockZ());
                ResultSet set = statement.executeQuery();
                if (set.next()){
                    e.setCancelled(true);
                }
                set.close();
                statement.close();
                con.close();
            } catch (SQLException ex){
                ex.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void PlayerInteractEntityEvent (PlayerInteractEntityEvent e){
        Entity entity = e.getRightClicked();

        if (!(entity instanceof ItemFrame)){
            return;
        }

        boolean isLock = false;
        boolean isAdd = false;
        LockCommandUser u = null;
        for (LockCommandUser user : lockUserList){
            if (user.getUserUUID().equals(e.getPlayer().getUniqueId())){
                isLock = true;
                isAdd = user.isAdd();
                u = user;
                break;
            }
        }

        if (u != null){
            lockUserList.remove(u);
        }

        ItemFrame frame = (ItemFrame) entity;
        UUID uuid = frame.getUniqueId();
        Player player = e.getPlayer();
        if (isLock){
            // ロック関係
            e.setCancelled(true);
            if (isAdd){
                // ロック追加
                try {
                    Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));
                    PreparedStatement statement = con.prepareStatement("SELECT * FROM LockList WHERE BlockID = ? AND Active = 1");
                    statement.setString(1, uuid.toString());
                    ResultSet set = statement.executeQuery();
                    boolean result = true;
                    if (set.next()){
                        result = false;
                    }
                    set.close();
                    statement.close();

                    if (!result){
                        con.close();
                        player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] "+ChatColor.RESET+"この額縁はすでに保護されています。");
                        return;
                    }

                    player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] "+ChatColor.RESET+"額縁を保護しました。");
                    PreparedStatement statement1 = con.prepareStatement("INSERT INTO `LockList`(`UUID`, `BlockID`, `BlockType`, `MinecraftUserID`, `IsParent`, `Active`) VALUES (?,?,?,?,?,?)");
                    statement1.setString(1, UUID.randomUUID().toString());
                    statement1.setString(2, uuid.toString());
                    statement1.setString(3, entity.getType().name());
                    statement1.setString(4, player.getUniqueId().toString());
                    statement1.setBoolean(5, true);
                    statement1.setBoolean(6, true);

                    statement1.execute();
                    statement1.close();
                    con.close();

                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                return;
            }
            // ロック解除
            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));
                PreparedStatement statement = con.prepareStatement("SELECT * FROM LockList WHERE BlockID = ? AND Active = 1");
                statement.setString(1, uuid.toString());
                ResultSet set = statement.executeQuery();
                boolean result = false;
                boolean isFound = false;
                while (set.next()){
                    isFound = true;
                    if (player.getUniqueId().toString().equals(set.getString("MinecraftUserID")) && set.getBoolean("IsParent")){
                        result = true;
                        break;
                    }
                }
                set.close();
                statement.close();

                if (!isFound){
                    con.close();
                    player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] "+ChatColor.RESET+"この額縁は保護されていません。");
                    return;
                }

                if (!result){
                    con.close();
                    player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] "+ChatColor.RESET+"この額縁は他の人が保護しています。");
                    return;
                }

                player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] "+ChatColor.RESET+"額縁の保護解除をしました。");

                PreparedStatement statement1 = con.prepareStatement("UPDATE `LockList` SET `Active` = 0 WHERE BlockID = ?");
                statement1.setString(1, uuid.toString());
                statement1.execute();
                statement1.close();
                con.close();

            } catch (SQLException ex){
                ex.printStackTrace();
            }

        }

        // 保護チェック
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));
            PreparedStatement statement = con.prepareStatement("SELECT * FROM LockList WHERE BlockID = ? AND Active = 1");
            statement.setString(1, uuid.toString());
            ResultSet set = statement.executeQuery();

            boolean result = false;
            boolean lock = false;
            while (set.next()){
                lock = true;
                if (player.getUniqueId().toString().equals(set.getString("MinecraftUserID"))){
                    result = true;
                    break;
                }
            }
            set.close();
            statement.close();
            con.close();

            if (!lock){
                return;
            }

            if (!result){
                player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] "+ChatColor.RESET+"この額縁は他の人が保護している額縁です。");
                e.setCancelled(true);
            }

        } catch (SQLException ex){
            ex.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void HangingBreakEvent (HangingBreakEvent e){
        if (e.getEntity() instanceof ItemFrame){
            ItemFrame frame = (ItemFrame) e.getEntity();
            UUID uuid = frame.getUniqueId();

            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));
                PreparedStatement statement = con.prepareStatement("SELECT * FROM LockList WHERE BlockID = ? AND Active = 1");
                statement.setString(1, uuid.toString());
                ResultSet set = statement.executeQuery();

                if (set.next()){
                    e.setCancelled(true);
                }

                set.close();
                statement.close();
                con.close();
            } catch (SQLException ex){
                ex.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void EntityDamageEvent(EntityDamageEvent e){
        if (e.getEntity() instanceof ItemFrame){
            ItemFrame frame = (ItemFrame) e.getEntity();
            UUID uuid = frame.getUniqueId();

            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));
                PreparedStatement statement = con.prepareStatement("SELECT * FROM LockList WHERE BlockID = ? AND Active = 1");
                statement.setString(1, uuid.toString());
                ResultSet set = statement.executeQuery();

                if (set.next()){
                    e.setCancelled(true);
                }

                set.close();
                statement.close();
                con.close();
            } catch (SQLException ex){
                ex.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void EntityDamageByEntityEvent (EntityDamageByEntityEvent e){
        if (e.getEntity() instanceof ItemFrame){
            ItemFrame frame = (ItemFrame) e.getEntity();
            UUID uuid = frame.getUniqueId();

            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));
                PreparedStatement statement = con.prepareStatement("SELECT * FROM LockList WHERE BlockID = ? AND Active = 1");
                statement.setString(1, uuid.toString());
                ResultSet set = statement.executeQuery();

                if (set.next()){
                    e.setCancelled(true);
                }

                set.close();
                statement.close();
                con.close();
            } catch (SQLException ex){
                ex.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void EntityTeleportEvent (EntityTeleportEvent e){
        if (e.getEntity() instanceof ItemFrame){
            ItemFrame frame = (ItemFrame) e.getEntity();
            UUID uuid = frame.getUniqueId();

            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysqlServer") + ":" + plugin.getConfig().getInt("mysqlPort") + "/" + plugin.getConfig().getString("mysqlDatabase") + plugin.getConfig().getString("mysqlOption"), plugin.getConfig().getString("mysqlUsername"), plugin.getConfig().getString("mysqlPassword"));
                PreparedStatement statement = con.prepareStatement("SELECT * FROM LockList WHERE BlockID = ? AND Active = 1");
                statement.setString(1, uuid.toString());
                ResultSet set = statement.executeQuery();

                if (set.next()){
                    e.setCancelled(true);
                }

                set.close();
                statement.close();
                con.close();
            } catch (SQLException ex){
                ex.printStackTrace();
            }
        }
    }
}
