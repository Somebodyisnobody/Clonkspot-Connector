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

package de.creative_land.discord.clonk_game_reference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import de.creative_land.Controller;
import de.creative_land.clonkspot.model.GameRefEvent;
import de.creative_land.clonkspot.model.GameReference;
import de.creative_land.discord.DiscordConnector;
import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.OnlineStatus;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Dispatcher {

    private final MessageBuilder messageBuilder;
    @Getter
    private final ArrayList<DispatchedMessage> dispatchedMessages;
    private final HashSet<Integer> processedGamesList;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 5, 2, TimeUnit.MINUTES, new ArrayBlockingQueue<>(50));

    public Dispatcher() {
        this.messageBuilder = new MessageBuilder();
        this.dispatchedMessages = new ArrayList<>();
        this.processedGamesList = new HashSet<>();
    }

    /**
     * Processes a game reference as SSE event. If it matches the conditions an announcing message will be sent to discord. <br/>
     * Synchronized as no init messages should be processed in parallel.
     *
     * @param message SSE message
     * @param event   SSE event
     */
    //
    public synchronized void process(@NotNull String message, @NotNull String event) {
        if (!Objects.equals(DiscordConnector.INSTANCE.status.getCurrentOnlineStatus(), OnlineStatus.ONLINE)) return;

        if (event.equals("init")) {
            processInitEvent(message);
            return;
        } else if (event.equals("create") || event.equals("update") || event.equals("delete") || event.equals("end")) {
            final GameReference gameReference;
            if ((gameReference = parseGameReference(message)) == null) return;
            final GameRefEvent gameRefEvent = new GameRefEvent(gameReference, GameRefEvent.EventType.fromValue(event));

            executor.execute(() -> routeGameRefEvent(gameRefEvent));
            return;
        }
        // Ignore all other event types
        Controller.INSTANCE.log.addLogEntry("DiscordConnector: Unknown event received: '%s'".formatted(event));
    }

    /**
     * Processes an init SSE event. New games will be determined, ended games will be marked as such.
     *
     * @param message SSE message
     */
    public void processInitEvent(@NotNull String message) {
        List<GameReference> gameReferences = parseGameReferenceList(message);
        if (gameReferences.isEmpty()) return;

        gameReferences.stream()
                .filter(
                        // All game references that are in the init message but not in the list of dispatched messages are new and to be processed as created
                        gameReference -> dispatchedMessages.stream()
                                .map(DispatchedMessage::getGameReference)
                                .map(GameReference::getId)
                                .noneMatch(id -> id.equals(gameReference.getId()))
                )
                .map(gameReference -> new GameRefEvent(gameReference, GameRefEvent.EventType.CREATE))
                .forEach(this::routeGameRefEvent);

        // All dispatched messages whose game references are not in the init message are old games which have ended in an unkonwn way.
        // Possible endings (💥 = SSE connection interrupt)
        // * Game was running -> 💥 -> game ended -> init event                  -> game ended
        // * Game was in lobby -> 💥 -> lobby closed -> init event               -> unknown status
        // * Game was in lobby -> 💥 -> game started -> game ended -> init event -> unknown status
        dispatchedMessages.stream()
                .filter(dispatchedMessage -> !dispatchedMessage.getDeleted())
                .map(DispatchedMessage::getGameReference)
                .filter(
                        gameReference -> gameReferences.stream()
                                .map(GameReference::getId)
                                .noneMatch(id -> id.equals(gameReference.getId()))
                )
                .map(lastKnownGameReference -> {
                    if (lastKnownGameReference.getStatus().equals("running")) {
                        return new GameRefEvent(lastKnownGameReference, GameRefEvent.EventType.GAME_ENDED);
                    } else {
                        // All references whoose last known status was not "running" have an unknown reason for not existing anymore.
                        return new GameRefEvent(lastKnownGameReference, GameRefEvent.EventType.UNKNOWN_ENDED_OR_CLOSED);
                    }
                })
                .forEach(this::routeGameRefEvent);
    }

    /**
     * Decides which action is to be performed out based on the event type.
     *
     * @param gameRefEvent The game reference event
     */
    private void routeGameRefEvent(@NotNull GameRefEvent gameRefEvent) {
        final GameReference gameReference = gameRefEvent.getGameReference();

        synchronized (processedGamesList) {
            while (processedGamesList.contains(gameReference.id)) {
                try {
                    processedGamesList.wait();
                } catch (InterruptedException ignored) {
                }
            }
            processedGamesList.add(gameReference.id);
        }

        switch (gameRefEvent.getEventType()) {
            case CREATE -> createEvent(gameReference);
            case UPDATE -> updateEvent(gameReference);
            case LOBBY_CLOSED -> deleteEvent(gameReference); //nicht gestartetes game beendet
            case GAME_ENDED, UNKNOWN_ENDED_OR_CLOSED -> endEvent(gameReference);    //gestartetes game zu ende
        }

        synchronized (processedGamesList) {
            processedGamesList.remove(gameReference.id);
            processedGamesList.notifyAll();
        }
    }

    //START EVENTS//
    private void createEvent(GameReference gameReference) {
        announceNewGameReference(gameReference, AnnounceReason.NEW);
    }

    @SuppressWarnings("UnnecessaryReturnStatement")
    private void updateEvent(GameReference gameReference) {
        if (gameReference.status.equals("lobby")) {

            //Announce a game if it was not announced by cooldown and the cooldown is over
            if (announceNewGameReference(gameReference, AnnounceReason.UPDATE)) return;

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
     * @param announceReason reason why the game is to be announced.
     * @return true if the game reference was handled, false if it was rejected.
     */
    private boolean announceNewGameReference(GameReference gameReference, AnnounceReason announceReason) {
        //Never announce if the bot status is not RUNNING
        if (!isRunning()) return false;

        //Only announce games with a specific host engine version
        if (!isTargetVersion(gameReference))
            return false;

        //Never announce an already announced game (checked by game id)
        if (isAlreadyAnnounced(gameReference)) return false;

        //Never announce ignored hosts exempt they have active players
        if (isIgnoredHost(gameReference)) {
            //Only log if it's a new game to avoid log spam
            if (announceReason == AnnounceReason.NEW)
                Controller.INSTANCE.log.addLogEntry("DiscordConnector: Ignored by hostname: " + gameReference.id + ", Host: \"" + gameReference.hostname + "\".");
            return false;
        }

        //Never announce hosts with active cooldown (checked by hostname)
        if (hostHasActiveCooldown(gameReference)) {
            //Only log if it's a new game to avoid log spam
            if (announceReason == AnnounceReason.NEW)
                Controller.INSTANCE.log.addLogEntry("DiscordConnector: Ignored by cooldown: " + gameReference.id + ", Host: \"" + gameReference.hostname + "\".");
            return false;
        }

        //Never announce password protected games
        if (gameReference.flags.passwordNeeded) {
            //Only log if it's a new game to avoid log spam
            if (announceReason == AnnounceReason.NEW)
                Controller.INSTANCE.log.addLogEntry("DiscordConnector: Ignored by password: " + gameReference.id + ", Host: \"" + gameReference.hostname + "\".");
            return false;
        }

        //now the request can be marked as handled (returning true)

        final String messageContent = messageBuilder.build(gameReference, BuildAction.CREATE, null);
        if (messageContent != null) {
            try {
                addDispatchedMessage(new DispatchedMessage(
                        DiscordConnector.INSTANCE.getGameReferenceDispatchChannel().sendMessage(messageContent).complete(),
                        gameReference
                ));
                System.out.println("DiscordConnector: Dispatched: " + gameReference.id + ".");
            } catch (RuntimeException failure) {
                dispatchFailure(gameReference, failure, BuildAction.CREATE);
            }
        }
        return true;
    }

    /**
     * Checks if the bot is in status "RUNNING".
     *
     * @return true, if the bot is in status "RUNNING".
     */
    private boolean isRunning() {
        return Objects.equals(DiscordConnector.INSTANCE.status.getCurrentActivity(), de.creative_land.discord.Activity.RUNNING);
    }

    /**
     * Checks if the game reference version information matches the requirements.
     *
     * @param gameReference parsed game reference.
     * @return true if the game reference has the correct version, false if not.
     */
    private boolean isTargetVersion(GameReference gameReference) {
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

        //Filter out the references that belongs to the game and allowed runtime join or was in status lobby before
        final var optionalDispatchedMessage = dispatchedMessages.stream()
                .filter(dispatchedMessage -> dispatchedMessage.getGameReference().id.equals(gameReference.id))
                .filter(dispatchedMessage -> !dispatchedMessage.getDeleted())
                .filter(dispatchedMessage -> dispatchedMessage.getGameReference().flags.joinAllowed || dispatchedMessage.getGameReference().status.equals("created"))
                .findFirst();

        //Don't handle if no game reference matched
        if (optionalDispatchedMessage.isEmpty()) return false;

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

        //Filter out the reference that belongs to the game and denied runtime join or was in status lobby before
        final var optionalDispatchedMessage = dispatchedMessages.stream()
                .filter(dispatchedMessage -> dispatchedMessage.getGameReference().id.equals(gameReference.id))
                .filter(dispatchedMessage -> !dispatchedMessage.getDeleted())
                .filter(dispatchedMessage -> !dispatchedMessage.getGameReference().flags.joinAllowed || dispatchedMessage.getGameReference().status.equals("created"))
                .findFirst();

        //Don't handle if no game reference matched
        if (optionalDispatchedMessage.isEmpty()) return false;

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

        //Search for game reference id and replace
        final var optionalDispatchedMessage = dispatchedMessages.stream()
                .filter(dispatchedMessage -> dispatchedMessage.getGameReference().id.equals(gameReference.id))
                .filter(dispatchedMessage -> !dispatchedMessage.getDeleted())
                .findFirst();

        if (optionalDispatchedMessage.isEmpty()) return;

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
        //Search for game reference id and delete
        final var optionalDispatchedMessage = dispatchedMessages.stream()
                .filter(dispatchedMessage -> dispatchedMessage.getGameReference().id.equals(gameReference.id))
                .filter(dispatchedMessage -> !dispatchedMessage.getDeleted())
                .findFirst();

        if (optionalDispatchedMessage.isEmpty()) return false;

        final var dispatchedMessage = optionalDispatchedMessage.get();
        dispatchedMessage.getMessage().delete().queue(
                deletedMessage -> dispatchedMessages.set(dispatchedMessages.indexOf(dispatchedMessage), dispatchedMessage.markAsDeleted(gameReference)),
                failure -> dispatchFailure(gameReference, failure, BuildAction.DELETE)
        );
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
        final String messageContent = messageBuilder.build(gameReference, buildAction, dispatchedMessage.getMessage().getMentions().getRoles());
        if (messageContent == null) return;
        dispatchedMessage.getMessage().editMessage(messageContent).queue(
                message -> dispatchedMessages.set(dispatchedMessages.indexOf(dispatchedMessage), dispatchedMessage.update(message, gameReference)),
                failure -> dispatchFailure(gameReference, failure, buildAction)
        );
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
        Controller.INSTANCE.log.addLogEntry("DiscordConnector: Dispatch failed: " + gameReference.id + ", Action: " + buildAction + ", Error: ", failure);
        DiscordConnector.INSTANCE.scanEnvironment(null);
    }

    /**
     * Checks if the game reference was already announced.
     *
     * @param gameReference parsed game reference.
     * @return true if a game with the same id was announced in earlier.
     */
    private boolean isAlreadyAnnounced(GameReference gameReference) {
        return dispatchedMessages.stream().anyMatch(dispatchedMessage -> dispatchedMessage.getGameReference().id.equals(gameReference.id));
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
     * Parses a JSON-String of an SSE-event into a {@link GameReference}.
     *
     * @param message SSE-message in JSON format.
     * @return a {@link GameReference} parsed from the String or null if the message could not be parsed.
     */
    private GameReference parseGameReference(String message) {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            return mapper.readValue(message, GameReference.class);
        } catch (JsonProcessingException e) {
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: JSON Parsing error: \n", e);
        }
        return null;
    }

    /**
     * Parses a JSON-String of an SSE-event into a {@link GameReference} list.
     *
     * @param message SSE-message in JSON format.
     * @return a list of {@link GameReference} parsed from the String or null if the message could not be parsed.
     */
    private @NonNull List<GameReference> parseGameReferenceList(String message) {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        TypeFactory typeFactory = mapper.getTypeFactory();
        try {
            return mapper.readValue(message, typeFactory.constructCollectionType(List.class, GameReference.class));
        } catch (JsonProcessingException e) {
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: JSON Parsing error: \n", e);
        }
        return Collections.emptyList();
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

    private enum AnnounceReason {
        NEW,
        UPDATE
    }
}
