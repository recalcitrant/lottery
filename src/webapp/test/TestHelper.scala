package test

import org.openqa.selenium.By
import org.openqa.selenium.remote.RemoteWebDriver

object TestHelper {

  protected[test] var d: RemoteWebDriver = _

  protected[test] def enter(id: String, value: String) {
    findById(id).sendKeys(value)
  }

  protected[test] def click(id: String) {
    findById(id).click()
  }

  protected[test] def findById(id: String) = d.findElement(By.id(id))

}