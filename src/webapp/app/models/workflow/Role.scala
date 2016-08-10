package models.workflow


class Role(val id: String, val name: String) {

	def this(id: String) = this (id, id)
}

object Role {
}