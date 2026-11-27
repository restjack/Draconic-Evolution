package com.brandon3055.draconicevolution.utils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Shared owner/team helpers for the fork's owned blocks (restjack fork, unofficial-1.36; was in TileCoreMonitor). */
public final class TeamUtils {
    private TeamUtils() {}

    /** Online members of the owner's FTB Teams party if that mod is loaded, else of the owner's scoreboard team. */
    public static List<ServerPlayer> teamMembers(ServerLevel sl, UUID owner, @Nullable ServerPlayer ownerPlayer) {
        List<ServerPlayer> out = new ArrayList<>();
        if (net.neoforged.fml.ModList.get().isLoaded("ftbteams")) {
            try { // reflection: no compile dependency on FTB Teams
                Object api = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI").getMethod("api").invoke(null);
                Object manager = api.getClass().getMethod("getManager").invoke(api);
                var team = (java.util.Optional<?>) manager.getClass().getMethod("getTeamForPlayerID", UUID.class).invoke(manager, owner);
                if (team.isPresent()) {
                    for (Object p : (java.util.Collection<?>) team.get().getClass().getMethod("getOnlineMembers").invoke(team.get())) {
                        if (p instanceof ServerPlayer sp) out.add(sp);
                    }
                    return out;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        if (ownerPlayer != null) {
            var team = sl.getServer().getScoreboard().getPlayersTeam(ownerPlayer.getScoreboardName());
            if (team != null) {
                for (String n : team.getPlayers()) {
                    ServerPlayer p = sl.getServer().getPlayerList().getPlayerByName(n);
                    if (p != null) out.add(p);
                }
            }
        }
        return out;
    }
}
