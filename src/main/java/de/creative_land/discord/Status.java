////////////////////////////////////////////////////////////////////////////////
// This file is part of the Clonkspot-Connector - https://github.com/Somebodyisnobody/Clonkspot-Connector
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
//
////////////////////////////////////////////////////////////////////////////////

package de.creative_land.discord;

import net.dv8tion.jda.api.OnlineStatus;

public class Status {

    private OnlineStatus currentOnlineStatus;

    private Activity currentActivity;

    public OnlineStatus getCurrentOnlineStatus() {
        return currentOnlineStatus;
    }

    public Activity getCurrentActivity() {
        return currentActivity;
    }

    public void setErrNoChannel() {
        DiscordConnector.INSTANCE.getJda().getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
        this.currentOnlineStatus = OnlineStatus.DO_NOT_DISTURB;
        DiscordConnector.INSTANCE.getJda().getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.competing(Activity.ERROR_NO_CHANNEL.toString()));
        this.currentActivity = Activity.ERROR_NO_CHANNEL;
    }

    public void setErrUpstreamOffline() {
        DiscordConnector.INSTANCE.getJda().getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
        this.currentOnlineStatus = OnlineStatus.DO_NOT_DISTURB;
        DiscordConnector.INSTANCE.getJda().getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.watching(Activity.ERROR_UPSTREAM_OFFLINE.toString()));
        this.currentActivity = Activity.ERROR_UPSTREAM_OFFLINE;
    }

    public void setRunning() {
        DiscordConnector.INSTANCE.getJda().getPresence().setStatus(OnlineStatus.ONLINE);
        this.currentOnlineStatus = OnlineStatus.ONLINE;
        DiscordConnector.INSTANCE.getJda().getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.watching(Activity.RUNNING.toString()));
        this.currentActivity = Activity.RUNNING;
    }

    public void setStopped() {
        DiscordConnector.INSTANCE.getJda().getPresence().setStatus(OnlineStatus.ONLINE);
        this.currentOnlineStatus = OnlineStatus.ONLINE;
        DiscordConnector.INSTANCE.getJda().getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.watching(Activity.STOPPED.toString()));
        this.currentActivity = Activity.STOPPED;
    }

    public void setErrNoGuild() {
        DiscordConnector.INSTANCE.getJda().getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
        this.currentOnlineStatus = OnlineStatus.DO_NOT_DISTURB;
        DiscordConnector.INSTANCE.getJda().getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.competing(Activity.ERROR_NO_GUILD.toString()));
        this.currentActivity = Activity.ERROR_NO_GUILD;
    }

    public void setErrNoAdminRole() {
        DiscordConnector.INSTANCE.getJda().getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
        this.currentOnlineStatus = OnlineStatus.DO_NOT_DISTURB;
        DiscordConnector.INSTANCE.getJda().getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.competing(Activity.ERROR_NO_ADMIN_ROLE.toString()));
        this.currentActivity = Activity.ERROR_NO_ADMIN_ROLE;
    }
}
