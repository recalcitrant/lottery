function addEmailField() {
  var elem = $("#info_workflow_action_fields");
  var label = $('<label>').attr("for", "flow_email_comment").append("E-Mail-Kommentar:").append("</label>");
  label.addClass('textarea');
  elem.append(label);
  var item = $('<textarea cols="30" rows="8"></textarea>');
  item.attr("id", "flow_email_comment").attr("name", "flow_email_comment");
  elem.append($('<div>').append(item));
  return elem;
}

function submitWorkflow(action, url) {
  $("#message").html("Der Status wird gespeichert");
  var json = {};
  json.emailcomment = $.trim($("#flow_email_comment").val());
  json.action = action;
  $("#submit_workflow_action_link").remove();
  $.ajax({
    url: url,
    type: "POST",
    data: JSON.stringify(json),
    contentType: "application/json; charset=utf-8",
    dataType: "json",
    success: function (json) {
      if ("ok" == json.status) window.location.href = "/drawing/list/" + json.msg;
      else $("#message").removeClass("hidden").html(json.msg);
    }
  });
}
