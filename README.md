# Clonkspot-Connector
This is a connector for a reference SSE stream of Clonk game events from
a [Clonk masterserver](https://github.com/clonkspot/league) (Clonkspot). It currently connects only with Discord and
publishes new game references on a text channel.

## Setup and first run
The connector accepts parameters via system ENVs or command line arguments.
### Environment variables
|ENV|Description|Value|Required|
|--|--|--|--|
|KEY|Discord api key of an already created [application](https://discord.com/developers/applications) |String|first run only|
|GUILD_ID|Discord server id|long|first run only|
|ADMIN_ROLE_NAME|Permitted discord group to control the application|long|first run only|
|DEBUG|This enables developers to host a game without disturbing other users in production|boolean|no|
|JOIN_URL|Url which is dispatched for others to join games|String|first run only|
|SSE_ENDPOINT|SSE endpoint where the application searches for new games|String|first run only|

### Command line arguments
Environment variables can be overwritten with command line arguments:
```
--guildid <number> ------------ Discord server id
--adminrolename <String> ------ Permitted discord group to control the application
--key <String> ---------------- Discord api key of an already created [application](https://discord.com/developers/applications)
--joinurl <String> ------------ Url which is dispatched for others to join games
--sseendpoint <String> -------- SSE endpoint where the application searches for new games
```

If arguments or ENVs are used they will always overwrite the configuration file.
### Configuration file
When starting the application a configuration file is being generated in the running directory as `config.json`. If such a file still exists it will be used so that no startup parameters are needed anymore.

## Usage
The application is reachable via private discord message for users which have the admin role. All specified commands and can be shown by sending `help` to the discord bot.