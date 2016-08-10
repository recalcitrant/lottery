package models.auth

trait PermissionBundle {

  val PERMISSION_ADMIN = 1l
  val PERMISSION_REDAKTEUR = 2l
  val PERMISSION_FREIGEBER = 3l
  val PERMISSION_SUPER_ADMIN = 4l

  val Redakteur_OR_Freigeber = Seq(PERMISSION_REDAKTEUR, PERMISSION_FREIGEBER)
  val Redakteur = Seq(PERMISSION_REDAKTEUR)
  val Admin = Seq(PERMISSION_ADMIN)
  val SuperAdmin = Seq(PERMISSION_SUPER_ADMIN)
}