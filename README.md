# TelegramSneakerBot
## A Telegram bot notifying registered Telegram chats on updates on the registered sites

### Quickstart
Add the following Scala objects to $/src/main/scala/credentials/$

```
package credentials

object BotCredentials {
  val fullToken: String = ??? // your token
}
```
and

```
package credentials

object FilePaths {
  val chats = ??? // "/../../chats.txt"
  val urls = ???  // "/../../urls.csv"
}

```
