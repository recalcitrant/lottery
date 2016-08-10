package utils.mail

object MailTemplateFunctions {

  val clientRegisterTemplates = Map(
    "hes" -> views.txt.email.client_register_hes.f,
    "osb" -> views.txt.email.client_register_osv.f,
    "rsg" -> views.txt.email.client_register_rsg.f,
    "svn" -> views.txt.email.client_register_svn.f,
    "thu" -> views.txt.email.client_register_thu.f,
    "wes" -> views.txt.email.client_register_wes.f,
    "wtm" -> views.txt.email.client_register_wtm.f
  )

  val clientUpdateTemplates = Map(
    "hes" -> views.txt.email.client_update_hes.f,
    "osb" -> views.txt.email.client_update_osv.f,
    "rsg" -> views.txt.email.client_update_rsg.f,
    "svn" -> views.txt.email.client_update_svn.f,
    "thu" -> views.txt.email.client_update_thu.f,
    "wes" -> views.txt.email.client_update_wes.f,
    "wtm" -> views.txt.email.client_update_wtm.f
  )

  val clientNewPasswordTemplates = Map(
    "hes" -> views.txt.email.client_new_password_hes.f,
    "osb" -> views.txt.email.client_new_password_osv.f,
    "rsg" -> views.txt.email.client_new_password_rsg.f,
    "svn" -> views.txt.email.client_new_password_svn.f,
    "thu" -> views.txt.email.client_new_password_thu.f,
    "wes" -> views.txt.email.client_new_password_wes.f,
    "wtm" -> views.txt.email.client_new_password_wtm.f
  )

  val clientUnregisterTemplates = Map(
    "hes" -> views.txt.email.client_unregister_hes.f,
    "osb" -> views.txt.email.client_unregister_osv.f,
    "rsg" -> views.txt.email.client_unregister_rsg.f,
    "svn" -> views.txt.email.client_unregister_svn.f,
    "thu" -> views.txt.email.client_unregister_thu.f,
    "wes" -> views.txt.email.client_unregister_wes.f,
    "wtm" -> views.txt.email.client_unregister_wtm.f
  )
}
