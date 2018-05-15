# VinallyFamous

Vine quiz website for CSCI 420: Networking Project 3

Ethan Duryea, Elliot Spicer, Joshua Weller, John Zamites

## Installation

Simply run `javac VineServer.java`
Then `java VineServer`
To connect, go to LAN address of the machine running the server in web browser. Server will be running on port 8080.
## Issues

1. A user cannot open multiple tabs of game in a browser without breaking game.
2. A user cannot disconnect and reconnect without breaking game.
3. Server is unable to kill threads of disconnected clients.
4. Game in client browser will sometimes be out of sync with server.
5. Youtube videos will not play.
