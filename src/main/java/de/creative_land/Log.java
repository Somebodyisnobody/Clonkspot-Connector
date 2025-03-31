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

package de.creative_land;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import static de.creative_land.Controller.getArtifactVersion;

public class Log {

    private final Vector<String> log;

    public Log() {
        log = new Vector<>();

        final StringBuilder sb = new StringBuilder("Clonkspot Discord Connector ☜(ﾟヮﾟ☜)\t|\tVersion ");
        try {
            sb.append(getArtifactVersion());
        } catch (IOException e) {
            e.printStackTrace();
            sb.append("Error reading artifact version!");
        }
        addLogEntry("Started the application");
        addLogEntry(sb);
    }

    public String printLog() {
        return String.join("\n", log);
    }

    public void addLogEntry(Serializable message) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM 'at' HH:mm z");
        System.out.println(message);
        log.add(formatter.format(new Date(System.currentTimeMillis())) + ":  " + message);
        if (log.size() > 2000) {
            log.remove(11);
        }
    }

    public void addLogEntry(Throwable e) {
        addLogEntry("", e);
    }

    public void addLogEntry(String prefix, Throwable e) {
        StringWriter writer = new StringWriter();
        writer.write(prefix);
        e.printStackTrace(new PrintWriter(writer));
        addLogEntry(writer.toString());
    }
}
