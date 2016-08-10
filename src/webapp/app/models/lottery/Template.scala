package models.lottery

import anorm._

case class Template(id: Pk[Long])

object Template {
  
  def list = Seq[Template]()
}