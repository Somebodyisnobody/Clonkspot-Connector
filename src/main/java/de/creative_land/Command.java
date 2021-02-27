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

public enum Command {
    EXIT("exit"),
    HELP("help"),
    STOP("stop  ------------------------------------------------------------------------  Stopps the service."),
    START("start  -----------------------------------------------------------------------  Starts the service."),
    LOG("log  -------------------------------------------------------------------------  Prints the log."),
    ADDNOTICE("addnotice <String>  ----------------------------------------------------------  Inserts a notice into the log."),
    CONFIG("config  ----------------------------------------------------------------------  Prints the current configuration."),
    TARGETCHANNEL("targetchannel <channelname>  -------------------------------------------------  Sets a new target channel."),
    NEWNAME("newname <name>  --------------------------------------------------------------  Sets a new name for the bot."),
    HOSTCOOLDOWN("hostcooldown <minutes>  ------------------------------------------------------  Sets a new general host cooldown for all hosts."),
    ADDIGNOREDHOST("addignoredhost `<minplayer>` `<hostname>` `<reason>`  ------------------------  Ignores a host if the number of players isn't reached (case-sensitive)."),
    REMOVEIGNOREDHOST("removeignoredhost <hostname>  ------------------------------------------------  Removes a host from the the ignored hosts list (case-sensitive)."),
    ADDMENTIONROLECOOLDOWN("addmentionrolecooldown <role> <minutes>  -------------------------------------  Adds a cooldown for a role mention (case-sensitive)."),
    REMOVEMENTIONROLECOOLDOWN("removementionrolecooldown <role>  --------------------------------------------  Removes an existing cooldown for role mention (case-sensitive)."),
    ADDMANIPULATIONRULE("addmanipulationrule `<name>` `<pattern>` `<replacement>` `<role>,<role>`  ----  Manipulating game titles and mention roles (replacement = regex capture group)."),
    REMOVEMANIPULATIONRULE("removemanipulationrule <name>  -----------------------------------------------  Removes an existing manipulation rule."),
    RESOLVEID("resolveid <id>  --------------------------------------------------------------  Resolves a dispatched game reference by id."),
    CLONKVERSION("clonkversion `<engine>` `<build version>`  -----------------------------------  Sets a new Clonk version for the bot which must match on new refrences. Use `null` and 0 for no restriction."),
    ;

    private final String value;

    Command(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
