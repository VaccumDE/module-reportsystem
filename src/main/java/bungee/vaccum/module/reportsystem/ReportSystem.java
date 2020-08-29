package bungee.vaccum.module.reportsystem;

import bungee.vaccum.api.vBungeeAPI;
import bungee.vaccum.module.reportsystem.commands.ReportCommand;
import bungee.vaccum.module.reportsystem.listener.JoinListener;
import net.md_5.bungee.api.plugin.Plugin;

public final class ReportSystem extends Plugin {

    private static ReportSystem instance;
    private vBungeeAPI bungeeAPI;
    public static String prefix = "§7» §cReport §8| §7";

    @Override
    public void onEnable() {
        instance = this;
        bungeeAPI = vBungeeAPI.getInstance();

        getProxy().getConsole().sendMessage("§7[" + getDescription().getName() + "] §aLoading...");

        getProxy().getPluginManager().registerListener(this, new JoinListener());
        getProxy().getPluginManager().registerCommand(this, new ReportCommand("report"));

        getProxy().getConsole().sendMessage("§7[" + getDescription().getName() + "] §aSucessfully loaded");
    }

    @Override
    public void onDisable() {

    }

    public static ReportSystem getInstance() { return instance; }

    public vBungeeAPI getBungeeAPI() { return bungeeAPI; }

}
