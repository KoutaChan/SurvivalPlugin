package xyz.n7mn.dev.survivalplugin.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;

public class DeathCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender.isOp()){

            try {
                YamlConfiguration config = new YamlConfiguration();
                config.load(new File("./plugins/SurvivalPlugin/de/" + args[0]+".yml"));




                Player player = Bukkit.getServer().getPlayer(args[1]);
                if (config.isSet("exp")){
                    player.giveExp((int) config.get("exp"));
                }

                for (int i = 0; i < 50; i ++){

                    if (!config.isSet("item"+i)){
                        continue;
                    }

                    player.getInventory().addItem(config.getItemStack("item"+i));
                }

            } catch (Exception ex){
                ex.printStackTrace();
            }

        }

        return true;
    }
}
