package controllers

import play.api.http.{ContentTypes, HeaderNames}

trait CTypes {

	val CONTENT_TYPE_JSON = (HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
	val CONTENT_TYPE_JPG = "image/jpg"
	val CONTENT_TYPE_PJPEG = "image/pjpeg"
	val CONTENT_TYPE_PDF = "application/pdf"
	val CONTENT_TYPE_BINARY = "application/octet-stream"

}
