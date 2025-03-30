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

package de.creative_land.clonkspot;

import de.creative_land.Controller;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WatchDog implements Runnable {

    private final Runnable alarmAction;
    private final BlockingQueue<Boolean> watchdogFood = new LinkedBlockingQueue<>();

    public WatchDog(Runnable alarmAction) {
        this.alarmAction = alarmAction;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("SSE Connection Watchdog");
        try {
            while (true) {
                if (watchdogFood.poll(61, TimeUnit.SECONDS) == null) {
                    Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: SSE timeout. Restarting SSE listener");
                    alarmAction.run();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void feed() {
        watchdogFood.add(true);
    }
}
