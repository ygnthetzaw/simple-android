package org.simple.clinic.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import org.simple.clinic.R
import org.simple.clinic.ui.theme.ButtonDefaults
import org.simple.clinic.ui.theme.buttonBig

class ProgressMaterialButton(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

  @DrawableRes
  private var icon: Int? = null

  @StringRes
  private var text: Int? = null

  init {
    val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressMaterialButton)
    val buttonState = ButtonState.values()[typedArray.getInt(R.styleable.ProgressMaterialButton_state, 0)]

    icon = typedArray.getResourceId(R.styleable.ProgressMaterialButton_icon, -1)
    text = typedArray.getResourceId(R.styleable.ProgressMaterialButton_text, -1)

    addView(ComposeView(context = context).apply {
      setContent { Button(buttonState = buttonState) }
    })

    typedArray.recycle()
  }

  fun setButtonState(buttonState: ButtonState) {
    val composeView = (this as ViewGroup).getChildAt(0) as ComposeView
    composeView.setContent { Button(buttonState = buttonState) }
  }

  @Composable
  fun Button(buttonState: ButtonState) {
    MdcTheme {
      ProgressMaterialButton(
          icon = { if (icon != null && icon != NO_ID) Image(painter = painterResource(id = icon!!), contentDescription = "") },
          text = { if (text != null && text != NO_ID) Text(text = stringResource(id = text!!), style = MaterialTheme.typography.buttonBig) },
          onClick = { (parent as View).performClick() },
          buttonState = buttonState
      )
    }
  }
}

@Composable
fun ProgressMaterialButton(
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonState: ButtonState = ButtonState.Enabled
) {
  val isInProgress = buttonState == ButtonState.InProgress
  val isButtonEnabled = buttonState == ButtonState.Enabled || isInProgress

  Button(
      modifier = Modifier
          .defaultMinSize(
              minHeight = ButtonDefaults.MinHeight
          )
          .then(modifier),
      onClick = onClick,
      enabled = isButtonEnabled,
      contentPadding = PaddingValues(horizontal = 16.dp)
  ) {
    if (isInProgress) {
      CircularProgressIndicator(color = MaterialTheme.colors.onPrimary,
          modifier = Modifier.size(16.dp),
          strokeWidth = 2.dp)
    } else {
      icon()
      Spacer(modifier = Modifier.width(8.dp))
      text()
    }
  }
}

@Preview
@Composable
fun ProgressMaterialButtonEnabledPreview() {
  ProgressMaterialButton(
      icon = { Image(Icons.Filled.Done, contentDescription = null) },
      text = { Text(text = "Done") },
      onClick = { /*TODO*/ },
      buttonState = ButtonState.Enabled
  )
}

@Preview
@Composable
fun ProgressMaterialButtonInProgressPreview() {
  ProgressMaterialButton(
      icon = { Image(Icons.Filled.Done, contentDescription = null) },
      text = { Text(text = "Done") },
      onClick = { /*TODO*/ },
      buttonState = ButtonState.InProgress
  )
}

@Preview
@Composable
fun ProgressMaterialButtonDisabledPreview() {
  ProgressMaterialButton(
      icon = { Image(Icons.Filled.Done, contentDescription = null) },
      text = { Text(text = "Done") },
      onClick = { /*TODO*/ },
      buttonState = ButtonState.Disabled
  )
}
