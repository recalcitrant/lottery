package models.lottery

case class WinningNotificatonConfig(server: String, port: Int, user: String, pw: String, tls: Boolean, fromAddress: String)
