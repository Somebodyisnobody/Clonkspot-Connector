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

package de.creative_land.twitch;

import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.TwitchHelixBuilder;
import de.creative_land.Controller;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TwitchController {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledRunnable scheduledRunnable;

    public TwitchController() {
        var config = Controller.INSTANCE.configuration.getTwitchConfiguration();
        if (isNewConfiguration(config)) {
            Controller.INSTANCE.log.addLogEntry("TwitchController: No configuration for Twitch found. Deactivating Twitchbot. Please adjust the config file.");
            return;
        }

        TwitchHelix client = TwitchHelixBuilder.builder()
                .withClientId(config.getClientId())
                .withClientSecret(config.getClientSecret())
                .build();

        // At development date 2022-10-25 the game id of LegacyClonk is 222765 according to igdb.com
        var gameId = client.getGames(null, null, List.of(config.getGame())).execute().getGames().get(0).getId();

        scheduledRunnable = new ScheduledRunnable(client, gameId);
        scheduler.scheduleAtFixedRate(scheduledRunnable, config.getInitialStartupDelay(), config.getSearchPeriod(), TimeUnit.SECONDS);
        Controller.INSTANCE.log.addLogEntry("TwitchController: Started Twitchbot.");

    }

    private boolean isNewConfiguration(TwitchConfiguration config) {
        return (Objects.equals(config.getClientId(), "") || Objects.equals(config.getClientSecret(), "") || Objects.equals(config.getGame(), ""));
    }

}
