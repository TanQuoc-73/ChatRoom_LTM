# chat-realtime

run phia server
mvn exec:java "-Dexec.mainClass=chat.server.ServerLauncher"

run phía client
mvn exec:java "-Dexec.mainClass=chat.client.ClientLauncher"
sửa xcong chatserver

client ok

maven reset:
mvn clean install
