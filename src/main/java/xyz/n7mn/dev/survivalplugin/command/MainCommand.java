package xyz.n7mn.dev.survivalplugin.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.n7mn.dev.survivalplugin.data.PlayerLocationData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainCommand implements CommandExecutor {

    private List<PlayerLocationData> locationDataList;
    public MainCommand(List<PlayerLocationData> locationDataList){
        this.locationDataList = locationDataList;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player){

            Player player = (Player) sender;

            if (!player.getLocation().getWorld().getName().equals("sigen")){
                player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "今いるワールドは資源ワールドではないです！！");
                return true;
            }

            List<PlayerLocationData> list = new ArrayList<>(locationDataList);
            for (PlayerLocationData data : list){
                if (data.getPlayer().getUniqueId().equals(player.getUniqueId())){
                    player.teleport(data.getLocation());
                    player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + "メインワールドへ移動しました。 もう一度移動するには「/sigen」と入力してください。");

                    locationDataList.remove(data);
                    return true;
                }
            }

            player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + " 何らかの原因で位置の保存が消えたようです！");
            player.sendMessage(ChatColor.YELLOW + "[ななみ生活鯖] " + ChatColor.RESET + " メインワールドにテレポートされます。");
            player.teleport(Objects.requireNonNull(Bukkit.getWorld("world")).getSpawnLocation());

        }

        return true;
    }
}
