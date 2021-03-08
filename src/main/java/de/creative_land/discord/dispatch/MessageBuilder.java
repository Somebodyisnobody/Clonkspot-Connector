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

package de.creative_land.discord.dispatch;

import de.creative_land.Controller;
import de.creative_land.clonkspot.model.GameReference;
import de.creative_land.discord.DiscordConnector;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageBuilder {

    private final HashMap<String, OffsetDateTime> mentionedRoles;

    private final String joinUrl;

    public MessageBuilder() {
        this.mentionedRoles = new HashMap<>();
        this.joinUrl = Controller.INSTANCE.configuration.getJoinUrl();
    }

    /**
     * Builds a new message string for a game reference. The message string contains all changes needed and is ready for being sent. Manipulation rules are already applied on the message.
     *
     * @param gameReference                           parsed game reference.
     * @param buildAction                             action which message string has to be built.
     * @param mentionedRolesFromLastDispatchedMessage the roles that were dispatched in the last message. If no last message exist (on announcing a new game reference) use null.
     * @return the built message string or null if the build process was intentionally aborted (e.g. code injection, test scenario).
     */
    String build(@NotNull GameReference gameReference, @NotNull BuildAction buildAction, List<Role> mentionedRolesFromLastDispatchedMessage) {

        if (System.getenv("DEBUG") == null && gameReference.title.equals("Testing Clonkspot-Discord connector do not join!"))
            return null;

        //Remove markup
        String title;
        final Matcher removeMarkup = Pattern.compile("([^<>]*)(<c [0-9a-fA-F]{3,8}>|</c>|<i>|</i>|\\\\t)([^<>]*)").matcher(gameReference.title);
        if (removeMarkup.find()) {
            title = removeMarkup.replaceAll("$1$3");
        } else {
            title = gameReference.title;
        }

        //Find code injections: <foo>, <foo> bar>, http://test.com, https://test.com, discord alerting, newline, tabulator
        if (Pattern.compile("([^<>]*)(<[^<>]*>|https?://|@everyone|@here|\\\\n)([^<>]*)").matcher((title + gameReference.hostname).toLowerCase()).find()) {
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Code injection: " + gameReference.id + " (Hostname: \"" + gameReference.hostname + "\", Title: \"" + gameReference.title + "\").");
            return null;
        }


        //Find and apply manipulation rule
        String mentions = null;
        final var manipulationRule = getManipulationRule(title);
        if (manipulationRule != null) {
            final var replacement = manipulationRule.getReplacement();
            if (replacement != null)
                try {
                    title = manipulationRule.getPattern().matcher(title).replaceAll(replacement);
                } catch (Exception e) {
                    Controller.INSTANCE.log.addLogEntry("Message Builder: Error while applying manipulation rule: Check if replacement \"" + replacement + "\" of \"" + manipulationRule.getName() + "\" is correct. " + e.getClass().getName() + " " + e.getMessage());
                }
            mentions = getMentions(manipulationRule.getRoles(), mentionedRolesFromLastDispatchedMessage);
        }

        //Mark title bold if exist after applying manipulation rule
        if (!title.equals("")) {
            title = "**" + title + "**";
        }

        switch (buildAction) {
            case CREATE -> {
                return buildCreateMessage(gameReference, mentions, title);
            }
            case RUNNING_NO_RUNTIME_JOIN -> {
                return buildRunningNoRuntimeJoinMessage(gameReference, mentions, title);
            }
            case RUNNING_WITH_RUNTIME_JOIN -> {
                return buildRunningWithRuntimeJoinMessage(gameReference, mentions, title);
            }
            case CLOSE -> {
                return buildCloseMessage(gameReference, mentions, title);
            }
        }
        return null;
    }

    /**
     * Builds a create message string.
     *
     * @param gameReference parsed game reference.
     * @param mentions      parsed mentions.
     * @param title         cleaned title.
     * @return create-message string.
     */
    private String buildCreateMessage(GameReference gameReference, String mentions, String title) {
        final StringBuilder stringBuilder = new StringBuilder();
        if (mentions != null) stringBuilder.append(mentions);
        stringBuilder.append(title);
        stringBuilder.append(" on \"");
        stringBuilder.append(gameReference.hostname);
        stringBuilder.append("\". ");
        stringBuilder.append("<:JoinLobby:803286358964830239>");
        stringBuilder.append(" ");
        stringBuilder.append(joinUrl);
        stringBuilder.append(gameReference.id);

        return stringBuilder.toString();
    }

    /**
     * Builds a running message string for games without runtime join enabled.
     *
     * @param gameReference parsed game reference.
     * @param mentions      parsed mentions.
     * @param title         cleaned title.
     * @return running-message string (without runtime join).
     */
    private String buildRunningNoRuntimeJoinMessage(GameReference gameReference, String mentions, String title) {
        final StringBuilder stringBuilder = new StringBuilder();
        if (mentions != null) stringBuilder.append(mentions);
        stringBuilder.append(title);
        stringBuilder.append(" on \"");
        stringBuilder.append(gameReference.hostname);
        stringBuilder.append("\". ");
        stringBuilder.append("<:Running:803286306624372776>️");
        stringBuilder.append(" ");
        stringBuilder.append("Running.");

        return stringBuilder.toString();
    }

    /**
     * Builds a running message string for games with runtime join enabled.
     *
     * @param gameReference parsed game reference.
     * @param mentions      parsed mentions.
     * @param title         cleaned title.
     * @return running-message string (with runtime join).
     */
    private String buildRunningWithRuntimeJoinMessage(GameReference gameReference, String mentions, String title) {
        final StringBuilder stringBuilder = new StringBuilder();
        if (mentions != null) stringBuilder.append(mentions);
        stringBuilder.append(title);
        stringBuilder.append(" on \"");
        stringBuilder.append(gameReference.hostname);
        stringBuilder.append("\". ");
        stringBuilder.append("<:JoinRunning:803286375986233405>");
        stringBuilder.append("<:Running:803286306624372776>");
        stringBuilder.append(" ");
        stringBuilder.append(joinUrl);
        stringBuilder.append(gameReference.id);

        return stringBuilder.toString();
    }

    /**
     * Builds a closed message string for games which ended.
     *
     * @param gameReference parsed game reference.
     * @param mentions      parsed mentions.
     * @param title         cleaned title.
     * @return closed-message string.
     */
    private String buildCloseMessage(GameReference gameReference, String mentions, String title) {
        final StringBuilder stringBuilder = new StringBuilder();
        if (mentions != null) stringBuilder.append(mentions);
        stringBuilder.append(title);
        stringBuilder.append(" on \"");
        stringBuilder.append(gameReference.hostname);
        stringBuilder.append("\". ");
        stringBuilder.append("<:Exit:710568465894867035>");
        stringBuilder.append(" Game closed.");

        return stringBuilder.toString();
    }

    /**
     * Gets the first manipulation rule matching the title.
     *
     * @param title the cleaned game title.
     * @return a {@link ManipulationRule} or null if no rule was found.
     */
    private ManipulationRule getManipulationRule(String title) {
        for (var manipulationRule : Controller.INSTANCE.configuration.getManipulationRules()) {
            if (manipulationRule.getPattern().matcher(title).find()) {
                return manipulationRule;
            }
        }
        return null;
    }

    /**
     * Gets all roles of the manipulation rule and converts them into mentions when there's no active cooldown.
     * If roles from a previous version of the dispatched message is provided this roles will be used.
     *
     * @param rolesToBeMentioned                      the roles list of a manipulation rule.
     * @param mentionedRolesFromLastDispatchedMessage the roles that were dispatched in the last message. If no last message exist (on announcing a new game reference) use null.
     * @return discord readable mentions or null if the list was null.
     */
    private String getMentions(List<String> rolesToBeMentioned, List<Role> mentionedRolesFromLastDispatchedMessage) {
        if (rolesToBeMentioned != null) {
            StringBuilder mentions = new StringBuilder();
            for (var roleToBeMentioned : rolesToBeMentioned) {

                //Will be null on create
                if (mentionedRolesFromLastDispatchedMessage != null) {
                    //Check if last message contained the current role as mention. If yes then mention this role ignoring the cooldown, if no just create the @-string
                    if (mentionedRolesFromLastDispatchedMessage.stream().noneMatch(mentionedRoleFromLastDispatchedMessage -> mentionedRoleFromLastDispatchedMessage.getName().equals(roleToBeMentioned))) {
                        mentions.append("@").append(roleToBeMentioned).append(" ");
                        continue;
                    }
                } else {
                    //Check if the role was mentioned before
                    final var lastMentionedRoleDate = mentionedRoles.get(roleToBeMentioned);
                    if (lastMentionedRoleDate != null) {

                        //Search for a cooldown for this role
                        int cooldown = 0;
                        try {
                            //Rules should be present in normal use
                            //noinspection OptionalGetWithoutIsPresent
                            cooldown = Controller.INSTANCE.configuration.getMentionRoleCooldowns().stream()
                                    .filter(mentionRoleCooldown -> mentionRoleCooldown.getRole().equals(roleToBeMentioned))
                                    .findFirst().get().getCooldown();
                        } catch (Exception ignored) {
                        }

                        //If there is a cooldown compare it with the current time and the last mention
                        if (cooldown != 0 && lastMentionedRoleDate.isAfter(OffsetDateTime.now().minusMinutes(cooldown))) {
                            mentions.append("@").append(roleToBeMentioned).append(" ");
                            continue;
                        }
                    }
                }
                final var roleIds = DiscordConnector.INSTANCE.getJda().getRolesByName(roleToBeMentioned, false);
                if (!roleIds.isEmpty()) {
                    mentions.append(roleIds.get(0).getAsMention()).append(" ");
                    if (mentionedRolesFromLastDispatchedMessage == null)
                        mentionedRoles.put(roleToBeMentioned, OffsetDateTime.now());
                }
            }
            return mentions.toString();
        }
        return null;
    }
}
