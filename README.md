# Poll Plugin
<img src="https://img.shields.io/github/actions/workflow/status/Shweit/Poll/runtime.yml" /> <img src="https://img.shields.io/github/v/release/Poll/MinecraftServerAPI" /> <img src="https://img.shields.io/github/license/Shweit/Poll" />

## Overview
The Poll Plugin is a versatile and easy-to-use tool for Minecraft servers, allowing administrators to create and manage polls directly in-game. With this plugin, server owners can engage their players by gathering feedback, making decisions, and encouraging participation in server activities.

## Features
- **Create Polls:** Users with the appropriate permissions can create polls with custom questions and options.
- **Vote:** Players can vote on polls using the in-game chat or GUI interface.
- **Poll Management:** Polls can be easily managed and viewed, with options for pagination and filtering of open polls.
- **Detailed Poll View:** Players can view detailed information about each poll, including the number of votes for each option and their own selected answers.
- **Real-time Updates**: Poll results are updated in real-time, allowing players to see the outcome as votes are cast.

## Commands
`/createpoll "<question>" "<anwser1>" "<answer2> ... [--multi]"`
- **Description:** Creates a new poll with the specified question and answers. The --multi flag allows multiple answers to be selected.
- **Permission:** poll.create
- **Example:** `/createpoll "What is your favorite color?" "Red" "Blue" "Green"`

`/polls [page]`
- **Description:** Opens the polls GUI, showing a list of all active polls. Use the optional page argument to navigate through multiple pages of polls.
- **Permission:** poll.view
- **Example:** `/polls 2`

`/vote <poll_id> <answer>`
- **Description:** Votes for the specified answer in the given poll.
- **Permission:** poll.vote
- **Example:** `/vote 1 "Red"`

## Installation
### Prerequisites
- **Java:** JDK 20 or higher is required to build and run the project.
- **Maven:** Make sure Maven is installed on your system.
  You can download it [here](https://maven.apache.org/download.cgi).

### Cloning the Repository
1. Clone the repository to your local machine.
```shell
git clone git@github.com:Shweit/Poll.git
cd POll
```
### Building the Project
2. Build the project using Maven.
```shell
mvn clean install
```
### Setting up the Minecraft Server
3. Copy the generated JAR file to the `plugins` directory of your Minecraft server.
```shell
cp target/Poll-*.jar /path/to/your/minecraft/server/plugins
```
4. Start or restart your Minecraft server.
```shell
java -Xmx1024M -Xms1024M -jar paper-1.21.jar nogui
```
5.  Once the server is running, the plugin will be loaded automatically. You can verify it by running:
```shell
/plugins
```

## Future Features
- **Custom Poll Durations:** Set start and end times for polls.
- **Poll Notifications:** Notify players when a new poll is created or when a poll is about to close.
- **Multiple Language Support:** Add localization support for multiple languages.
- **Advanced Poll Analytics:** Provide detailed statistics and insights on poll results.

## Contributing
Contributions are welcome! Please read the [contributing guidelines](CONTRIBUTING.md) to get started.