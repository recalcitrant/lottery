package controllers

package object client {
	implicit val drawingJsonFormat = models.lottery.drawing.DrawingJsonFormat.DrawingFormat
	implicit val drawingResultJsonFormat = models.lottery.drawing.DrawingJsonFormat.DrawingResultFormat
	implicit val clientJsonFormat = models.client.ClientJsonFormat.ClientFormat
	implicit val userQueryJsonFormat = models.client.ClientJsonFormat.UserQueryFormat
	implicit val ticketRangeJsonFormat = models.client.ClientJsonFormat.RangeFormat
}
