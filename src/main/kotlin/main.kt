package io.gitlab.edrd.explore.compose.desktop

import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonConstants
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Position
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed class ResponseBody {
  abstract val text: String

  class Content(override val text: String) : ResponseBody()

  object NoContent : ResponseBody() {
    override val text = "<no content>"
  }
}

fun main() = Window(title = "Dudu's HTTP Client") {
  var url by remember { mutableStateOf("https://reqres.in/api/users/2") }
  var method by remember { mutableStateOf(httpMethods.first()) }
  var responseBody: ResponseBody by remember { mutableStateOf(ResponseBody.Content("")) }

  MaterialTheme {
    Column {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(70.dp)
          .padding(10.dp)
      ) {
        HttpMethodMenu(
          toggleModifier = Modifier.width(httpMethodMenuWidth).fillMaxHeight(),
          onValueChanged = { newValue -> method = newValue }
        )
        Spacer(modifier = Modifier.width(10.dp))
        TextField(
          value = url,
          onValueChange = { value -> url = value },
          modifier = Modifier.weight(4f)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Button(
          text = "Execute",
          onClick = {
            responseBody = fetchUrl(method, url).let { body ->
              if (body.isBlank()) ResponseBody.NoContent
              else ResponseBody.Content(body)
            }
          },
          modifier = Modifier
            .width(100.dp)
            .fillMaxHeight()
        )
      }
      Text(
        text = responseBody.text,
        modifier = Modifier.padding(horizontal = 10.dp),
        fontWeight = when (responseBody) {
          ResponseBody.NoContent -> FontWeight.Bold
          else -> FontWeight.Normal
        }
      )
    }
  }
}

@Composable
fun HttpMethodMenu(toggleModifier: Modifier, onValueChanged: (String) -> Unit) {
  var expanded by remember { mutableStateOf(false) }
  var selectedValue by remember { mutableStateOf(httpMethods.first()) }

  DropdownMenu(
    toggle = {
      Button(
        modifier = Modifier.fillMaxSize(),
        onClick = { expanded = true },
        colors = ButtonConstants.defaultButtonColors(
          backgroundColor = MaterialTheme.colors.secondaryVariant
        )
      ) {
        Text(
          text = selectedValue,
          color = Color.White,
          fontWeight = FontWeight.Bold
        )
      }
    },
    toggleModifier = toggleModifier,
    dropdownOffset = Position(x = -httpMethodMenuWidth, y = 0.dp),
    onDismissRequest = { expanded = false },
    expanded = expanded
  ) {
    httpMethods.forEach { method ->
      DropdownMenuItem(onClick = {
        selectedValue = method
        expanded = false
        onValueChanged(method)
      }) {
        Text(method)
      }
    }
  }
}

private val httpMethodMenuWidth = 100.dp
private val httpMethods = setOf("GET", "POST", "PATCH", "PUT", "DELETE", "HEAD", "TRACE")

@Composable
fun Button(onClick: suspend () -> Unit, text: String, modifier: Modifier) {
  val scope = rememberCoroutineScope()
  var isLoading by remember { mutableStateOf(false) }

  Button(
    onClick = {
      isLoading = true
      scope.launch {
        onClick()
        isLoading = false
      }
    },
    modifier = modifier,
    enabled = !isLoading
  ) { Text(text) }
}

private suspend fun fetchUrl(method: String, url: String): String =
  suspendCoroutine { continuation ->
    val request = HttpRequest
      .newBuilder()
      .method(method, HttpRequest.BodyPublishers.noBody())
      .uri(URI.create(url))
      .build()

     val completableFuture = HttpClient.newHttpClient()
       .sendAsync(request, HttpResponse.BodyHandlers.ofString())

    completableFuture.thenApply { response -> continuation.resume(response.body()) }
  }
