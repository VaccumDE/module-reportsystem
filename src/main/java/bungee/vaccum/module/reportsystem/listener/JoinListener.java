package bungee.vaccum.module.reportsystem.listener;

import bungee.vaccum.api.cloud.CloudHandler;
import bungee.vaccum.api.cloud.ReportManager;
import bungee.vaccum.api.objects.Report;
import bungee.vaccum.api.vBungeeAPI;
import bungee.vaccum.module.reportsystem.ReportSystem;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class JoinListener implements Listener {

    ReportManager reportManager = ReportSystem.getInstance().getBungeeAPI().getReportManager();
    CloudHandler cloudHandler = ReportSystem.getInstance().getBungeeAPI().getCloudHandler();
    private final IPlayerManager playerManager = CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class);

    @EventHandler
    public void onConnect(PostLoginEvent event) throws Exception {
        ProxiedPlayer proxiedPlayer = event.getPlayer();

        if(cloudHandler.hasPlayerAcceptedTerms(proxiedPlayer)) {
            if (!proxiedPlayer.hasPermission("vaccum.report.process")) {
                reportManager.setReportAutoLoginStatus(proxiedPlayer, false);
            }

            if (reportManager.getReportAutoLoginStatus(proxiedPlayer)) {
                if (proxiedPlayer.hasPermission("vaccum.report.process")) {
                    reportManager.setReportStatus(proxiedPlayer, true);
                    proxiedPlayer.sendMessage(ReportSystem.prefix + "Du wurdest eingeloggt.");

                    int reportCaseSize = reportManager.getOpenCases().size();
                    if (reportCaseSize == 0) {
                        proxiedPlayer.sendMessage(ReportSystem.prefix + "Es ist aktuell kein Report-Case offen.");
                    } else if (reportCaseSize == 1) {
                        proxiedPlayer.sendMessage(ReportSystem.prefix + "Es ist aktuell §aein §7Report-Case offen.");
                    } else {
                        proxiedPlayer.sendMessage(ReportSystem.prefix + "Es sind aktuell §a" + reportCaseSize + " §7Report-Cases offen.");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer proxiedPlayer = event.getPlayer();

        vBungeeAPI.getInstance().getProxy().getScheduler().runAsync(vBungeeAPI.getInstance(), () -> {
            try {
                if (reportManager.getReportStatus(proxiedPlayer)) {
                    reportManager.setReportStatus(proxiedPlayer, false);
                }

                if (reportManager.isEditorAlreadyInCase(proxiedPlayer)) {
                    Report report = reportManager.getCaseOfEditor(proxiedPlayer);
                    reportManager.setEditorToNull(report.getCaseId());
                    for (UUID uuid : reportManager.getOnlineReportPlayers()) {
                        ICloudPlayer all = playerManager.getOnlinePlayer(uuid);
                        all.getPlayerExecutor().sendChatMessage(ReportSystem.prefix + "Die Case §e" + report.getCaseId() + " §7wurde wieder eröffnet, da " + cloudHandler.getDisplayName(proxiedPlayer) + " §7den Server verlassen hat.");
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

}
