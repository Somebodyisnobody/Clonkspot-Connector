# Documentation Twitch stream notifications

The bot allows to send a notification if a stream in a specific twitch game category is running. It also sends a
notification when the stream ended.

## Configuration

Configuration is done by a JSON object in the config:

```json
{
    "twitchConfiguration" : {
        "clientId" : "",
        "clientSecret" : "",
        "game" : "",
        "initialStartupDelay" : 5,
        "searchPeriod" : 30
    }
}
```

| Key | Description | Default value |
|--|--|--|
| clientId | The client id for the API connection | "" |
| clientSecret | The token for the API connection | "" |
| game | The name of the game to search for streams | "" |
| initialStartupDelay | The initial delay in seconds after which the game list is being fetched | 5 |
| searchPeriod | The period in seconds in which the game list is being fetched | 30 |

## Get an API token

Prequisites:

* Twitch account
* 2FA activated

1. Login into https://dev.twitch.tv/
2. Go to https://dev.twitch.tv/console/apps
3. Create a new application with a spelling name
4. Add any URL to the OAuth Redirect URLs (would be used when authenticating viy OAuth was successful)
5. Click on "Save", and select the new created application again
6. Copy Client-ID
7. Generate new secret

