/**
 *
 * InstallKeystoreActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 08/02/16.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Ledger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package co.ledger.wallet.legacy

import java.security.KeyStore.PasswordProtection

import android.content.Intent
import android.os.Bundle
import android.view.{KeyEvent, Menu, MenuItem}
import android.widget.TextView.OnEditorActionListener
import android.widget.{TextView, Toast}
import co.ledger.wallet.R
import co.ledger.wallet.core.base.BaseActivity
import co.ledger.wallet.core.concurrent.ExecutionContext.Implicits.ui
import co.ledger.wallet.core.security.{ApplicationKeystore, Keystore}
import co.ledger.wallet.core.utils.logs.LogCatReader
import co.ledger.wallet.core.utils.{AndroidUtils, TR}
import co.ledger.wallet.core.widget.EditText
import co.ledger.wallet.legacy.unplugged.UnpluggedTapActivity

import scala.util.{Failure, Success}

class InstallKeystoreActivity extends BaseActivity {

  lazy val passwordEditText = TR(R.id.password).as[EditText]
  lazy val confirmEditText = TR(R.id.confirmation).as[EditText]

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.install_keystore_activity)
    confirmEditText.setImeActionLabel(getString(R.string.action_done), KeyEvent.KEYCODE_ENTER)
    confirmEditText.setOnEditorActionListener(new OnEditorActionListener {
      override def onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean = {
        if (event.getAction == KeyEvent.ACTION_DOWN && event.getKeyCode == KeyEvent.KEYCODE_ENTER) {
          installKeystore()
          true
        } else {
          false
        }
      }
    })
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(R.menu.home_activity_menu, menu)
    if (!AndroidUtils.hasNfcFeature()) {
      menu.findItem(R.id.setup_unplugged).setVisible(false)
    }
    menu.findItem(R.id.settings).setVisible(false)
    true
  }

  def startConfigureUnplugged(): Unit =
    startActivity(new Intent(this, classOf[UnpluggedTapActivity]).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))


  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    super.onOptionsItemSelected(item)

    item.getItemId match {
      case R.id.export_logs =>
        exportLogs()
        true
      case somethingElse => false
    }
  }

  def installKeystore(): Unit = {
    val pwd = passwordEditText.getText.toString
    val confirmation = confirmEditText.getText.toString

    if (pwd.length < 6) {
      Toast.makeText(this, R.string.install_keystore_too_short, Toast.LENGTH_SHORT).show()
    } else if (pwd != confirmation) {
      passwordEditText.setText("")
      confirmEditText.setText("")
      Toast.makeText(this, R.string.install_keystore_password_mismatch, Toast.LENGTH_SHORT).show()
    } else {
      Keystore.defaultInstance.asInstanceOf[ApplicationKeystore].install(new PasswordProtection(pwd.toCharArray)) onComplete {
        case Success(_) => finish()
        case Failure(ex) =>
          ex.printStackTrace()
          Toast.makeText(this, R.string.install_keystore_install_error, Toast.LENGTH_SHORT).show()
      }
    }
  }


  private[this] def exportLogs(): Unit = {
    LogCatReader.createEmailIntent(this).onComplete {
      case Success(intent) =>
        startActivity(intent)
      case Failure(ex) =>
        ex.printStackTrace()
    }
  }

}
