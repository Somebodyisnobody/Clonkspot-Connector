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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.creative_land.Controller;
import de.creative_land.clonkspot.model.GameReference;
import de.creative_land.discord.DiscordConnector;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class Dispatcher {

    private final MessageBuilder messageBuilder;
    private final ArrayList<DispatchedMessage> dispatchedMessages;
    private final Semaphore semaphore;

    public Dispatcher() {
        this.messageBuilder = new MessageBuilder();
        this.dispatchedMessages = new ArrayList<>();
        this.semaphore = new Semaphore(1);
    }

    /**
     * Processes a game reference as SSE event. If it matches the conditions an announcing message will be sent to discord.
     *
     * @param message SSE message
     * @param event   SSE event
     */
    public void process(@NotNull String message, @NotNull String event) {
        if (!(event.equals("create") || event.equals("update") || event.equals("delete") || event.equals("end")))
            return;

        final GameReference gameReference;
        if ((gameReference = parseJson(message)) == null) return;
        gameReference.sseEventType = event;

        switch (event) {
            case "create" -> createEvent(gameReference);
            case "update" -> updateEvent(gameReference);
            case "delete" -> deleteEvent(gameReference); //nicht gestartetes game beendet
            case "end" -> endEvent(gameReference);    //gestartetes game zu ende
        }
    }

    //START EVENTS//
    private void createEvent(GameReference gameReference) {
        announceNewGameReference(gameReference);
    }

    @SuppressWarnings("UnnecessaryReturnStatement")
    private void updateEvent(GameReference gameReference) {
        if (gameReference.status.equals("lobby")) {

            //Announce a game if it was not announced by cooldown and the cooldown is over
            if (announceNewGameReference(gameReference)) return;

        } else if (gameReference.status.equals("running")) {

            if (updateReferenceRunningNoRuntimeJoin(gameReference)) return;
            if (updateReferenceRunningWithRuntimeJoin(gameReference)) return;

        }

        //Delete game if password is set later.
        if (gameReference.flags.passwordNeeded) {
            if (deleteReference(gameReference)) return;
        }
        //More stuff like updating lobbystatus later
    }

    private void deleteEvent(GameReference gameReference) {
        deleteReference(gameReference);
    }

    private void endEvent(GameReference gameReference) {
        closeReference(gameReference);
    }
    //END EVENTS//

    /**
     * Sends a new message to discord. On success the game is added to a {@link DispatchedMessage} list to avoid double announcing. On fail a rescan of the environment is called.
     *
     * @param gameReference parsed game reference.
     * @return true if the game reference was handled, false if it was rejected.
     */
    private boolean announceNewGameReference(GameReference gameReference) {
        //Never announce if the bot status is not RUNNING
        if (!isRunning()) return false;

        //Only announce games with a specific host engine version
        if (!isCorrectVersion(gameReference))
            return false;

        try {
            semaphore.acquire();
        } catch (InterruptedException ignored) {
        }
        //Never announce an already announced game (checked by game id)
        if (isAlreadyAnnounced(gameReference)) {
            semaphore.release();
            return false;
        }

        //Never announce ignored hosts exempt they have active players
        if (isIgnoredHost(gameReference)) {
            semaphore.release();
            //Only log if it's a new game to avoid log spam
            if (gameReference.sseEventType.equals("create"))
                Controller.INSTANCE.log.addLogEntry("DiscordConnector: Ignored by hostname: " + gameReference.id + ", Host: \"" + gameReference.hostname + "\".");
            return false;
        }

        //Never announce hosts with active cooldown (checked by hostname)
        if (hostHasActiveCooldown(gameReference)) {
            semaphore.release();
            //Only log if it's a new game to avoid log spam
            if (gameReference.sseEventType.equals("create"))
                Controller.INSTANCE.log.addLogEntry("DiscordConnector: Ignored by cooldown: " + gameReference.id + ", Host: \"" + gameReference.hostname + "\".");
            return false;
        }

        //Never announce password protected games
        if (gameReference.flags.passwordNeeded) {
            semaphore.release();
            //Only log if it's a new game to avoid log spam
            if (gameReference.sseEventType.equals("create"))
                Controller.INSTANCE.log.addLogEntry("DiscordConnector: Ignored by password: " + gameReference.id + ", Host: \"" + gameReference.hostname + "\".");
            return false;
        }

        //now the request can be marked as handled (returning true)

        final String messageContent;
        if ((messageContent = messageBuilder.build(gameReference, BuildAction.CREATE, null)) != null) {
            DiscordConnector.INSTANCE.getTargetDispatchChannel().sendMessage(messageContent).queue(message -> {
                addDispatchedMessage(new DispatchedMessage(message, gameReference));
                semaphore.release();
                System.out.println("DiscordConnector: Dispatched: " + gameReference.id + ".");
            }, failure -> dispatchFailure(gameReference, failure, BuildAction.CREATE));
        } else {
            semaphore.release();
        }


        return true;
    }

    /**
     * Checks if the bot is in status "RUNNING".
     *
     * @return true, if the bot is in status "RUNNING".
     */
    private boolean isRunning() {
        return Objects.equals(DiscordConnector.INSTANCE.getJda().getPresence().getActivity(), net.dv8tion.jda.api.entities.Activity.watching(de.creative_land.discord.Activity.RUNNING.toString()));
    }

    /**
     * Checks if the game reference version information matches the requirements.
     *
     * @param gameReference parsed game reference.
     * @return true if the game reference has the correct version, false if not.
     */
    private boolean isCorrectVersion(GameReference gameReference) {
        final var engine = Controller.INSTANCE.configuration.getEngine();
        final var engineBuild = Controller.INSTANCE.configuration.getEngineBuild();

        if (engine != null && engineBuild != 0) {
            return gameReference.engine.equalsIgnoreCase(engine) && gameReference.engineBuild == engineBuild;
        }
        return true;
    }

    /**
     * Updates references when runtime join was disabled since last update.
     * On fail a rescan of the environment is called.
     *
     * @param gameReference parsed game reference.
     * @return true if the game reference was handled, false if it was rejected.
     */
    private boolean updateReferenceRunningNoRuntimeJoin(GameReference gameReference) {
        //Only update references without runtime join
        if (gameReference.flags.joinAllowed) return false;

        try {
            semaphore.acquire();
        } catch (InterruptedException ignored) {
        }
        //Filter out the references that belongs to the game and allowed runtime join or was in status lobby before
        final var optionalDispatchedMessage = dispatchedMessages.stream()
                .filter(dispatchedMessage -> dispatchedMessage.getGameReference().id == gameReference.id)
                .filter(dispatchedMessage -> !dispatchedMessage.getDeleted())
                .filter(dispatchedMessage -> dispatchedMessage.getGameReference().flags.joinAllowed || dispatchedMessage.getGameReference().status.equals("created"))
                .findFirst();

        //Don't handle if no game reference matched
        if (optionalDispatchedMessage.isEmpty()) {
            semaphore.release();
            return false;
        }

        editMessage(optionalDispatchedMessage.get(), gameReference, BuildAction.RUNNING_NO_RUNTIME_JOIN);
        return true;
    }

    /**
     * Updates references when runtime join was enabled since last update.
     * On fail a rescan of the environment is called.
     *
     * @param gameReference parsed game reference.
     * @return true if the game reference was handled, false if it was rejected.
     */
    private boolean updateReferenceRunningWithRuntimeJoin(GameReference gameReference) {
        //Only update references with runtime join
        if (!gameReference.flags.joinAllowed) return false;

        try {
            semaphore.acquire();
        } catch (InterruptedException ignored) {
        }

        //Filter out the reference that belongs to the game and denied runtime join or was in status lobby before
        final var optionalDispatchedMessage = dispatchedMessages.stream()
                .filter(dispatchedMessage -> dispatchedMessage.getGameReference().id == gameReference.id)
                .filter(dispatchedMessage -> !dispatchedMessage.getDeleted())
                .filter(dispatchedMessage -> !dispatchedMessage.getGameReference().flags.joinAllowed || dispatchedMessage.getGameReference().status.equals("created"))
                .findFirst();

        //Don't handle if no game reference matched
        if (optionalDispatchedMessage.isEmpty()) {
            semaphore.release();
            return false;
        }

        editMessage(optionalDispatchedMessage.get(), gameReference, BuildAction.RUNNING_WITH_RUNTIME_JOIN);
        return true;
    }

    /**
     * Searches for earlier sent messages that belongs to the game reference. Any matching message will be edited. The link for direct join is replaced by a another message defined in {@link MessageBuilder}.
     * On fail a rescan of the environment is called.
     *
     * @param gameReference parsed game reference to be marked as closed.
     */
    private void closeReference(GameReference gameReference) {
        try {
            semaphore.acquire();
        } catch (InterruptedException ignored) {
        }

        //Search for game reference id and replace
        final var optionalDispatchedMessage = dispatchedMessages.stream()
                .filter(dispatchedMessage -> dispatchedMessage.getGameReference().id == gameReference.id)
                .filter(dispatchedMessage -> !dispatchedMessage.getDeleted())
                .findFirst();

        if (optionalDispatchedMessage.isEmpty()) {
            semaphore.release();
            return;
        }

        editMessage(optionalDispatchedMessage.get(), gameReference, BuildAction.CLOSE);
    }

    /**
     * Deletes a dispatched message and marks this message in cache as deleted .
     * On fail a rescan of the environment is called.
     *
     * @param gameReference parsed game reference.
     * @return true if the game reference was handled, false if it was rejected.
     */
    private boolean deleteReference(GameReference gameReference) {
        try {
            semaphore.acquire();
        } catch (InterruptedException ignored) {
        }

        //Search for game reference id and delete
        final var optionalDispatchedMessage = dispatchedMessages.stream()
                .filter(dispatchedMessage -> dispatchedMessage.getGameReference().id == gameReference.id)
                .filter(dispatchedMessage -> !dispatchedMessage.getDeleted())
                .findFirst();

        if (optionalDispatchedMessage.isEmpty()) {
            semaphore.release();
            return false;
        }
        final var dispatchedMessage = optionalDispatchedMessage.get();
        dispatchedMessage.getMessage().delete().queue(deletedMessage -> {
            dispatchedMessages.set(dispatchedMessages.indexOf(dispatchedMessage), dispatchedMessage.markAsDeleted());
            semaphore.release();
        }, failure -> dispatchFailure(gameReference, failure, BuildAction.DELETE));
        return true;
    }

    /**
     * Changes a message based on game reference and build action.
     * On fail a rescan of the environment is called.
     *
     * @param dispatchedMessage the message to be edited.
     * @param gameReference     parsed game reference.
     * @param buildAction       action which message string has to be built.
     */
    private void editMessage(DispatchedMessage dispatchedMessage, GameReference gameReference, BuildAction buildAction) {
        final String messageContent = messageBuilder.build(gameReference, buildAction, dispatchedMessage.getMessage().getMentionedRoles());
        if (messageContent == null) {
            semaphore.release();
            return;
        }
        dispatchedMessage.getMessage().editMessage(messageContent).queue(newMessage -> {
            dispatchedMessages.set(dispatchedMessages.indexOf(dispatchedMessage), dispatchedMessage.update(newMessage, gameReference));
            semaphore.release();
        }, failure -> dispatchFailure(gameReference, failure, buildAction));
    }

    /**
     * Handling for callback dispatch failure.
     * Rescans the environment.
     *
     * @param gameReference parsed game reference.
     * @param failure       throwable of the callback.
     * @param buildAction   type of action.
     */
    private void dispatchFailure(GameReference gameReference, Throwable failure, BuildAction buildAction) {
        semaphore.release();
        Controller.INSTANCE.log.addLogEntry("DiscordConnector: Dispatch failed: " + gameReference.id + ", Action: " + buildAction + ", Error: \"" + failure.getMessage() + "\".");
        DiscordConnector.INSTANCE.scanEnvironment(null);
    }

    /**
     * Checks if the game reference was already announced.
     *
     * @param gameReference parsed game reference.
     * @return true if a game with the same id was announced in earlier.
     */
    private boolean isAlreadyAnnounced(GameReference gameReference) {
        return dispatchedMessages.stream().anyMatch(dispatchedMessage -> dispatchedMessage.getGameReference().id == gameReference.id);
    }

    /**
     * Checks if the host is in the list of ignored hosts and has not enough player.
     *
     * @param gameReference parsed game reference.
     * @return true if the host is in the list of ignored hosts and doesn't match the required minimum of players.
     */
    private boolean isIgnoredHost(GameReference gameReference) {
        final var matchedIgnoredHostnames = Controller.INSTANCE.configuration.getIgnoredHostnames().stream()
                .filter(hostname -> hostname.getHostname().equals(gameReference.hostname)).collect(Collectors.toList());
        for (final var matchedIgnoredHostname : matchedIgnoredHostnames) {
            if (gameReference.players == null || gameReference.players.length < matchedIgnoredHostname.getMinPlayer())
                return true;
        }
        return false;
    }

    /**
     * Checks if the host of the game reference has an active cooldown.
     *
     * @param gameReference parsed game reference.
     * @return true if the host has an active cooldown and the game shouldn't be announced.
     */
    private boolean hostHasActiveCooldown(GameReference gameReference) {
        //Filter messages that match on that hostname in the last X minutes
        final var messages = dispatchedMessages.stream()
                .filter(dispatchedMessage -> dispatchedMessage.getGameReference().hostname.equals(gameReference.hostname))
                .filter(dispatchedMessage -> dispatchedMessage.getCreated().isAfter(OffsetDateTime.now().minusMinutes(Controller.INSTANCE.configuration.getHostCooldown())))
                .collect(Collectors.toList());
        return messages.size() > 0;
    }

    /**
     * Parses a JSON-String of a SSE-event into a {@link GameReference}.
     *
     * @param message SSE-message in JSON format.
     * @return a {@link GameReference} parsed from the String or null if the message could not be parsed.
     */
    private GameReference parseJson(String message) {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            return mapper.readValue(message, GameReference.class);
        } catch (JsonProcessingException e) {
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: JSON Parsing error: \n" + e.getMessage());
        }
        return null;
    }

    /**
     * Adds a new message to the dispatched messages.
     *
     * @param dispatchedMessage the message with the game reference that was announced.
     */
    public void addDispatchedMessage(DispatchedMessage dispatchedMessage) {
        while (dispatchedMessages.size() > 5000) {
            dispatchedMessages.remove(0);
        }
        dispatchedMessages.add(dispatchedMessage);
    }

    public ArrayList<DispatchedMessage> getDispatchedMessages() {
        return dispatchedMessages;
    }
}
