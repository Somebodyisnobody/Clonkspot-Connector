// This file is part of the Clonkspot-Connector - https://github.com/Somebodyisnobody/Clonkspot-Connector/
//
// Clonkspot-Connector is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// Clonkspot-Connector is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Clonkspot-Connector.  If not, see <http://www.gnu.org/licenses/>.

package de.creative_land.discord;

import net.dv8tion.jda.api.OnlineStatus;

public class Status {

    public void setErrNoChannel() {
        DiscordConnector.INSTANCE.getJda().getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
        DiscordConnector.INSTANCE.getJda().getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.competing(Activity.ERROR_NO_CHANNEL.toString()));
    }

    public void setErrUpstreamOffline() {
        DiscordConnector.INSTANCE.getJda().getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
        DiscordConnector.INSTANCE.getJda().getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.watching(Activity.ERROR_UPSTREAM_OFFLINE.toString()));
    }

    public void setRunning() {
        DiscordConnector.INSTANCE.getJda().getPresence().setStatus(OnlineStatus.ONLINE);
        DiscordConnector.INSTANCE.getJda().getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.watching(Activity.RUNNING.toString()));
    }

    public void setStopped() {
        DiscordConnector.INSTANCE.getJda().getPresence().setStatus(OnlineStatus.ONLINE);
        DiscordConnector.INSTANCE.getJda().getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.watching(Activity.STOPPED.toString()));
    }

    public void setErrNoGuild() {
        DiscordConnector.INSTANCE.getJda().getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
        DiscordConnector.INSTANCE.getJda().getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.competing(Activity.ERROR_NO_GUILD.toString()));
    }

    public void setErrNoAdminRole() {
        DiscordConnector.INSTANCE.getJda().getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
        DiscordConnector.INSTANCE.getJda().getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.competing(Activity.ERROR_NO_ADMIN_ROLE.toString()));
    }
}
